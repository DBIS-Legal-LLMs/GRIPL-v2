from pydantic import BaseModel, ConfigDict, Field

class SQLGenerationAnswer(BaseModel):
    model_config = ConfigDict(extra="forbid")
    query: str = Field(default="")