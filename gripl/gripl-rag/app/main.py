"""
GRIPL-RAG FastAPI application entry point.

Run with:
    uvicorn app.main:app --reload --port 8081
"""
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router as rag_router
from app.api.evaluation_routes import router as evaluation_router
from app.api.pdf_routes import router as pdf_router

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
)

# ---------------------------------------------------------------------------
# FastAPI app
# ---------------------------------------------------------------------------
# TODO: Disable docs in production (e.g., FastAPI(docs_url=None)) later
app = FastAPI(
    title="GRIPL RAG Service",
    description=(
        "Retrieval-Augmented Generation API for the GRIPL project. "
        "Queries a LightRAG knowledge graph built from GDPR-related "
        "legal documents and returns relevant context."
    ),
    version="0.1.0",
    root_path="/rag",
)

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

# ---------------------------------------------------------------------------
# Register routers
# ---------------------------------------------------------------------------
app.include_router(rag_router)
app.include_router(evaluation_router)
app.include_router(pdf_router)


@app.get("/", tags=["Root"])
async def root():
    """Root endpoint – redirects to the docs."""
    return {
        "service": "gripl-rag",
        "docs": app.docs_url,
        "health": "/api/health",
    }
