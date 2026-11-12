
import json
import logging
import math
import os
from pathlib import Path
from typing import Any

from app.config.component_name import EMBEDDING_MODEL, VERIFICATION_MODEL, POST_PROCESSING_MODEL, SQL_GENERATION_MODEL, \
    INTENTION_MODEL
from app.data_loader_component.data_loader import DataLoader
from app.pipeline_component.pipeline import PipelineComponent
from app.logging_component.logger import Logger
from app.schemas import ComponentLLMConfig, RunConfig

logger = logging.getLogger(__name__)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(name)s - %(message)s",
)


class Evaluator:

    def __init__(
        self,
        data_loader: DataLoader,
        pipe_line: PipelineComponent,
        logger: Logger,
        max_samples: int | None = 1,
    ):
        self.data_loader = data_loader
        self.pipe_line = pipe_line
        self.logger = logger
        self.max_samples = max_samples

    # ---------------------------------------------------------
    # Main evaluation
    # ---------------------------------------------------------

    async def evaluate(self) -> dict[str, Any]:

        self.delete_all_models_before_run()
        self.delete_result_file_if_exists()
        self.fill_models_with_fall_backs_in_db()

        eval_pd_set = self.data_loader.load_evaluation_data_set_as_pd()

        if self.max_samples is not None:
            eval_pd_set = eval_pd_set.head(self.max_samples)

        total_samples = len(eval_pd_set)

        # Global confusion matrix
        total_counts = {
            "tp": 0,
            "fp": 0,
            "fn": 0,
            "tn": 0,
        }

        evaluated_samples = 0
        failed_samples = 0
        exact_matches = 0
        total_activities = 0

        # Category and reason evaluation
        category_correct = 0
        category_total = 0

        reason_correct = 0
        reason_total = 0

        api_key = os.environ.get("OPEN_ROUTER_API_KEY")

        models_by_component = {
            INTENTION_MODEL: ComponentLLMConfig(
                model_name=os.getenv("INTENTION_LLM_MODEL"),
                base_url=os.getenv("INTENTION_MODEL_URL"),
            ),
            SQL_GENERATION_MODEL: ComponentLLMConfig(
                model_name=os.getenv("SQL_GENERATION_MODEL"),
                base_url=os.getenv("SQL_GENERATION_MODEL_URL"),
            ),
            POST_PROCESSING_MODEL: ComponentLLMConfig(
                model_name=os.getenv("POST_PROCESSING_MODEL"),
                base_url=os.getenv("POST_PROCESSING_MODEL_URL"),
            ),
            VERIFICATION_MODEL: ComponentLLMConfig(
                model_name=os.getenv("VERIFICATION_MODEL"),
                base_url=os.getenv("VERIFICATION_MODEL_URL"),
            ),
            EMBEDDING_MODEL: ComponentLLMConfig(
                model_name=os.getenv("EMBEDDING_MODEL_MODEL"),
                base_url=os.getenv("EMBEDDING_MODEL_URL"),
            ),
        }


        for index, row in eval_pd_set.iterrows():

            try:
                # 1. Load BPMN file
                current_bpmn_file = row["bpmn_xml"]

                run_config = RunConfig(
                    bpmn_file=current_bpmn_file,
                    api_key=api_key,
                    models_by_component=models_by_component,
                )

                # 2. Extract all activity IDs from BPMN
                all_activity_ids = self.get_all_activity_ids(
                    current_bpmn_file
                )

                # 3. Execute pipeline
                predicted_critical_elements = (
                    await self.pipe_line.get_analysis(
                        run_config
                    )
                )

                # 4. Get ground truth
                gold_critical_elements = self.parse_ground_truth(
                    row.get("expected_values", [])
                )

                # 5. Extract activity IDs
                gold_critical_ids = self.get_gold_critical_ids(
                    gold_critical_elements
                )

                predicted_critical_ids = (
                    self.get_predicted_critical_ids(
                        predicted_critical_elements
                    )
                )

                # 6. Calculate confusion matrix for current BPMN
                current_counts = self.calculate_confusion_matrix(
                    all_activity_ids=all_activity_ids,
                    gold_critical_ids=gold_critical_ids,
                    predicted_critical_ids=predicted_critical_ids,
                )

                # 7. Aggregate global confusion matrix
                for metric, count in current_counts.items():
                    total_counts[metric] += count

                total_activities += len(all_activity_ids)

                # 8. Exact match evaluation
                if self.check_equal_and_predicted_equal(
                    predicted_critical_elements,
                    gold_critical_elements,
                ):
                    exact_matches += 1

                # 9. Category and reason accuracy
                category_counts = self.calculate_category_and_reason_accuracy(
                    predicted_critical_elements,
                    gold_critical_elements,
                )

                category_correct += category_counts["category_correct"]
                category_total += category_counts["category_total"]

                reason_correct += category_counts["reason_correct"]
                reason_total += category_counts["reason_total"]

                evaluated_samples += 1

                # 10. Log detailed results
                try:
                    self.logger.log_results(
                        gold_results=gold_critical_elements,
                        predicted_results=predicted_critical_elements,
                    )
                except Exception:
                    logger.exception(
                        "Failed to log evaluation results for BPMN %s",
                        index,
                    )

                logger.info(
                    "BPMN %s evaluated successfully: %s",
                    index,
                    current_counts,
                )

            except Exception:
                failed_samples += 1

                logger.exception(
                    "Evaluation failed for BPMN at index %s",
                    index,
                )


        # -----------------------------------------------------
        # Calculate global metrics
        # -----------------------------------------------------

        tp = total_counts["tp"]
        fp = total_counts["fp"]
        fn = total_counts["fn"]
        tn = total_counts["tn"]

        precision = self.safe_division(
            tp,
            tp + fp,
        )

        recall = self.safe_division(
            tp,
            tp + fn,
        )

        f1 = self.safe_division(
            2 * precision * recall,
            precision + recall,
        )

        accuracy = self.safe_division(
            tp + tn,
            tp + fp + fn + tn,
        )

        exact_match_rate = self.safe_division(
            exact_matches,
            evaluated_samples,
        )

        category_accuracy = (
            self.safe_division(category_correct, category_total)
            if category_total > 0 else None
        )

        reason_accuracy = (
            self.safe_division(reason_correct, reason_total)
            if reason_total > 0 else None
        )

        # -----------------------------------------------------
        # Final evaluation report
        # -----------------------------------------------------

        metrics = {
            "total_bpmn_files": total_samples,
            "evaluated_bpmn_files": evaluated_samples,
            "failed_bpmn_files": failed_samples,
            "total_activities": total_activities,

            "confusion_matrix": {
                "true_positives": tp,
                "false_positives": fp,
                "false_negatives": fn,
                "true_negatives": tn,
            },

            "classification_metrics": {
                "precision": precision,
                "recall": recall,
                "f1": f1,
                "accuracy": accuracy,
            },

            "exact_match": {
                "correct_bpmn_files": exact_matches,
                "exact_match_rate": exact_match_rate,
            },

            "category_accuracy": category_accuracy,
            "reason_accuracy": reason_accuracy,
        }


        logger.info("========== EVALUATION REPORT ==========")

        for key, value in metrics.items():
            logger.info("%s: %s", key, value)

        return metrics

    # ---------------------------------------------------------
    # Helper: Safe division
    # ---------------------------------------------------------

    @staticmethod
    def safe_division(
        numerator: float,
        denominator: float,
    ) -> float:

        if denominator == 0:
            return 0.0

        return numerator / denominator

    # ---------------------------------------------------------
    # Helper: Parse ground truth
    # ---------------------------------------------------------

    @staticmethod
    def parse_ground_truth(
        gold_critical_elements: Any,
    ) -> list[dict]:

        if gold_critical_elements is None:
            return []

        # Handle NaN values from pandas
        if (
            isinstance(gold_critical_elements, float)
            and math.isnan(gold_critical_elements)
        ):
            return []

        if isinstance(gold_critical_elements, str):

            if not gold_critical_elements.strip():
                return []

            gold_critical_elements = json.loads(
                gold_critical_elements
            )

        if not isinstance(gold_critical_elements, list):
            raise ValueError(
                "Ground truth must be a list of dictionaries."
            )

        if not all(isinstance(item, dict) for item in gold_critical_elements):
            raise ValueError(
                "Each ground-truth entry must be a dictionary."
            )

        return gold_critical_elements

    # ---------------------------------------------------------
    # Helper: Extract all BPMN activity IDs
    # ---------------------------------------------------------

    def get_all_activity_ids(
        self,
        bpmn_file_content: bytes | str,
    ) -> set[str]:

        activity_fields = self.pipe_line.get_only_activity_fields(
            bpmn_file_content
        )

        activity_ids = set()

        for activity_field in activity_fields:

            activity_id = (
                self.pipe_line.bpmn_data_pre_processor
                .get_sid_from_activity_field_of_bpmn_file(
                    activity_field
                )
            )

            if not activity_id:
                raise ValueError(
                    "BPMN activity without a valid ID found."
                )

            activity_ids.add(activity_id)

        return activity_ids

    # ---------------------------------------------------------
    # Helper: Extract ground-truth critical IDs
    # ---------------------------------------------------------

    @staticmethod
    def get_gold_critical_ids(
        gold_critical_elements: list[dict],
    ) -> set[str]:

        result = set()

        for element in gold_critical_elements:

            activity_id = element.get("value")

            if activity_id is None or not str(activity_id).strip():
                raise ValueError(
                    f"Ground-truth activity has no valid 'value': {element}"
                )

            result.add(str(activity_id))

        return result

    # ---------------------------------------------------------
    # Helper: Extract predicted critical IDs
    # ---------------------------------------------------------

    @staticmethod
    def get_predicted_critical_ids(
        predicted_critical_elements: list[dict],
    ) -> set[str]:

        result = set()

        for element in predicted_critical_elements:

            activity_id = element.get("id")

            if activity_id is None or not str(activity_id).strip():
                raise ValueError(
                    f"Predicted activity has no valid 'id': {element}"
                )

            result.add(str(activity_id))

        return result

    # ---------------------------------------------------------
    # Confusion matrix: TP, FP, FN, TN
    # ---------------------------------------------------------

    def calculate_confusion_matrix(
        self,
        all_activity_ids: set[str],
        gold_critical_ids: set[str],
        predicted_critical_ids: set[str],
    ) -> dict[str, int]:

        # Ground-truth IDs must exist in the BPMN document
        unknown_gold_ids = gold_critical_ids - all_activity_ids

        if unknown_gold_ids:
            raise ValueError(
                f"Ground truth contains unknown activity IDs: "
                f"{unknown_gold_ids}"
            )

        tp = len(
            gold_critical_ids & predicted_critical_ids
        )

        fp = len(
            predicted_critical_ids - gold_critical_ids
        )

        fn = len(
            gold_critical_ids - predicted_critical_ids
        )

        tn = len(
            all_activity_ids
            - gold_critical_ids
            - predicted_critical_ids
        )

        return {
            "tp": tp,
            "fp": fp,
            "fn": fn,
            "tn": tn,
        }

    # ---------------------------------------------------------
    # Exact match: Activity ID + reason
    # ---------------------------------------------------------

    def check_equal_and_predicted_equal(
        self,
        predicted_critical_elements: list[dict],
        gold_critical_elements: list[dict],
    ) -> bool:

        predicted = {
            (
                str(element.get("id", "")),
                self.normalize_text(element.get("reason")),
            )
            for element in predicted_critical_elements
        }

        gold = {
            (
                str(element.get("value", "")),
                self.normalize_text(element.get("reason")),
            )
            for element in gold_critical_elements
        }

        return predicted == gold

    # ---------------------------------------------------------
    # Category and reason accuracy
    # ---------------------------------------------------------

    def calculate_category_and_reason_accuracy(
        self,
        predicted_critical_elements: list[dict],
        gold_critical_elements: list[dict],
    ) -> dict[str, int]:

        predicted_by_id = {
            str(element["id"]): element
            for element in predicted_critical_elements
        }

        category_correct = 0
        category_total = 0

        reason_correct = 0
        reason_total = 0

        for gold_element in gold_critical_elements:

            activity_id = str(gold_element["value"])

            prediction = predicted_by_id.get(activity_id)

            # Category accuracy
            gold_category = gold_element.get("category")

            if gold_category is not None:

                category_total += 1

                if (
                    prediction is not None
                    and self.normalize_text(
                        prediction.get("category")
                    )
                    == self.normalize_text(gold_category)
                ):
                    category_correct += 1

            # Reason accuracy
            gold_reason = gold_element.get("reason")

            if gold_reason is not None:

                reason_total += 1

                if (
                    prediction is not None
                    and self.normalize_text(
                        prediction.get("reason")
                    )
                    == self.normalize_text(gold_reason)
                ):
                    reason_correct += 1

        return {
            "category_correct": category_correct,
            "category_total": category_total,
            "reason_correct": reason_correct,
            "reason_total": reason_total,
        }

    # ---------------------------------------------------------
    # Normalize strings for comparisons
    # ---------------------------------------------------------

    @staticmethod
    def normalize_text(value: Any) -> str | None:

        if value is None:
            return None

        return str(value).strip().casefold()

    # ---------------------------------------------------------
    # Delete previous evaluation logs
    # ---------------------------------------------------------

    def delete_result_file_if_exists(self):

        project_root = Path(__file__).resolve().parent.parent.parent

        logging_files = [
            project_root / "app" / "eval" / "intention.csv",
            project_root / "app" / "eval" / "sql_generation.csv",
            project_root / "app" / "eval" / "post_processing.csv",
            project_root / "app" / "eval" / "result.csv",
        ]

        for file in logging_files:

            if file.exists():
                file.unlink()

    # ---------------------------------------------------------
    # Legacy: Reset model configuration in SQLite
    # ---------------------------------------------------------

    def delete_all_models_before_run(self):

        sql_execution_component = self.pipe_line.sql_execution

        result = sql_execution_component.delete_sql(
            "DELETE FROM fallback_llm"
        )

        if isinstance(result, dict) and result.get("error"):
            raise RuntimeError(
                f"Failed to delete fallback models: {result['error']}"
            )

        logger.info("Fallback model configurations cleared.")

    # ---------------------------------------------------------
    # Legacy: Insert fallback models into SQLite
    # ---------------------------------------------------------

    def fill_models_with_fall_backs_in_db(self):

        sql_execution_component = self.pipe_line.sql_execution

        test_data = {
            "INTENTION_MODEL": [
                (
                    "openai/gpt-oss-120b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
                (
                    "openai/gpt-oss-20b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
            ],
            "SQL_GENERATION_MODEL": [
                (
                    "openai/gpt-oss-120b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
                (
                    "openai/gpt-oss-20b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
            ],
            "POST_PROCESSING_MODEL": [
                (
                    "openai/gpt-oss-120b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
                (
                    "openai/gpt-oss-20b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
            ],
            "VERIFICATION_MODEL": [
                (
                    "openai/gpt-oss-120b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
                (
                    "openai/gpt-oss-20b",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1",
                ),
            ],
            "EMBEDDING_MODEL": [
                (
                    "openai/text-embedding-3-small",
                    "OPEN_ROUTER_API_KEY",
                    "https://openrouter.ai/api/v1/embeddings",
                ),
            ],
        }

        for component_key, entries in test_data.items():

            for index, (model_name, api_key_name, base_url) in enumerate(entries):

                sql = """
                    INSERT INTO fallback_llm
                        (
                            corresponding_comment,
                            name,
                            "order",
                            model_url,
                            env_api_key_name
                        )
                    VALUES (?, ?, ?, ?, ?);
                """

                params = (
                    component_key,
                    model_name,
                    index + 1,
                    base_url,
                    api_key_name,
                )

                result = sql_execution_component.insert_sql(
                    sql,
                    params,
                )

                if isinstance(result, dict) and result.get("error"):
                    raise RuntimeError(
                        f"Failed to insert fallback model: {result['error']}"
                    )

        logger.info("All fallback models inserted successfully.")
