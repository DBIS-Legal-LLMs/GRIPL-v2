import traceback
from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.find_intention_component.find_intention import FindIntention
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

    async def get_analysis(self, bpmn_file_content: str):

        try:

            results = []

            total_activities_fields = self.get_only_activity_fields(bpmn_file_content)

            for activity in total_activities_fields:

                current_result = await self.get_sid_and_reason_if_critical(activity)

                if current_result and current_result.get("id", "") != "" and current_result.get("reason", "") != "":
                    results.append(current_result)

            return results

        except Exception:
            print(traceback.format_exc())
            return []


    def get_only_activity_fields(self, bpmn_file_content: str):
        try:
            return self.bpmn_data_pre_processor.get_only_activity_field_of_bpmn_file(
                bpmn_content=bpmn_file_content,
            )
        except Exception as e:
            print(e)
            return []

    async def get_sid_and_reason_if_critical(self, activity_field: str)->dict[str, str]:
        try:

            intentions = self.find_intention.find_intention_of_current_activity_field(activity_field)

            if not intentions:
                return {}

            intent = intentions[0]

            reasons_of_intention = [execution_result.get("reason", "") for execution_result in
                                    self.sql_execution.get_sql_query_results(f"""
                                                                            SELECT  r.reason
                                                                            FROM reason r
                                                                                     JOIN category_reason_association cra
                                                                                          ON r.id = cra.reason_id
                                                                                     JOIN category c
                                                                                          ON c.id = cra.category_id
                                                                            WHERE c.name = '{intent}';
                                                                            """)]


            activity_name = self.bpmn_data_pre_processor.get_name_from_activity_field_of_bpmn_file(activity_field)
            few_shot_examples = self.chroma_db_client.get_top_k_results_only_meta_data(activity_name)

            formated_few_shot_examples = self.format_few_shot_examples(few_shot_examples)

            db_schema = get_db_schema_string()

            generated_query = self.sql_generation.generate_query(
                activity_field=activity_field,
                db_schema=db_schema,
                intentions=intentions,
                reasons_of_intentions=reasons_of_intention,
                few_shot_examples=formated_few_shot_examples,
            )

            print("generated query")
            print(generated_query)


            results_of_generated_query = self.sql_execution.get_sql_query_results(generated_query)[0].get("reason", "")

            error_message = ""

            if isinstance(results_of_generated_query, dict) and "error" in results_of_generated_query:
                error_message = results_of_generated_query["error"]

            post_processed_query = await self.post_processing.post_process_generated_query(
                db_schema=db_schema,
                activity_field=activity_field,
                generated_query=generated_query,
                error_message=error_message,
                intentions=intentions,
                reasons_of_intentions=reasons_of_intention,
            )

            print("post processed query")
            print(post_processed_query)

            results_of_post_processed_query = self.sql_execution.get_sql_query_results(post_processed_query)[0].get("reason", "")


            if results_of_post_processed_query:
                return {
                    "id": self.bpmn_data_pre_processor.get_sid_from_activity_field_of_bpmn_file(activity_field),
                    "reason": results_of_post_processed_query ,
                }

            return {}

        except Exception as e:
            print(e)
            return {}

    def format_few_shot_examples(self, list):

        formated_prompt = "Consider for your generation as a guide. \n"

        for activity_name, metadata in list:
            formated_prompt += formated_prompt + f"Activity field name:  {activity_name}\n"
            formated_prompt += formated_prompt + f"SQL query:  {metadata.get('sql', '')}\n"

        return formated_prompt

