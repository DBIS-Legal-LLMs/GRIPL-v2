from typing import Type
from pydantic import BaseModel
import traceback
import instructor
import requests
from app.config.retry_configuration import MAX_RETRIES


class LLM:

    def __init__(self,
                 model_name: str,
                 model_url: str,
                 api_key: str,
                 schema_output: Type[BaseModel]
                 ):
        self.model_name = model_name
        self.model_url = model_url
        self.api_key = api_key
        self.schema_output = schema_output
        self.max_retries: int = MAX_RETRIES

    def get_answer_from_llm(self,
                            messages: list,
                            tools: list | None = None,
                            ):

        try:

            if not tools:
                return self.get_structured_answer_open_router_based(messages)

            client = instructor.from_provider(
                model=self.model_name,
                base_url=self.model_url,
                api_key=self.api_key,
            )

            raw = client.create(
                messages=messages,
                tools=tools,
                response_model=None,
                max_retries=self.max_retries,
            )

            message = raw.choices[0].message

            if message.tool_calls:
                return message

            return self.get_structured_answer_open_router_based(messages)

        except Exception:
            print("error in open router answer")
            print(traceback.format_exc())
            raise

    def get_structured_answer_open_router_based(self, messages: list):
        try:

            client = instructor.from_provider(
                model=self.model_name,
                base_url=self.model_url,
                api_key=self.api_key,
                mode=instructor.Mode.JSON,
            )

            return client.create(
                messages=messages,
                response_model=self.schema_output,
                max_retries=self.max_retries,
            )

        except Exception as e :
            print("error in llm answer with open router")
            print(traceback.format_exc())
            raise


    def get_embeddings_from_llm(self, texts: list[str]) -> list[list[float]]:

        try:
            if isinstance(texts, str):
                texts = [texts]

            response = requests.post(
                self.model_url,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model_name,
                    "input": texts,
                },
                timeout=120,
            )

            response.raise_for_status()

            data = response.json()["data"]

            data = sorted(
                data,
                key=lambda item: item["index"],
            )

            return [
                item["embedding"]
                for item in data
            ]
        except Exception as e:
            print("error in getting embedding in llm ")
            print(traceback.format_exc())
            return []


