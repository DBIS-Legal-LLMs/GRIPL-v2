# GRIPL - GDPR Risk Identification in Processes using Large Language Models

Identifying GDPR-Critical Tasks in Business Processes using Large Language Models

## Overview

This repository contains the source code and research artifacts for the GRIPL framework. The system consists of a frontend, a backend, and a PostgreSQL database.

In addition to the code, the repository includes:
- A labeled dataset of BPMN files in `dataset/` for reproducing the evaluation
- Experiment configurations and results in `experiments/`
- A system demo in `system demo/`

The datasets are provided as CSV exports of the database and can be imported to run the application with the same data used in the experiments.

## Docker Setup

This project can be run in different environments by **composing multiple docker-compose files**.

This project provides:

* `docker-compose.yml` – base services (frontend, backend, Postgres)
* `docker-compose.local.yml` – local testing and running
* `docker-compose.prod.yml` – production style, with Traefik labels and watchtower labels, expects an existing **external** Traefik network (default: `web`)
* `docker-compose.traefik.yml` – optional Traefik container, in case the host does **not** already run Traefik
* `docker-compose.watchtower.yml` – optional Watchtower container

Environment specific values (like hosts, Traefik network name, email, ports) are provided via `.env` files.

Attention: If you are using **Windows**, make sure to adjust the following:
- Under Settins > General enable: Expose daemon on tcp://localhost:2375 without TLS
- Under Settings > Docker Engine add: "min-api-version": "1.24"

Attention: If you are using **Linux**:
- Change the docker-compose.traefik to use
  ```bash
  command:
    - --providers.docker=true
    - --providers.docker.endpoint=unix://var/run/docker.sock
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
  ```

---

### 1. Run locally

```bash
cp .env.local.example .env.local

# Fill out .env.local as needed

docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.local.yml up -d
```

You will then have:

* frontend: [http://localhost:3000](http://localhost:3000)
* backend: [http://localhost:8080/api](http://localhost:8080/api)
* Postgres: localhost:5432

### 3. Run in production (server already has Traefik + Watchtower)

```bash
cp .env.prod.example .env.prod
# make sure the external Docker network exists:
# docker network create web
docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d
```

This will:

* attach all services to your existing `web` network,
* expose the frontend on `${GRIPL_HOST}` and `www.${GRIPL_HOST}`,
* expose the backend on `${GRIPL_HOST}/api`,
* and enable Watchtower updates for all of them.

---

### 4. Run Watchtower and Traefik alongside (server does not have them yet)

If your host does **not** run Watchtower and Traefik yet, add:

```bash
docker compose --env-file .env.prod \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.traefik.yml \
  -f docker-compose.watchtower.yml up -d
```

Watchtower is configured to only update containers that have the label
`com.centurylinklabs.watchtower.enable=true`, which we add in `docker-compose.prod.yml`.

---

## Run locally without Docker for development

See [gripl/gripl-backend/README.md](gripl/gripl-backend/README.md) for instructions on running the backend locally.

See [gripl/gripl-frontend/README.md](gripl/gripl-frontend/README.md) for instructions on running the frontend locally.

Local development requires a running PostgreSQL database. The simplest option is to start a fresh instance with Docker and set the backend’s database connection via environment variables. On startup, the backend will automatically create any missing tables.

Alternatively, you can use `docker-compose.local.yaml` to bring up the entire stack (frontend, backend, PostgreSQL) with Docker as explained above and point your locally running frontend and backend to that database as well.

## Tool Components
The GRIPL tool consists of three main components:

1. The Sandbox
Here, users can model or import processes and let the LLM analyze the process model. 
<img width="2232" height="1265" alt="sandbox-analyzed-model-annotated" src="https://github.com/user-attachments/assets/1e7ac03b-2f76-4e10-8f21-ed4fc1d4af16" />

The model also creates a reasoning for the activity classification.
<img width="1138" height="607" alt="sandbox-ai-reasoning" src="https://github.com/user-attachments/assets/7ed9f60b-18d8-4a06-b8b5-09e4e15d682b" />

2. The Labeling Editor
Here, users can create and label their own BPMN 2.0 dataset. A reasoning can also be added to the labels.
<img width="2227" height="1258" alt="labeling-datasets" src="https://github.com/user-attachments/assets/7651e26e-b3a5-48bb-beca-67daee7d6c7e" />

<img width="2232" height="1260" alt="labeling-editor-annotated" src="https://github.com/user-attachments/assets/e7f03bc1-2a74-44c6-adf7-e05f906594a2" />

3. The evaluation plattform
Evaluations can be configured via the GUI or a YAML file.
<img width="1137" height="1183" alt="evaluation-config_new" src="https://github.com/user-attachments/assets/7ccd4c36-a674-4a69-88c5-6ae181fae186" />

The results are represented by model 
<img width="1327" height="1211" alt="evaluation-result-by-model" src="https://github.com/user-attachments/assets/52a3e330-22d4-42ad-9e70-14e3a4baef7a" />

or by run 
<img width="1318" height="1125" alt="evaluation-result-by-run_new" src="https://github.com/user-attachments/assets/c82634f2-71c4-4bf8-9b94-5a059ecada9c" />

And also by test case 
<img width="1503" height="1268" alt="evaluation-result-by-testcase_new" src="https://github.com/user-attachments/assets/6272dfc7-533e-47dc-a57d-c143e1caa864" />



