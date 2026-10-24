from dataclasses import dataclass
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