import sqlite3
import traceback
from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.find_intention_component.find_intention import FindIntention
from app.schemas import RunConfig
from app.sql_execution_component.sql_execution import SQLExecution
from app.sql_generation_component.sql_generation import SQLGenerator
from app.sql_post_processing_component.sql_post_processing import SQLPostProcessing
from app.bpmn_data_pre_processor_component.bpmn_data_pre_processor import BPMNDataPreProcessor
from app.database.utils import get_db_schema_string
import logging

logger = logging.getLogger(__name__)

class PipelineComponent:

    def __init__(self,
            chroma_db_client: ChromaDatabaseClient,
            find_intention: FindIntention,
            sql_execution: SQLExecution,
            sql_generation: SQLGenerator,
            post_processing: SQLPostProcessing,
            bpmn_data_pre_processor: BPMNDataPreProcessor,
                 ):
        self.chroma_db_client = chroma_db_client
        self.find_intention = find_intention
        self.sql_execution = sql_execution
        self.sql_generation = sql_generation
        self.post_processing = post_processing
        self.bpmn_data_pre_processor = bpmn_data_pre_processor

    async def get_analysis(self, run_config: RunConfig):

        try:

            results = []

            total_activities_fields = self.get_only_activity_fields(run_config.bpmn_file)

            for activity in total_activities_fields[:3]:

                current_result = await self.get_sid_and_reason_if_critical(activity,
                                                                           run_config
                                                                           )

                if current_result and current_result.get("id", "") != "" and current_result.get("reason", "") != "":
                    results.append(current_result)

            return results

        except Exception:
            logger.exception(traceback.format_exc())
            raise


    def get_only_activity_fields(self, bpmn_file_content: bytes):
        try:
            return self.bpmn_data_pre_processor.get_only_activity_field_of_bpmn_file(
                bpmn_content=bpmn_file_content,
            )
        except Exception as e:
            logger.exception(traceback.format_exc())
            raise

    async def get_sid_and_reason_if_critical(
            self,
            activity_field: str,
            run_config: RunConfig,
    ) -> dict[str, str]:

        intentions = self.find_intention.find_intention_of_current_activity_field(
            activity_field,
            run_config=run_config,
        )

        if not intentions:
            return {}

        intent = intentions[0]

        activity_id = (
            self.bpmn_data_pre_processor
            .get_sid_from_activity_field_of_bpmn_file(activity_field)
        )

        try:
            reason_rows = self.sql_execution.get_sql_query_results(
                """
                SELECT r.reason
                FROM reason r
                         JOIN category_reason_association cra
                              ON r.id = cra.reason_id
                         JOIN category c
                              ON c.id = cra.category_id
                WHERE c.name = ?;
                """,
                (intent,),
            )

        except sqlite3.Error as e:
            logger.exception("Failed to retrieve reasons for intention.")

            return {
                "id": activity_id,
                "reason": "",
                "error": f"Failed to retrieve intention reasons: {e}",
            }

        reasons_of_intention = [
            row["reason"]
            for row in reason_rows
            if row.get("reason") is not None
        ]

        activity_name = (
            self.bpmn_data_pre_processor
            .get_name_from_activity_field_of_bpmn_file(activity_field)
        )


        few_shot_examples = (
            self.chroma_db_client.get_top_k_results_only_meta_data(
                activity_name,
                run_config=run_config,
            )
        )

        formatted_few_shot_examples = self.format_few_shot_examples(
            few_shot_examples
        )

        db_schema = get_db_schema_string()

        generated_query = self.sql_generation.generate_query(
            activity_field=activity_field,
            db_schema=db_schema,
            intentions=intentions,
            reasons_of_intentions=reasons_of_intention,
            few_shot_examples=formatted_few_shot_examples,
            run_config=run_config,
        )

        error_message = ""

        try:
            generated_results = self.sql_execution.get_sql_query_results(
                generated_query
            )

            if not generated_results:
                error_message = (
                    "The SQL query executed successfully but returned no rows."
                )

            elif not generated_results[0].get("reason"):
                error_message = (
                    "The SQL query returned no non-empty 'reason' value."
                )

        except sqlite3.Error as e:
            error_message = str(e)

            logger.warning(
                "Generated SQL query failed for activity %s: %s",
                activity_id,
                error_message,
            )

        post_processed_query = (
            await self.post_processing.post_process_generated_query(
                db_schema=db_schema,
                activity_field=activity_field,
                generated_query=generated_query,
                error_message=error_message,
                intentions=intentions,
                reasons_of_intentions=reasons_of_intention,
                run_config=run_config,
            )
        )

        try:
            post_processed_results = (
                self.sql_execution.get_sql_query_results(post_processed_query)
            )

        except sqlite3.Error as e:
            logger.exception(
                "Post-processed SQL query failed for activity %s.",
                activity_id,
            )

            return {
                "id": activity_id,
                "reason": "",
                "error": f"Post-processed SQL execution failed: {e}",
            }

        if not post_processed_results:
            return {}


        reason = post_processed_results[0].get("reason")

        if reason:
            return {
                "id": activity_id,
                "reason": str(reason),
            }

        return {}

    def format_few_shot_examples(self, few_shot_examples) -> str:

        formatted_examples = [
            (
                f"Activity field name: {activity_name}\n"
                f"SQL query: {metadata.get('sql', '')}"
            )
            for activity_name, metadata in few_shot_examples
        ]

        return (
                "Consider for your generation as a guide.\n"
                + "\n\n".join(formatted_examples)
        )

