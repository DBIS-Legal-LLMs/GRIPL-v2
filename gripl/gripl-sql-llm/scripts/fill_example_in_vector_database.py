from pathlib import Path
import pandas as pd
import json
from app.llm_component.llm import LLM
from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.bpmn_data_pre_processor_component.bpmn_data_pre_processor import BPMNDataPreProcessor
from dotenv import load_dotenv
import os
from app.config.component_name import EMBEDDING_MODEL
import traceback
from app.sql_execution_component.sql_execution import SQLExecution
from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager

load_dotenv()


def build_sql_query(category: str, reason: str):
    return f"""
        SELECT r.reason 
        FROM reason r JOIN category_reason_association cra ON r.id = cra.reason_id 
        JOIN category c ON c.id = cra.category_id 
        WHERE c.name = '{category}' AND r.reason = '{reason}'
"""


def get_embedding_model(
        component_name: str,
):
    try:

        sql_execution_component = SQLExecution()

        sql = f"""
                                SELECT name, 
                                model_url, 
                                env_api_key_name 
                                FROM fallback_llm
                                 WHERE corresponding_comment = '{component_name}'
                                ORDER BY "order" ASC
                                LIMIT 1;

                        """

        return [(execution_result.get("name", ""),
                 execution_result.get("model_url", ""),
                 execution_result.get("env_api_key_name", "")
                 ) for execution_result in
                sql_execution_component.get_sql_query_results(sql)][0]

    except Exception:
        print(traceback.format_exc())
        return []


data_path = Path(__file__).parents[3] / "dataset" / "evaluation_data.csv"

df = pd.read_csv(data_path)

