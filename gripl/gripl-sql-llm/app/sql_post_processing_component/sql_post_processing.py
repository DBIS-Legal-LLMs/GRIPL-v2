from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager
from app.logging_component.logger import Logger
from app.prompt_management_component.prompt_management import PromptManagement
from .post_processing_mcp_client import PostProcessingMcpClient
from pathlib import Path
import traceback


class SQLPostProcessing:

    def __init__(self,
                 llm_handler: LLMFallBackManager,
                 verification_llm_handler: LLMFallBackManager,
                 prompt_management: PromptManagement,
                 post_processing_mcp_client: PostProcessingMcpClient,
                 logging_component: Logger,
                 ):
        self.llm_handler = llm_handler
        self.verification_llm_handler = verification_llm_handler
        self.prompt_management = prompt_management
        self.post_processing_mcp_client = post_processing_mcp_client
        self.logging_component = logging_component

        base_dir = Path(__file__).parent

        self.system_prompt_path = base_dir / "system_prompt.txt"
        self.user_prompt_path = base_dir / "user_prompt.txt"

        self.verification_system_prompt_path = base_dir / "verification_system_prompt.txt"
        self.verification_user_prompt_path = base_dir / "verification_user_prompt.txt"

    async def post_process_generated_query(self,
                                           db_schema: str,
                                           activity_field: str,
                                           generated_query: str,
                                           error_message: str,
                                           intentions: list[str],
                                           reasons_of_intentions: list[str],
                                           ) -> str:
        try:

            return await self.post_processing_mcp_client.get_mcp_client_answer(
                db_schema,
                activity_field,
                generated_query,
                error_message,
                intentions,
                reasons_of_intentions,
                self
            )

        except Exception as e:
            print("e in post_processing")
            print(traceback.format_exc())
            print(e)
            return ""

    def current_post_processed_query(self,
                                     db_schema: str,
                                     activity_field: str,
                                     generated_query: str,
                                     error_message: str,
                                     intentions: list[str],
                                     reasons_of_intentions: list[str],
                                     hint:str
                                     ) -> str:

        try:

            post_processed_user_prompt = self.prompt_management.fill_prompt(
                self.user_prompt_path,
                db_schema=db_schema,
                activity_field=activity_field,
                generated_query=generated_query,
                error_message=error_message,
                intent=intentions[0],
                reasons_of_intentions=",".join(reasons_of_intentions),
                hint=hint
            )

            post_processed_system_prompt = self.prompt_management.fill_prompt(
                self.system_prompt_path,
            )

            messages = [
                {
                    "role": "user",
                    "content": post_processed_user_prompt,
                },
                {
                    "role": "system",
                    "content": post_processed_system_prompt,
                }
            ]

            answer = self.llm_handler.get_answer_with_fallback(
                messages,
            ).final_query

            self.logging_component.log(
                post_processed_user_prompt,
                post_processed_system_prompt,
                answer,
            )

            return answer

        except Exception as e:
            print("e in post_processing")
            print(traceback.format_exc())
            return ""

