# ─────────────────────────────────────────────
# Stage 1 – build
# ─────────────────────────────────────────────

FROM python:3.11-slim AS builder

WORKDIR /app


RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    git \
    && rm -rf /var/lib/apt/lists/*

RUN pip install uv

COPY requirements.txt .


RUN uv pip install --system --no-cache-dir -r requirements.txt --prefix /install



# ─────────────────────────────────────────────
# Stage 2 – runtime
# ─────────────────────────────────────────────

FROM python:3.11-slim AS runtime


WORKDIR /app


COPY --from=builder /install /usr/local


COPY app/ ./app/


COPY scripts/ ./scripts/


EXPOSE 8082

COPY database-setup/init-database.sh /init-database.sh

RUN chmod +x /init-database.sh

ENTRYPOINT ["/init-database.sh"]


CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082"]