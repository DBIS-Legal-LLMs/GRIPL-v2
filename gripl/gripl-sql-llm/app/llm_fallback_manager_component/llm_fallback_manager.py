import traceback
from typing import Type
from pydantic import BaseModel
from app.llm_component.llm import LLM
from app.sql_execution_component.sql_execution import SQLExecution
from dotenv import load_dotenv
import os
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)
from instructor.core.exceptions import InstructorRetryException
from app.config.retry_configuration import MAX_RETRIES

load_dotenv()


class LLMFallBackManager:

    def __init__(self,
                 llm_component_name: str,
                 schema_output: Type[BaseModel]
                 ):
        self.llm_component_name = llm_component_name
        self.schema_output = schema_output
        self.current_llm_index = 0
        self.sql_execution_component = SQLExecution()

    def get_answer_with_fallback(self,
                                 messages: list,
                                 tools: list | None = None,
                                 ):

        try:

            models_env_api_key_list = self.get_all_possible_llm_models_of_component(self.llm_component_name)

            for model, env_api_key_name, model_url in models_env_api_key_list:
                try:
                    return self.get_answer_from_current_llm(
                        messages=messages,
                        tools=tools,
                        model_name=model,
                        model_url=model_url,
                        api_key_env_name=env_api_key_name,
                    )

                except InstructorRetryException:
                    print(f"Model {model} failed after all retries. Trying next model...")
                    continue

            return self.schema_output()

        except Exception as e:
            print("fehler in llm handler")
            print(traceback.format_exc())
            return self.schema_output()

    def get_all_possible_llm_models_of_component(self, component_name: str) -> list:

        try:
            sql = """
                  SELECT name, \
                         env_api_key_name, \
                         model_url
                  FROM fallback_llm
                  WHERE corresponding_comment = ?
                  ORDER BY "order" ASC; \
                  """

            results = self.sql_execution_component.get_sql_query_results(
                sql,
                (component_name,),
            )

            return [
                (
                    execution_result.get("name", ""),
                    execution_result.get("env_api_key_name", ""),
                    execution_result.get("model_url", ""),
                )
                for execution_result in results
            ]

        except Exception:
            
            raise

    @retry(
        retry=retry_if_exception_type(InstructorRetryException),
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(min=2, max=5),
        reraise=True,
    )
    def get_answer_from_current_llm(self,
                                    messages: list,
                                    model_name: str,
                                    model_url: str,
                                    api_key_env_name: str,
                                    tools: list | None = None,
                                    ):
        try:
            api_key = self._resolve_api_key(api_key_env_name)
            llm = LLM(
                model_name=model_name,
                model_url=model_url,
                api_key=api_key,
                schema_output=self.schema_output
            )

            return   llm.get_answer_from_llm(
                messages=messages,
                tools=tools,
            )


        except Exception as e:
            print(traceback.format_exc())
            raise

    def get_embedding_of_current_embedding_model(self,
                                                 documents:list[str]
                                                 ):

        try:

            models_env_api_key_list = self.get_all_possible_llm_models_of_component(self.llm_component_name)

            for model, env_api_key_name, model_url in models_env_api_key_list:
                try:
                    api_key = self._resolve_api_key(env_api_key_name)
                    llm = LLM(
                        model_name=model,
                        model_url=model_url,
                        api_key=api_key,
                        schema_output=None
                    )

                    return llm.get_embeddings_from_llm(
                        texts=documents,
                    )

                except Exception as e:
                    print(f"Model {model} failed after all retries. Trying next model...")
                    print("get error in getting embedidng")
                    print(traceback.format_exc())
                    continue

            return []

        except Exception as e:
            print("get error in getting embedidng")
            print(traceback.format_exc())
            return []

    def _resolve_api_key(self, api_key_value: str) -> str:
        """
        Resolves an environment variable name or accepts an actual API key.

        Local evaluation:
            "OPEN_ROUTER_API_KEY" -> actual API key from environment

        Web evaluation:
            actual API key -> actual API key
        """

        if not api_key_value:
            raise ValueError("API key is missing.")

        resolved_api_key = os.getenv(api_key_value)

        if resolved_api_key:
            return resolved_api_key

        return api_key_value