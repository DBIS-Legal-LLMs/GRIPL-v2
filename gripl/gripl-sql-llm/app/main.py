from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from .schemas import AnalysisSQLRequest, RunConfig, ComponentLLMConfig
import os
from app.database.db import Base, engine
from app.database.models import *
import json
import traceback
from app.pipeline_component.pipeline import PipelineComponent
from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.find_intention_component.find_intention import FindIntention
from app.find_intention_component.schemas import IntentionAnswer
from app.sql_execution_component.sql_execution import SQLExecution
from app.sql_generation_component.sql_generation import SQLGenerator
from app.sql_generation_component.schemas import SQLGenerationAnswer
from app.sql_post_processing_component.sql_post_processing import SQLPostProcessing
from app.sql_post_processing_component.schemas import SQLPostProcessingAnswer, SQLVerificationResultAnswer
from app.sql_post_processing_component.post_processing_mcp_client import PostProcessingMcpClient
from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager
from app.prompt_management_component.prompt_management import PromptManagement
from app.bpmn_data_pre_processor_component.bpmn_data_pre_processor import BPMNDataPreProcessor
from dotenv import load_dotenv
from app.config.component_name import (INTENTION_MODEL, SQL_GENERATION_MODEL, POST_PROCESSING_MODEL, VERIFICATION_MODEL,
                                       EMBEDDING_MODEL)
from app.logging_component.logger import Logger
from fastapi import HTTPException

load_dotenv()

app = FastAPI(
    title="GRIPL SQL LLM Service",
    description=(
        "Use Identification with SLQ LLM "
    ),
    version="0.1.0",
    root_path="/sql-llm",
)

# --------------------------------------------------------------------------
# DB initialisation
# --------------------------------------------------------------------------

Base.metadata.create_all(bind=engine)

# ---------------------------------------------------------------------------
# CORS
# ---------------------------------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    # TODO: Change this to only allow the frontend and backend origins later
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SUPPORTED_COMPONENTS = {
    "INTENTION_MODEL",
    "SQL_GENERATION_MODEL",
    "POST_PROCESSING_MODEL",
    "VERIFICATION_MODEL",
    "EMBEDDING_MODEL",
}


@app.post("/analyze", include_in_schema=False, response_model=None)
async def analyse(
    analysis_request: AnalysisSQLRequest = Depends(),
):
    try:
        print("in analyse")
        if not analysis_request.llmProps_raw:
            raise HTTPException(
                status_code=400,
                detail="LLM configuration is missing.",
            )
        print("analysis request")
        print(vars(analysis_request))
        try:
            llm_props = json.loads(analysis_request.llmProps_raw)
        except json.JSONDecodeError:
            raise HTTPException(
                status_code=400,
                detail="Invalid LLM configuration JSON.",
            )



        if not isinstance(llm_props, dict):
            raise HTTPException(
                status_code=400,
                detail="LLM configuration must be a JSON object.",
            )

        model_names_raw = llm_props.get("modelName")
        base_urls_raw = llm_props.get("baseUrl")
        api_key = llm_props.get("apiKey")

        if not isinstance(api_key, str) or not api_key.strip():
            raise HTTPException(
                status_code=400,
                detail="API key is missing.",
            )

        if not isinstance(model_names_raw, str) or not isinstance(base_urls_raw, str):
            raise HTTPException(
                status_code=400,
                detail="Model names or base URLs are missing.",
            )

        model_names = parse_component_configs(
            model_names_raw,
            "modelName",
        )

        base_urls = parse_component_configs(
            base_urls_raw,
            "baseUrl",
        )

        if model_names.keys() != base_urls.keys():
            raise HTTPException(
                status_code=400,
                detail="Model names and base URLs must contain the same components.",
            )

        missing_components = SUPPORTED_COMPONENTS - model_names.keys()

        if missing_components:
            raise HTTPException(
                status_code=400,
                detail=f"Missing model configurations: {sorted(missing_components)}",
            )

        models_by_component = {
            component: ComponentLLMConfig(
                model_name=model_names[component],
                base_url=base_urls[component],
            )
            for component in model_names
        }

        bpmn_file = await analysis_request.bpmnFile.read()

        if not bpmn_file:
            raise HTTPException(
                status_code=400,
                detail="BPMN file is empty.",
            )

        run_config = RunConfig(
            bpmn_file=bpmn_file,
            api_key=api_key,
            models_by_component=models_by_component,
        )

        pipeline_component = get_pipeline()

        result = await pipeline_component.get_analysis(run_config)

        return {
            "criticalElements": result
        }

    except HTTPException:
        raise

    except Exception:

        raise HTTPException(
            status_code=500,
            detail="Analysis failed due to an internal server error.",
        )

