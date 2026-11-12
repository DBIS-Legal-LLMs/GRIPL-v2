from typing import Literal

from pydantic import BaseModel, ConfigDict, Field

class SQLPostProcessingAnswer(BaseModel):
    model_config = ConfigDict(extra="forbid")
    final_query: str = Field(default="")

class SQLVerificationResultAnswer(BaseModel):
    model_config = ConfigDict(extra="forbid")
    intention_status: Literal["unchanged", "changed"] = Field(default="unchanged")
    reason_status: Literal["unchanged", "changed"] = Field(default="unchanged")

    intention: str = Field(default="")
    reason: str = Field(default="")
    explanation: str = Field(default="")
    final_query: str = Field(default="")