from app.logging_component.logger import Logger
from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager
from app.prompt_management_component.prompt_management import PromptManagement
from app.sql_execution_component.sql_execution import SQLExecution
from pathlib import Path
import traceback
import logging

logger = logging.getLogger(__name__)

class FindIntention:

    def __init__(self,
                 llm_handler: LLMFallBackManager,
                 prompt_management: PromptManagement,
                 sql_execution: SQLExecution,
                 logging_component: Logger,
                 ):
        self.llm_handler = llm_handler
        self.prompt_management = prompt_management
        self.sql_execution = sql_execution
        self.logging_component = logging_component

        base_dir = Path(__file__).parent

        self.system_prompt_path = base_dir / "system_prompt.txt"
        self.user_prompt_path = base_dir / "user_prompt.txt"

    def find_intention_of_current_activity_field(self, activity_field: str)->list[str]:
        try:

            available_intents = self.get_all_intentions()

            user_prompt = self.prompt_management.fill_prompt(
                self.user_prompt_path,
                activity_field=activity_field,
                available_intents=available_intents,
            )

            system_prompt = self.prompt_management.fill_prompt(
                self.system_prompt_path,
            )

            messages = [
                {
                    "role": "system",
                    "content": system_prompt,
                },
                {
                    "role": "user",
                    "content": user_prompt,
                },
            ]

            answer = self.llm_handler.get_answer_with_fallback(
                messages,
            ).intents


            print("current intention")
            print(answer)

            self.logging_component.log(
                user_prompt,
                system_prompt,
                answer,
            )

            return answer

        except Exception as e:
            print(traceback.format_exc())
            print(e)
            return []

    def get_all_intentions(self,)->list[str]:
        try:
            row = self.sql_execution.get_sql_query_results("SELECT category.name FROM category")

            return [r.get('name', '') for r in row ]
        except Exception as e:
            print("in intent finding")
            print(traceback.format_exc())
            print(e)
            return []

