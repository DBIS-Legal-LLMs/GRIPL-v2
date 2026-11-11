import traceback

from dotenv import load_dotenv

from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.config.component_name import EMBEDDING_MODEL
from app.llm_fallback_manager_component.llm_fallback_manager import (
    LLMFallBackManager,
)
from app.sql_execution_component.sql_execution import SQLExecution


load_dotenv()


def get_embedding_model(component_name: str):
    """Return the first configured embedding model for a component."""
    try:
        sql_execution_component = SQLExecution()

        sql = f"""
            SELECT
                name,
                model_url,
                env_api_key_name
            FROM fallback_llm
            WHERE corresponding_comment = '{component_name}'
            ORDER BY "order" ASC
            LIMIT 1;
        """

        results = sql_execution_component.get_sql_query_results(sql)
        if not results:
            return []

        result = results[0]
        return (
            result.get("name", ""),
            result.get("model_url", ""),
            result.get("env_api_key_name", ""),
        )

    except Exception:
        print(traceback.format_exc())
        return []


# Chroma configuration: same collection names as in the original script.
dictionary_name = "activity_example"
collection_name = "activity_example"

embedding_llm_handler = LLMFallBackManager(
    llm_component_name=EMBEDDING_MODEL,
    schema_output=None,
)

chroma_db_client = ChromaDatabaseClient(
    embedding_model_handler=embedding_llm_handler,
    dictionary_name=dictionary_name,
    collection_name=collection_name,
)


# Synthetic few-shot examples. No BPMN or gold annotation is loaded from
# evaluation_data.csv. Each item keeps the original Chroma data structure:
#   content   -> activity name used for similarity search
#   meta_data -> SQL query returned as the few-shot example
#
# The SQL reason strings below must match reason_catalog in
# scripts/insert_data_in_db.py exactly.
data_to_fill = [
    # Collection (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Collection'
                AND r.reason = 'Personal data is collected directly from the data subject.'
            """
        },
        "content": "Register nature reserve permit holder",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Collection'
                AND r.reason = 'Personal data is obtained from an external source or another system.'
            """
        },
        "content": "Import emergency contact details from partner registry",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Collection'
                AND r.reason = 'Personal data is collected directly from the data subject.'
            """
        },
        "content": "Capture details for community sports pass",
    },

    # Storage (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Storage'
                AND r.reason = 'Personal data is stored or retained in a database, file, or other storage system.'
            """
        },
        "content": "Archive museum conservation case records",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Storage'
                AND r.reason = 'Personal data is organized or maintained in structured records.'
            """
        },
        "content": "Maintain regional alumni directory",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Storage'
                AND r.reason = 'Personal data is stored or retained in a database, file, or other storage system.'
            """
        },
        "content": "Retain property inspection records with resident details",
    },

    # Usage (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Usage'
                AND r.reason = 'Personal data is used to carry out an operational or business activity.'
            """
        },
        "content": "Calculate eligibility for public transport subsidy",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Usage'
                AND r.reason = 'Personal data is analyzed or evaluated to produce information, insights, or decisions.'
            """
        },
        "content": "Assess applicant suitability for community grant",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Usage'
                AND r.reason = 'Personal data is analyzed or evaluated to produce information, insights, or decisions.'
            """
        },
        "content": "Analyze passenger travel patterns for route planning",
    },

    # Transferal (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Transferal'
                AND r.reason = 'Personal data is transmitted between internal systems or organizational units.'
            """
        },
        "content": "Forward chapter member details to regional office",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Transferal'
                AND r.reason = 'Personal data is disclosed or transmitted to an external recipient or third party.'
            """
        },
        "content": "Send contractor access details to building management firm",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Transferal'
                AND r.reason = 'Personal data is disclosed or transmitted to an external recipient or third party.'
            """
        },
        "content": "Transmit resident support referral to municipal office",
    },

    # Modification (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Modification'
                AND r.reason = 'Existing personal data is corrected or updated.'
            """
        },
        "content": "Correct date of birth in permit record",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Modification'
                AND r.reason = 'Personal data is enriched, combined, or otherwise altered.'
            """
        },
        "content": "Enrich supplier contact profile with verified details",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Modification'
                AND r.reason = 'Existing personal data is corrected or updated.'
            """
        },
        "content": "Revise vehicle permit holder details",
    },

    # Deletion (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Deletion'
                AND r.reason = 'Personal data is erased or destroyed from a system or record.'
            """
        },
        "content": "Remove expired visitor permit records",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Deletion'
                AND r.reason = 'Personal data is erased or destroyed from a system or record.'
            """
        },
        "content": "Erase former committee member profile",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Deletion'
                AND r.reason = 'Personal data is anonymized or pseudonymized as part of a processing operation.'
            """
        },
        "content": "Anonymize public consultation submissions",
    },

    # Access (3 examples)
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Access'
                AND r.reason = 'A person or system retrieves, queries, or views personal data.'
            """
        },
        "content": "Retrieve archived tenancy correspondence",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Access'
                AND r.reason = 'A person or system retrieves, queries, or views personal data.'
            """
        },
        "content": "View participant dietary requirements for event planning",
    },
    {
        "meta_data": {
            "sql": """
                SELECT r.reason
                FROM reason r
                JOIN category_reason_association cra ON r.id = cra.reason_id
                JOIN category c ON c.id = cra.category_id
                WHERE c.name = 'Access'
                AND r.reason = 'Access permissions for personal data are granted, changed, or revoked.'
            """
        },
        "content": "Grant case worker access to resident records",
    },

]


# Basic checks before insertion.
if len(data_to_fill) != 21:
    raise ValueError(f"Expected 21 few-shot examples, got {len(data_to_fill)}")

for index, example in enumerate(data_to_fill, start=1):
    if not example.get("content") or not example.get("meta_data", {}).get("sql"):
        raise ValueError(
            f"Few-shot example {index} is missing content or SQL metadata"
        )


# Insert the 21 synthetic examples into the activity_example collection.
chroma_db_client.fill_collection_with_meta_data(data_to_fill)

print(f"Inserted {len(data_to_fill)} synthetic few-shot examples.")