data_into_vector_database = [
    {
        "row": 8,
        "activity_field_id": "sid-E242E6B3-FB76-4A9A-89FB-A11DD4A16FBA14-A2E4-CD0FAE1D636E",
        "category": "Access"
    },
    {
        "row": 8,
        "activity_field_id": "sid-563F0ED1-DA1F-4145-9B71-775A412021EB",
        "category": "Access"
    },
    {
        "row": 8,
        "activity_field_id": "sid-7CC416D3-B929-460C-9E2A-4430A58A5DE7",
        "category": "Collection"
    },
    {
        "row": 8,
        "activity_field_id": "sid-F851F1C8-D476-48B0-B163-B888C57D9AFE",
        "category": "Collection"
    },
    {
        "row": 9,
        "activity_field_id": "sid-20FD33F5-ADEB-428F-AC40-58F6C01C5F3A",
        "category": "Usage"
    },
    {
        "row": 11,
        "activity_field_id": "sid-BBCC1036-FF64-4FA0-B0CB-E16651DD1649",
        "category": "Access"
    },
    {
        "row": 12,
        "activity_field_id": "sid-18CEBA33-917E-40DE-9EC9-ACDDD3587422",
        "category": "Collection"
    },
    {
        "row": 12,
        "activity_field_id": "sid-C518C3D7-08CD-45FF-B2A3-A9AFB402EACF",
        "category": "Collection"
    },
    {
        "row": 12,
        "activity_field_id": "sid-36BAAFE7-9AEF-40F0-B8DF-11F485BD3E78",
        "category": "Access"
    },
    {
        "row": 13,
        "activity_field_id": "sid-1177E778-9B60-4A48-895E-82DAAC180C9E",
        "category": "Access"
    },
    {
        "row": 13,
        "activity_field_id": "sid-4FAB8B03-8311-42F4-91B2-BDC72772535E",
        "category": "Collect"
    },
    {
        "row": 14,
        "activity_field_id": "sid-96D618DB-9481-472D-A470-FA1AF1C512D6",
        "category": "Usage"
    },
    {
        "row": 14,
        "activity_field_id": "sid-E6D606BE-E68F-47F3-9B7A-B89B778BA550",
        "category": "Collect"
    },
    {
        "row": 14,
        "activity_field_id": "sid-1B1BE497-3F85-4A78-8294-85F51CF58933",
        "category": "Access"
    },
    {
        "row": 14,
        "activity_field_id": "sid-9EBF4ECB-075E-4B12-9DBB-6B11330DF00C",
        "category": "Modification"
    },
    {
        "row": 15,
        "activity_field_id": "sid-FEF6E086-4453-4529-A655-A1CD93CFA631",
        "category": "Transferal"
    },
    {
        "row": 15,
        "activity_field_id": "sid-8296CDBE-FA70-4131-B122-953362768E48",
        "category": "Modification"
    },
    {
        "row": 15,
        "activity_field_id": "sid-35302D57-34E2-44BB-B3A5-1E42D00C68EC",
        "category": "Modification"
    },
    {
        "row": 16,
        "activity_field_id": "sid-F9D1CF99-6047-4EE5-BD01-E616F8E86527",
        "category": "Modification"
    },
    {
        "row": 16,
        "activity_field_id": "sid-87E6CC43-F782-4F48-B905-BC69F0E9E619",
        "category": "Access"
    },
    {
        "row": 16,
        "activity_field_id": "sid-E09CB8DD-CEC3-43B2-A362-5362C7842065",
        "category": "Access"
    },
    {
        "row": 16,
        "activity_field_id": "sid-3EFF04BA-EC3F-49DE-BFB5-83ACB3217926",
        "category": "Usage"
    },
    {
        "row": 16,
        "activity_field_id": "sid-E5EE26F3-76FD-494F-9E66-A0C7A854FD8A",
        "category": "Usage"
    },
    {
        "row": 16,
        "activity_field_id": "sid-CB47348C-5462-43DF-9029-631DD8C5F1FA",
        "category": "Transferal"
    },
    {
        "row": 16,
        "activity_field_id": "sid-B33E91ED-46BB-47F2-A1E1-5FD46EFD850C",
        "category": "Transferal"
    },
    {
        "row": 21,
        "activity_field_id": "sid-35E7B43A-A7A0-4147-BD0A-B3E81C2B292F",
        "category": "Storage"
    },
    {
        "row": 21,
        "activity_field_id": "sid-503F8F5D-6B24-4020-97CC-67FDD274D1F4",
        "category": "Modification"
    },
    {
        "row": 21,
        "activity_field_id": "sid-52C67D4C-A188-48A5-92B8-ABD79F14417C",
        "category": "Collection"
    },
    {
        "row": 21,
        "activity_field_id": "sid-173FE34D-6ED1-4D01-B0E3-A70FD0DAAA3D",
        "category": "Collection"
    },
    {
        "row": 21,
        "activity_field_id": "sid-8EED8EA0-011B-47EB-A0ED-ED841C8F6D52",
        "category": "Usage"
    },
    {
        "row": 21,
        "activity_field_id": "sid-FE5E482F-E85A-45A4-A50F-8F905455E730",
        "category": "Usage"
    },
    {
        "row": 22,
        "activity_field_id": "Activity_1ikpcwz",
        "category": "Transferal"
    },
    {
        "row": 22,
        "activity_field_id": "Activity_1vbekrj",
        "category": "Transferal"
    },
    {
        "row": 25,
        "activity_field_id": "Activity_1jmvap9",
        "category": "Transferal"
    },
    {
        "row": 26,
        "activity_field_id": "sid-4F9C8D16-F28B-455C-B231-64950CC55CF5",
        "category": "Storage"
    },
    {
        "row": 30,
        "activity_field_id": "sid-65B4E414-5E32-47F3-8CD1-49E2EB22A8B9",
        "category": "Storage"
    },
    {
        "row": 30,
        "activity_field_id": "sid-44714A30-7EA2-4EF7-AADA-CFBE92BB4826",
        "category": "Collection"
    },
    {
        "row": 30,
        "activity_field_id": "sid-91544046-99F6-46D2-8B1D-19BFF462C976",
        "category": "Collection"
    },
    {
        "row": 30,
        "activity_field_id": "sid-6CD7CAF0-7B2A-48BC-B54E-C7599689324C",
        "category": "Usage"
    },
    {
        "row": 30,
        "activity_field_id": "sid-D8B4F61C-1639-43FE-B408-E95125D5BC3C",
        "category": "Usage"
    },
    {
        "row": 30,
        "activity_field_id": "sid-D5E51C27-1F63-49F0-A709-0140EEE68DB5",
        "category": "Modification"
    },
    {
        "row": 30,
        "activity_field_id": "sid-C61635D2-E3DB-4EB8-95BA-563D7F0D2A2E",
        "category": "Modification"
    },
    {
        "row": 31,
        "activity_field_id": "sid-14183816-D7CD-4AF7-A2E9-208E151796AE ",
        "category": "Access"
    },
    {
        "row": 31,
        "activity_field_id": "sid-54418E5F-348A-474E-A214-F556D67EE515",
        "category": "Storage"
    },
    {
        "row": 34,
        "activity_field_id": "abpu-b0399020c4b54c4fa3a4761cf43b01e1",
        "category": "Modification"
    },
    {
        "row": 34,
        "activity_field_id": "sid-1v3m-b0399020c4b54c4fa3a4761cf43b01e1",
        "category": "Transferal"
    },
    {
        "row": 34,
        "activity_field_id": "abo9-b0399020c4b54c4fa3a4761cf43b01e1",
        "category": "Access"
    },
]

bpmn_data_preprocessor = BPMNDataPreProcessor()

dictionary_name = "activity_example"

collection_name = "activity_example"


embedding_llm_handler = LLMFallBackManager(
    llm_component_name=EMBEDDING_MODEL,
    schema_output=None
)

chroma_db_client = ChromaDatabaseClient(
    embedding_model_handler=embedding_llm_handler,
    dictionary_name=dictionary_name,
    collection_name=collection_name,
)

data_to_fill = []

for item in data_into_vector_database:

    current_category = item["category"]

    current_row = int(item.get("row", "0"))

    current_sid = item.get("activity_field_id", "")

    current_expected_values = json.loads(df.iloc[current_row].get("expected_values", []))

    current_bpmn_file = df.iloc[current_row].get("bpmn_xml", "")

    for val_reason in current_expected_values:

        all_activities_field = bpmn_data_preprocessor.get_only_activity_field_of_bpmn_file(current_bpmn_file)

        for activity in all_activities_field:

            activity_sid = bpmn_data_preprocessor.get_sid_from_activity_field_of_bpmn_file(activity)

            if activity_sid == current_sid and current_sid == val_reason.get("value", ""):
                activity_name = bpmn_data_preprocessor.get_name_from_activity_field_of_bpmn_file(activity)

                reason = val_reason.get("reason", "")

                data_to_fill.append({
                    "meta_data": {
                        "sql": build_sql_query(current_category, reason)
                    },
                    "content": activity_name
                })

chroma_db_client.fill_collection_with_meta_data(data_to_fill)
