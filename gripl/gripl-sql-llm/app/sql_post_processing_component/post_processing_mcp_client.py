from mcp import ClientSession
from mcp.client.sse import sse_client
import traceback
from app.bpmn_data_pre_processor_component.bpmn_data_pre_processor import BPMNDataPreProcessor
from typing import TYPE_CHECKING
import json
from app.find_intention_component.schemas import IntentionAnswer
from app.sql_post_processing_component.schemas import SQLVerificationResultAnswer

if TYPE_CHECKING:
    from .sql_post_processing import SQLPostProcessing
from dotenv import load_dotenv
import os

load_dotenv()


class PostProcessingMcpClient:

    def __init__(self):
        self.max_iteration = 3
        self.bpmn_data_pre_processor = BPMNDataPreProcessor()
        self.server_url = os.getenv("MCP_SERVER_URL", "")

    async def get_mcp_client_answer(self,
                                    db_schema: str,
                                    activity_field: str,
                                    generated_query: str,
                                    error_message: str,
                                    intentions: list[str],
                                    reasons_of_intentions: list[str],
                                    sql_post_processing_component: "SQLPostProcessing"
                                    ):

        try:

            async with sse_client(self.server_url) as (reader, writer):
                async with ClientSession(reader, writer) as session:
                    await session.initialize()

                    tools_result = await session.list_tools()
                    mcp_tools = tools_result.tools

                    current_intention = intentions[0]
                    current_query = generated_query
                    current_reasons_list = reasons_of_intentions
                    current_error_message = error_message
                    current_hint = ""

                    groq_tools = self.convert_mcp_tool_list_to_groq_schema_tool_list(mcp_tools)

                    verification_system_prompt = sql_post_processing_component.prompt_management.fill_prompt(
                        sql_post_processing_component.verification_system_prompt_path)

                    verification_component_messages = [
                        {
                            "role": "system",
                            "content": verification_system_prompt,
                        },
                    ]

                    for iteration in range(1, self.max_iteration + 1):

                        messages_backup = list(verification_component_messages)

                        try:

                            current_query = sql_post_processing_component.current_post_processed_query(
                                db_schema=db_schema,
                                activity_field=activity_field,
                                generated_query=current_query,
                                error_message=current_error_message,
                                intentions=current_intention,
                                reasons_of_intentions=current_reasons_list,
                                hint=current_hint,
                            )

                            current_iteration_user_prompt = sql_post_processing_component.prompt_management.fill_prompt(
                                sql_post_processing_component.verification_user_prompt_path,
                                activity_field=activity_field,
                                intention=current_intention,
                                generated_query=current_query,
                                reasons_of_intentions=current_reasons_list,
                                db_schema=db_schema,
                            )


                            verification_component_messages.append({
                                "role": "user",
                                "content": current_iteration_user_prompt,
                            })



                            for i in range(1, self.max_iteration + 1):
                                verification_answer = sql_post_processing_component.verification_llm_handler.get_answer_with_fallback(
                                    messages=verification_component_messages[:],
                                    tools=groq_tools,
                                )

                                tool_calls = getattr(verification_answer, "tool_calls", None)

                                if tool_calls:

                                    verification_component_messages.append({
                                        "role": "assistant",
                                        "content": verification_answer.content,
                                        "tool_calls": [
                                            {
                                                "id": tc.id,
                                                "type": tc.type,
                                                "function": {
                                                    "name": tc.function.name,
                                                    "arguments": tc.function.arguments,
                                                },
                                            }
                                            for tc in tool_calls
                                        ],
                                    })

                                    tools_messages = await self.execute_tools_from_answer(
                                        tool_calls, session
                                    )
                                    verification_component_messages.extend(tools_messages)

                                    continue


                                break

                            verification_component_messages.append({
                                "role": "assistant",
                                "content": verification_answer.model_dump_json(),
                            })

                            if isinstance(verification_answer, SQLVerificationResultAnswer):

                                if (verification_answer.intention_status == "unchanged"
                                    and verification_answer.reason_status == "unchanged"):

                                        sql_post_processing_component.logging_component.log(
                                            str(verification_component_messages),
                                            verification_system_prompt,
                                            verification_answer.final_query,
                                        )
                                        answer =  verification_answer.final_query

                                        return answer

                                current_intention = verification_answer.intention
                                current_reasons_list = [verification_answer.reason]
                                current_hint = verification_answer.explanation
                                current_query = verification_answer.final_query

                        except Exception:
                            print("error in one iteration")
                            print(traceback.format_exc())
                            verification_component_messages = messages_backup
                            continue

                    final_query = sql_post_processing_component.llm_handler.get_answer_with_fallback(
                        verification_component_messages[:]
                    ).final_query

                    sql_post_processing_component.logging_component.log(
                        str(verification_component_messages),
                        verification_system_prompt,
                        final_query,
                    )

                    print("post processed query after all iterations")
                    print(answer)

                    return final_query

        except Exception:
            print("error in mcp client")
            print(traceback.format_exc())


    def convert_mcp_tool_list_to_groq_schema_tool_list(self, mcp_tools: list):
        groq_tools = []
        for tool in mcp_tools:
            params = tool.inputSchema.copy()

            params.pop('title', None)
            params.pop('description', None)

            if 'type' not in params or params['type'] != 'object':
                params['type'] = 'object'

            properties = params.get('properties', {})
            if not isinstance(properties, dict):
                properties = {}
            for prop_name, prop_schema in list(properties.items()):
                if not isinstance(prop_schema, dict):
                    properties[prop_name] = {"type": "string"}
                prop_schema.pop('title', None)
                if 'description' not in prop_schema:
                    prop_schema['description'] = prop_name.replace('_', ' ')
                if 'type' not in prop_schema:
                    prop_schema['type'] = 'string'
            params['properties'] = properties

            if 'required' not in params and properties:
                params['required'] = list(properties.keys())

            groq_tools.append({
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description or "",
                    "parameters": params
                }
            })
        return groq_tools

    async def execute_tools_from_answer(self, tools: list,
                                        session
                                        ):

        try:

            tool_messages = []

            for tool_call in tools:
                tool_name = tool_call.function.name
                tool_args = json.loads(tool_call.function.arguments or "{}")

                mcp_result = await session.call_tool(tool_name, tool_args)

                parts = []
                for block in mcp_result.content:
                    text = getattr(block, "text", None)
                    if text is not None:
                        parts.append(text)
                    else:
                        parts.append(json.dumps(block.model_dump(), ensure_ascii=False))
                tool_result_text = "\n".join(parts)

                tool_messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "name": tool_name,
                    "content": tool_result_text,
                })

            return tool_messages
        except Exception as e:
            print("error in tool usage")
            print(traceback.format_exc())
            return []
