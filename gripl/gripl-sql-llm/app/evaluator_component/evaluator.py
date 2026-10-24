import time
from app.data_loader_component.data_loader import DataLoader
from app.pipeline_component.pipeline import PipelineComponent
from app.logging_component.logger import Logger
import traceback
import json
from pathlib import Path

class Evaluator:

    def __init__(self,
                 data_loader: DataLoader,
                 pipe_line: PipelineComponent,
                 logger: Logger,
                 ):
        self.data_loader = data_loader
        self.pipe_line = pipe_line
        self.logger = logger
        self.amount_using_testdata = 5

    async def evaluate(self):
        try:

            self.delete_all_models_before_run()

            self.delete_result_file_if_exists()

            self.fill_models_with_fall_backs_in_db()

            eval_pd_set = self.data_loader.load_evaluation_data_set_as_pd()

            eval_pd_set = eval_pd_set.head(self.amount_using_testdata)

            for index, row in eval_pd_set.iterrows():
                current_bpmn_file = row["bpmn_xml"]

                predicted_critical_elements = await self.pipe_line.get_analysis(current_bpmn_file)

                gold_critical_elements = row.get("expected_values", [])

                time.sleep(60)

                self.logger.log_results(
                    gold_results=gold_critical_elements,
                    predicted_results=predicted_critical_elements,
                )

                time.sleep(60)

        except Exception as e:
            print("in eval pipeline")
            print(traceback.format_exc())
            print(e)

    def check_equal_and_predicted_equal(self,
                                        predicted_critical_elements,
                                        gold_critical_elements
                                        )-> bool:
        gold_critical_elements = json.loads(gold_critical_elements)
        return set(
            (element["value"], element["reason"])
            for element in predicted_critical_elements
        ) == set(
            (element["value"], element["reason"])
            for element in gold_critical_elements
        )

    def delete_result_file_if_exists(self,):

        project_root = Path(__file__).resolve().parent.parent.parent

        intention_csv = project_root / "app" / "eval" / "intention.csv"

        sql_generation_csv = project_root / "app" / "eval" / "sql_generation.csv"

        post_processing_csv = project_root / "app" / "eval" / "post_processing.csv"

        result_csv = project_root / "app" / "eval" / "result.csv"

        logging_files = [intention_csv, sql_generation_csv, post_processing_csv, result_csv]

        for file in logging_files:
            if file.exists():
                file.unlink()

    def delete_all_models_before_run(self):
        try:

            sql = "DELETE FROM fallback_llm"

            sql_execution_component = self.pipe_line.sql_execution

            sql_execution_component.delete_sql(sql)

            print("All fallback LLMs deleted successfully")

        except Exception as e:
            print("error deleting all models")
            print(traceback.format_exc())

    def fill_models_with_fall_backs_in_db(self):
        try:

            sql_execution_component = self.pipe_line.sql_execution

            test_data = {
                "INTENTION_MODEL": [
                    ("openai/gpt-oss-120b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                    ("openai/gpt-oss-20b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                ],
                "SQL_GENERATION_MODEL": [
                    ("openai/gpt-oss-120b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                    ("openai/gpt-oss-20b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                ],
                "POST_PROCESSING_MODEL": [
                    ("openai/gpt-oss-120b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                    ("openai/gpt-oss-20b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                ],
                "VERIFICATION_MODEL": [
                    ("openai/gpt-oss-120b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                    ("openai/gpt-oss-20b", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1"),
                ],
                "EMBEDDING_MODEL": [
                    ("openai/text-embedding-3-small", "OPEN_ROUTER_API_KEY", "https://openrouter.ai/api/v1/embeddings"),
                ],
            }

            for comp_key, entries in test_data.items():
                for idx, (model_name, api_key_name, base_url) in enumerate(entries):
                    sql = """
                          INSERT INTO fallback_llm
                              (corresponding_comment, name, "order", model_url, env_api_key_name)
                          VALUES (?, ?, ?, ?, ?) \
                          """
                    params = (comp_key, model_name, idx + 1, base_url, api_key_name)

                    result = sql_execution_component.insert_sql(sql, params)
                    if result and "error" in result:
                        print(f"Fehler beim Einfügen: {result['error']}")
                        return result

            print("Alle Fallback-Modelle erfolgreich eingefügt.")

        except Exception as e:
            print(traceback.format_exc())
            return {"error": str(e)}

