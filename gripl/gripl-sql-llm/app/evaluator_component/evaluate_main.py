from app.logging_component.logger import Logger
from .evaluator import Evaluator
from app.pipeline_component.pipeline import PipelineComponent
from app.data_loader_component.data_loader import DataLoader
from app.chroma_database_client.chroma_db_client import ChromaDatabaseClient
from app.find_intention_component.find_intention import FindIntention
from app.find_intention_component.schemas import IntentionAnswer
from app.sql_execution_component.sql_execution import SQLExecution
from app.sql_generation_component.sql_generation import SQLGenerator
from app.sql_generation_component.schemas import SQLGenerationAnswer
from app.sql_post_processing_component.sql_post_processing import SQLPostProcessing
from app.sql_post_processing_component.schemas import SQLPostProcessingAnswer, SQLVerificationResultAnswer
from app.sql_post_processing_component.post_processing_mcp_client import PostProcessingMcpClient
from sentence_transformers import SentenceTransformer
from app.llm_fallback_manager_component.llm_fallback_manager import LLMFallBackManager
from app.prompt_management_component.prompt_management import PromptManagement
from app.bpmn_data_pre_processor_component.bpmn_data_pre_processor import BPMNDataPreProcessor
from dotenv import load_dotenv
from app.config.component_name import (INTENTION_MODEL, SQL_GENERATION_MODEL, POST_PROCESSING_MODEL,
                                       VERIFICATION_MODEL, EMBEDDING_MODEL)
from pathlib import Path
import asyncio

load_dotenv()

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

embedding_llm_handler = LLMFallBackManager(
    llm_component_name=EMBEDDING_MODEL,
    schema_output=None
)

post_processing_mcp_client = PostProcessingMcpClient()


dictionary_name = "activity_example"

collection_name = "activity_example"

chroma_db_client = ChromaDatabaseClient(
    embedding_model_handler=embedding_llm_handler,
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

result_logger = Logger(
    file_name="result.csv"
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

pipeline = PipelineComponent(
    chroma_db_client=chroma_db_client,
    find_intention=intention_component,
    sql_execution=sql_execution,
    sql_generation=sql_generator,
    post_processing=sql_post_processing,
    bpmn_data_pre_processor=bpmn_data_pre_processor,
)

project_root = Path(__file__).resolve().parents[4]

dataset_path = project_root / "dataset" / "evaluation_data.csv"

data_loader = DataLoader(
    str(dataset_path),
)

evaluator = Evaluator(
    pipe_line=pipeline,
    data_loader=data_loader,
    logger=result_logger
)

asyncio.run(evaluator.evaluate())
