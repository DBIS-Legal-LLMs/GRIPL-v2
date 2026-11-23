# Rebuild the server

What to do if the virtual machine is lost or has to be replaced. The installation itself is not repeated here: it is the **One-time setup**
in [`deploy/README.md`](../deploy/README.md), the same steps that created the server.

## What is lost with the machine

| Data | Where | If it is not restored |
|---|---|---|
| GRIPL data (datasets, test cases, process models, settings) | Postgres volume | users' work is gone |
| Accounts, roles, stored OpenRouter keys | MongoDB volume | everybody has to register again |
| Knowledge graph | Neo4j and RAG volumes | ingest again, which costs OpenRouter credit |
| `.env` and `rag.env` of every stack | `/srv/gripl/<env>/<stack>/` | without `SECRETS_ENCRYPTION_KEY` the stored OpenRouter keys are unreadable; the database passwords must match the restored volumes |
| `/srv/gripl/traefik/certs/` | next to the proxy | new CA: everybody has to install the new `ca.crt` |

The automatic backups under `/srv/gripl/backups/` are on the same machine and are lost with it. Whatever should survive has to be copied to
another place beforehand ([server-operations.md](server-operations.md), *Backups*; secrets: [handover.md](handover.md)). Keep copies of the
dumps, of every `.env`/`rag.env` and of `traefik/certs/`.

## Rebuild

1. Ask the university's server administrators for a new Ubuntu VM with the same network access. If the IP address changes, all host names
   change with it (`<ip>.nip.io`): set the new `GRIPL_HOST` and base domain in the `.env` files. The old CA only accepts the old names (name
   constraints), so delete `traefik/certs/` and run the certificate script again; everybody has to install the new `ca.crt`.
2. Do the **One-time setup** in [`deploy/README.md`](../deploy/README.md) up to and including the `init-stack.sh` calls and the certificate. If you
   have the old `traefik/certs/` folder and the names are the same, put it back instead of running the certificate script, so installed CAs stay valid.
3. Put the saved `.env` and `rag.env` files into the stack folders instead of filling in the examples.
4. Restore the data **before** the applications start. Start only the databases, restore into them with the commands of
   [server-operations.md](server-operations.md#restore-a-backup), then start everything:

   ```bash
   cd /srv/gripl/<env>/auth  && docker compose up -d --wait auth-mongo
   cd /srv/gripl/<env>/gripl && docker compose up -d --wait gripl-postgres
   # restore here, then in each of the two folders:
   docker compose up -d --wait
   ```

   Start `traefik` first, as in the setup. The first-start chores of the setup (registering GRIPL in auth-service, creating a superuser,
   ingesting the knowledge base) are only needed for what was **not** restored.
5. Re-enable the update timers from the setup, and the 03:00 run of prod ([server-operations.md](server-operations.md#update-once-a-night-prod)).
6. Check: log in on prod and dev, open an old dataset, and run `docker compose ps` in every stack.

## Start over on the same machine

This deletes every database of this server, so take the backups first:

```bash
for s in prod/gripl dev/gripl prod/auth dev/auth traefik; do (cd /srv/gripl/$s && docker compose down -v); done
```

`-v` removes the volumes; without it the data survives. Then continue with step 2 (the stack folders and their `.env` files stay).
