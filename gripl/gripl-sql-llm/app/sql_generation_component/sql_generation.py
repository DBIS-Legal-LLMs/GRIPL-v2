from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager
from app.logging_component.logger import Logger
from app.prompt_management_component.prompt_management import PromptManagement
from pathlib import Path
import traceback




class SQLGenerator:

    def __init__(self,
                 llm_handler: LLMFallBackManager,
                 prompt_management: PromptManagement,
                 logging_component: Logger,
                 ):
        self.llm_handler = llm_handler
        self.prompt_management = prompt_management
        self.logging_component = logging_component

        base_dir = Path(__file__).parent

        self.system_prompt_path = base_dir / "system_prompt.txt"
        self.user_prompt_path = base_dir / "user_prompt.txt"


    def generate_query(self,
                       activity_field: str,
                       db_schema:str,
                       intentions: list[str],
                       reasons_of_intentions: list[str],
                       few_shot_examples: str
                       )->str:
        try:
            user_prompt = self.prompt_management.fill_prompt(
                self.user_prompt_path,
                activity_field=activity_field,
                db_schema=db_schema,
                intent=intentions,
                reasons_of_intentions=reasons_of_intentions,
                few_shot_examples=few_shot_examples
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
                messages
            ).query

            self.logging_component.log(
                user_prompt,
                system_prompt,
                answer,
            )

            return answer
        except Exception as e:
            print("in sql generating ")
            print(traceback.format_exc())
            print(e)
            return ""

