from dataclasses import dataclass, field
from typing import Optional
from fastapi import File, UploadFile, Form

@dataclass
class AnalysisSQLRequest:
    def __init__(
        self,
        bpmnFile: UploadFile = File(...),
        useRag: bool = Form(False),
        ragMode: Optional[str] = Form(None),
        useSQLLM: bool = Form(False),
        activitiesOnly: bool = Form(False),
        llmProps: Optional[str] = Form(None),
    ):
        self.bpmnFile = bpmnFile
        self.useRag = useRag
        self.ragMode = ragMode
        self.useSQLLM = useSQLLM
        self.activitiesOnly = activitiesOnly
        self.llmProps_raw = llmProps


@dataclass(frozen=True)
class ComponentLLMConfig:
    model_name: str
    base_url: str


@dataclass(frozen=True)
class RunConfig:
    bpmn_file: bytes = field(repr=False)
    api_key: str = field(repr=False)
    models_by_component: dict[str, ComponentLLMConfig] = field(repr=False)

    def get_model_config(self, component_name: str) -> ComponentLLMConfig:
        try:
            return self.models_by_component[component_name]
        except KeyError:
            raise ValueError(
                f"No model configuration found for {component_name}"
            )