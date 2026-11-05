# Troubleshooting

Find the symptom, check the cause, apply the fix. Commands run on the server in the stack folder
(`/srv/gripl/<env>/gripl` or `/srv/gripl/<env>/auth`), see [server-operations.md](server-operations.md).

| Symptom | Likely cause | What to do |
|---|---|---|
| A container never becomes healthy; the update run fails | wrong value in `.env`, missing secret, dependency down | `docker compose logs --tail 80 <service>`; fix `.env`; `docker compose up -d` |
| Reading works, **every write fails with 403** and an empty body, nothing in the logs | the browser origin is not in the backend's CORS list (`APP_CORS_ALLOWED_ORIGINS`, built from `PUBLIC_SCHEME` and `GRIPL_HOST` in the stack's `.env`) | check `GRIPL_HOST`/`PUBLIC_SCHEME`, then `docker compose up -d` |
| Analysis fails with `PKIX path building failed` | the backend image does not trust the CA of the LLM gateway | the CA is built into the backend image (`gripl/gripl-backend/certs/`); an old image or a rotated CA is the cause. On rotation update the certificate and the pinned fingerprint in the `Dockerfile` together |
| Message "Set your OpenRouter API key..." on analysis | that user has no key stored | the user saves it under **Account Settings** |
| Yellow banner "knowledge graph is empty" | the RAG corpus was not ingested in this environment | run the ingestion, or copy the graph ([server-operations.md](server-operations.md), "Knowledge base") |
| After a role change the user still sees the old pages | the role is part of the token | log out and in again |
| *Labeling* / *Evaluation* are missing for a user | role is not `admin` or `researcher` | [users-and-roles.md](users-and-roles.md), "Change a user's role" |
| Analysis or token errors with a 403 saying the token carries no GRIPL role | GRIPL is not registered in this auth-service instance | register the application ([users-and-roles.md](users-and-roles.md)); a new environment needs this once |
| Backend does not start, Flyway reports a validation or checksum error | an applied migration was edited, or a migration number is not the highest | never edit applied migrations; add a new one ([develop-and-release.md](develop-and-release.md)); if it happened on a database, restore the backup |
| The updater log says `BACKUP FAILED` | the dump failed (database down, disk full) | nothing was updated; fix the cause (`df -h /srv/gripl`, `docker compose ps`), the next run tries again |
| Image pull error `failed commit on ref layer-sha256...` | images pulled in parallel (old updater) | current `update-stack.sh` pulls one at a time; make sure `/srv/gripl/bin` is up to date ([server-operations.md](server-operations.md)) |
| Prod runs the wrong version | `IMAGE_TAG` is pinned in the stack's `.env`, or the promotion did not run | check `IMAGE_TAG`, and `gh run list --workflow Promote` ([deploy-to-prod.md](deploy-to-prod.md)) |
| Login answers `429` for a minute | rate limit after several wrong logins, on purpose | wait |
| `/rag/api/...` answers `403` | blocked on purpose, only the PDF viewer path is open | nothing to fix |
| A secret in `.env` was changed after the database already existed | the database keeps the password it was created with | put the old value back, or start with an empty database (`docker compose down -v` deletes that stack's data, restore a backup afterwards) |
| The server is unreachable | VPN off, or the VM is down | connect the VPN; otherwise ask the server admin |
| On a laptop, local containers are unreachable while the university VPN is connected | some VPN clients claim Docker's default subnets (`172.17.0.0/16`, `172.20.0.0/16`) | give Docker other ranges (`default-address-pools` in `/etc/docker/daemon.json`) |

If nothing here fits: `docker compose logs --tail 200 <service>` of the failing service, and the updater log
(`journalctl -u 'dbis-update@*' -n 50`).
