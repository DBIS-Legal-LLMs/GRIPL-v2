# GRIPL - NLP-to-SQL Extension

## Overview

This repository contains an extension of the GRIPL framework
(GDPR Risk Identification in Processes using Large Language Models).

The extension provides an LLM-based NLP-to-SQL service for identifying
GDPR-critical activities in business processes.

Instead of relying exclusively on a Retrieval-Augmented Generation (RAG)
approach, the service uses a Large Language Model (LLM) to identify
GDPR-related intentions and retrieve relevant information from a SQL
database.

The SQL database contains GDPR-related categories, reasons, criteria,
and legal information.

The service can be used to classify process activities according to
their GDPR relevance and provide reasons for the classification.

This repository is a subproject of GRIPL. For information about the
overall GRIPL framework, its architecture, and the other services,
please refer to the README of the main GRIPL project.

---

## Project Structure

```text
gripl-sql-llm/
│
├── app/
│   └── evaluator_component/
│       ├── evaluator.py
│       └── ...
│
├── scripts/
│   ├── insert_data_in_db.py
│   ├── fill_example_in_vector_database.py
│   
│
├── requirements.txt
├── .env
└── ...
```

---

# Local Setup

## Prerequisites

The service is intended to be executed locally using:

- Python 3.11
- pip
- Required LLM provider API keys
- Dependencies specified in `requirements.txt`


---

## 1. Navigate to the Project Directory

Navigate to the `gripl-sql-llm` directory:

```bash
cd gripl-sql-llm
```

---

## 2. Create a Virtual Environment

Create a Python 3.11 virtual environment:

```bash
python3.11 -m venv venv
```

Activate the virtual environment.

### macOS / Linux

```bash
source venv/bin/activate
```

### Windows

```powershell
venv\Scripts\activate
```

---

## 3. Install Dependencies

Install the required dependencies:

```bash
python -m pip install -r requirements.txt
```

---

# Configuration before local or online in GRIPL

Before running the evaluation, the LLM models and API keys
must be configured.

## Configure LLM Models

Open the following file:

```text
app/evaluator_component/evaluator.py
```

In this file, configure the models used by the individual components.

For each component, configure:

- Model name
- OpenRouter API URL
- Environment variable name of the API key, the env file in `gripl-sql-llm` directory:

The environment variable name must match the name defined
in the `.env` file in `gripl-sql-llm` directory

Example:

```python
model = "your-model-name"

model_url = "https://openrouter.ai/api/v1"

env_api_key_name = "OPEN_ROUTER_API_KEY"
```

The actual configuration depends on the individual component.

Make sure that the model name, OpenRouter URL, and API key
environment variable are correctly configured before running
the evaluation.

---

## Configure Environment Variables

Create or update the `.env` file in `gripl-sql-llm` directory.

Example:

```dotenv
OPEN_ROUTER_API_KEY=your-api-key
```

Do not commit API keys or other secrets to the repository.

---

# Preparing the Evaluation

In the test.db must be the choosen models inserted.
The columns component_name in llm_fallback -> see in config-component_name.py
Before running the local evaluation, the required data must be
inserted into the SQLite database test.db and example activities must be
loaded into the vector database.

The following scripts are provided for this purpose.

---

## 0. Make sure in gripl-sql-llm directory is a test.db file

## 1. Insert Data into the SQLite Database from `gripl-sql-llm` directory:

Run the following script:

```text
python -m scripts.insert_data_in_db
```

From the `gripl-sql-llm` directory:

```bash
python -m scripts.insert_data_in_db
```

## 2. start the mcp server

1. From the `gripl-sql-llm` directory:

2. run 

```bash
python -m app.sql_post_processing_component.post_processing_mcp_server
```


### Purpose

This script inserts the required GDPR-related data into the
SQLite database.

The database contains information used by the LLM to identify
GDPR-related categories and reasons.

The data is used during the SQL-based retrieval and classification
of process activities.

Run this script before starting the evaluation.

---

## 2. Insert Examples into the Vector Database

From the `gripl-sql-llm` directory:

Run the following script:

```text
python -m scripts.fill_example_in_vector_database
```



### Purpose

This script inserts example activities into the vector database.

The examples can be used to retrieve similar activities during
the evaluation process.

The example data can be adapted if necessary.

### Customizing the Examples

The examples can be modified in:

```text
scripts/fill_example_in_vector_database.py
```

The examples may be adapted according to the requirements
of the experiment.

After modifying the examples, run the script again to update
the vector database.

---

# Running in GRIPL 


use as custom url the following: http://gripl-nlpsql:8082/analyze



# Running the Local Evaluation

After completing the setup and preparing the databases,
the evaluation can be started locally.

## 1. Navigate to the Project Directory

```bash
cd gripl-sql-llm
```

## 2. Run the Evaluation

Execute:

```bash
python -m app.evaluator_component.evaluate_main
```

The evaluation script runs the LLM-based classification process.

The LLM attempts to identify the GDPR-related category of each
activity and determine whether the activity is GDPR-critical.

The evaluation results can be used to compare the predicted
classifications with the expected labels in the dataset.

---

## Complete Local Evaluation Workflow

```bash
# Navigate to the project directory
cd gripl-sql-llm

# Create a Python 3.11 virtual environment
python3.11 -m venv venv

# Activate the virtual environment
source venv/bin/activate

# Install dependencies
python -m pip install -r requirements.txt

# Configure models and API keys
# Edit:
# app/evaluator_component/evaluator.py

# Insert GDPR-related data into the SQLite database
python -m scripts.insert_data_in_db

# Insert examples into the vector database
python -m scripts.fill_example_in_vector_database

# Run the evaluation
python -m app.evaluator_component.evaluate_main
```

---

# Running with Docker

The NLP-to-SQL service can also be started as part of
the GRIPL application using Docker Compose of the original project.

The Docker Compose configuration is located in the root dictionary:

```text
docker-compose.local.yml
```

## Prerequisites

Make sure Docker and Docker Compose are installed.

---

## Start the NLP-to-SQL Service and MCP Server

To start only the NLP-to-SQL service and the MCP server:

```bash
docker compose -f docker-compose.local.yml up -d --build gripl-nlpsql mcp-server
```

---



