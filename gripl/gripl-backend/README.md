# GRIPL Backend

The **GRIPL Backend** is a Spring Boot application that provides a REST API for analyzing BPMN files and evaluating labeled datasets of processes. It also includes full CRUD endpoints for managing the test dataset. While it is designed to work seamlessly with the GRIPL frontend, it can also be used independently via a command-line interface (CLI).

## Prerequisites 
Please ensure that you have the following installed and configured on your system before running the backend:
- Postgres Database (Version 15 or higher recommended)
- Java JDK 21
- Maven

If needed change the database connection settings in `src/main/resources/application.properties` to match your local Postgres configuration.

### Authentication

The backend does not issue tokens. It verifies incoming RS256 JWTs against
[`auth-service`](https://github.com/DBIS-Legal-LLMs/auth-service)'s JWKS
(`JwtAuthenticationWebFilter` → `JwksProvider`). Point it at a running
`auth-service` with:

```
app.jwt.jwks-uri = ${AUTH_SERVICE_JWKS_URI:http://localhost:8100/.well-known/jwks.json}
```

The default suits `auth-service` running locally on `:8100`. In Docker, set
`AUTH_SERVICE_JWKS_URI=http://host.docker.internal:8100/.well-known/jwks.json`.
See the repo root README's *Authentication* section for the full picture.

### Dataset ownership

Datasets are private to the `auth-service` user (JWT `sub`) that created them
(GRIPL-v2#33). `POST /dataset` stamps the caller as `owner_user_id`; `GET
/dataset` and `DELETE /dataset/{id}` only see the caller's own rows. Test cases
(`/dataset/testcase/**`) inherit this through their parent `dataset_id` — a test
case must live in a dataset you own, and accessing anyone else's returns `404`.

Rows created before this feature (migration `V5`) have `owner_user_id = NULL`
and are owned by nobody, so they disappear from these endpoints until an owner
is assigned by hand:

```sql
UPDATE dataset SET owner_user_id = '<your auth-service user id>' WHERE owner_user_id IS NULL;
```

The evaluation-run code paths still read all datasets regardless of owner;
role-based gating for those is GRIPL-v2#40.

## Running Locally with Maven

You can run the backend locally using **Maven**. Make sure Maven is installed on your system.

There are several ways to run the backend locally:

1. **Run as the backend for the GRIPL frontend:**

   ```bash
   mvn spring-boot:run
   ```

2. **Run CLI commands without starting the frontend:**
   The application will shut down automatically after executing the command.

   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="analysis ./path-to-bpmn-file.bpmn --output json"
   mvn spring-boot:run -Dspring-boot.run.arguments="evaluation"
   ```

3. **Run with interactive shell for executing commands while running:**

   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments="--app.shell.enabled=true"
   ```
   
### Optional – Customising the LLM endpoint

The backend can talk to any OpenAI‑compatible LLM.

If you want to point the analysis command at a private or self‑hosted model, simply add two extra CLI flags:

| Flag	           | Meaning                                                      | Example value                  |
|-----------------|--------------------------------------------------------------|--------------------------------|
| `--llm.base-url` | The base URL of the LLM API (must include /v1 in most cases) | `http://127.0.0.1:1234/v1`     |
| `--llm.model-name` | The name/id of the model to use | `deepseek-r1-distill-llama-8b` |
| `--llm.api-key` | The API key for the LLM (if required) | `sk-...`                       |
| `--llm.timeout-seconds` | The timeout for LLM requests in seconds (default: 30) | `240` (4 Minutes)              |

Usage example

```bash
# Run a single analysis and write JSON to stdout,
# while pointing the LLM at a private endpoint.
mvn spring-boot:run \
-Dspring-boot.run.arguments="analysis ./test-diagram.bpmn -o json \
--llm.base-url=http://127.0.0.1:1234/v1 \
--llm.model-name=deepseek-r1-distill-llama-8b"
```

The two flags are optional – if omitted, the backend falls back to the default openai model.

## Running with Docker

You can also run the backend in a containerized environment using Docker. Ensure Docker is installed on your system.

1. **Build the Docker image:**

   ```bash
   docker build -t gripl-backend .
   ```

2. **Run the Docker container** (ensure the `.env` path is correct):

   ```bash
   docker run -d --name gripl-backend -p 8080:8080 --env-file .env gripl-backend
   ```

## Available CLI Commands

The backend provides several CLI commands for BPMN analysis and dataset evaluation:

* `analysis <bpmn-file>` — Analyzes the specified BPMN file.

   * Optional: `--outputFormat` (Available: `json`, `pretty` *(default)*)
* `evaluation` — Evaluates BPMN processes stored in the database.

   * Optional: `--outputFormat` (Available: `json`, `pretty` *(default)*)
* `-h` — Displays help for commands and options.

> **Tip:** Use `-h` with any command (e.g., `analysis -h`) for detailed command-specific help.