def get_pipeline(
):
    try:

        prompt_management = PromptManagement()

        bpmn_data_pre_processor = BPMNDataPreProcessor()

        intention_llm_handler = LLMFallBackManager(
            llm_component_name=INTENTION_MODEL,
            schema_output=IntentionAnswer,
        )

        sql_generation_llm_handler = LLMFallBackManager(
            llm_component_name=SQL_GENERATION_MODEL,
            schema_output=SQLGenerationAnswer,
        )

        post_processing_llm_handler = LLMFallBackManager(
            llm_component_name=POST_PROCESSING_MODEL,
            schema_output=SQLPostProcessingAnswer,
        )

        verification_llm_handler = LLMFallBackManager(
            llm_component_name=VERIFICATION_MODEL,
            schema_output=SQLVerificationResultAnswer,
        )

        embedding_model_handler = LLMFallBackManager(
            llm_component_name=EMBEDDING_MODEL,
            schema_output=None
        )

        post_processing_mcp_client = PostProcessingMcpClient()

        dictionary_name = "activity_example"

        collection_name = "activity_example"

        chroma_db_client = ChromaDatabaseClient(
            embedding_model_handler=embedding_model_handler,
            dictionary_name=dictionary_name,
            collection_name=collection_name,
        )

        sql_execution = SQLExecution()

        intention_logger = Logger(
            file_name="intention.csv"
        )

        sql_generation_logger = Logger(
            file_name="sql_generation.csv"
        )

        post_processed_logger = Logger(
            file_name="post_processing.csv"
        )

        intention_component = FindIntention(
            llm_handler=intention_llm_handler,
            prompt_management=prompt_management,
            sql_execution=sql_execution,
            logging_component=intention_logger,
        )

        sql_generator = SQLGenerator(
            llm_handler=sql_generation_llm_handler,
            prompt_management=prompt_management,
            logging_component=sql_generation_logger,
        )

        sql_post_processing = SQLPostProcessing(
            llm_handler=post_processing_llm_handler,
            verification_llm_handler=verification_llm_handler,
            prompt_management=prompt_management,
            post_processing_mcp_client=post_processing_mcp_client,
            logging_component=post_processed_logger
        )

        return PipelineComponent(
            chroma_db_client=chroma_db_client,
            find_intention=intention_component,
            sql_execution=sql_execution,
            sql_generation=sql_generator,
            post_processing=sql_post_processing,
            bpmn_data_pre_processor=bpmn_data_pre_processor,
        )

    except Exception:
        print(traceback.format_exc())
        raise

def parse_component_configs(raw_value: str, field_name: str) -> dict[str, str]:
    result = {}

    for entry in raw_value.split(";"):
        component, separator, value = entry.strip().partition(":")

        if not separator or not component.strip() or not value.strip():
            raise HTTPException(
                status_code=400,
                detail=f"Invalid {field_name} configuration.",
            )

        component = component.strip()
        value = value.strip()

        if component not in SUPPORTED_COMPONENTS:
            raise HTTPException(
                status_code=400,
                detail=f"Unknown component: {component}.",
            )

        if component in result:
            raise HTTPException(
                status_code=400,
                detail=f"Duplicate component: {component}.",
            )

        result[component] = value

    return result

