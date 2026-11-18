# Move the dev users and data to prod

One-time cutover for going live: the people who work on dev log in on prod with the **same accounts** and find their
**datasets, test cases and process models**. Prod starts empty, so this is a copy from dev into prod. Dev is only read.

| Copied | Not copied |
|---|---|
| accounts, password hashes, roles, application registry (auth-service) | the token signing key: prod has its own, everybody logs in once more |
| GRIPL's Postgres: datasets, test cases, process models, per-user settings | users' stored OpenRouter keys (prod has its own `SECRETS_ENCRYPTION_KEY`): everybody enters theirs again under **Account Settings** |
| knowledge graph (`--with-rag`), so it is not ingested twice | |

User ids stay the same, which is what keeps datasets attached to their owners (they are owned by the auth user id).
Registering the users again on prod would give them new ids and orphan their data.

## Before you start

- [ ] The version dev runs is **promoted to prod** ([deploy-to-prod.md](deploy-to-prod.md)). The prod backend has to be at least as
  new as dev's database schema, otherwise it refuses the copied database.
- [ ] Both prod stacks (`/srv/gripl/prod/auth`, `/srv/gripl/prod/gripl`) have their `.env`/`rag.env` with **their own** secrets, and for `--with-rag` the
  same `NEO4J_PASSWORD` as dev.
- [ ] Pull the prod images beforehand, one stack after the other (the RAG image is several GB):
  `cd /srv/gripl/prod/auth && docker compose pull` and `cd /srv/gripl/prod/gripl && docker compose pull`.
- [ ] The prod updater timers are still **off** until the move is done ([server-operations.md](server-operations.md)).
- [ ] The users know the time; **nothing written on dev after the start is carried over.**

## Run it

The scripts live in the dev stacks' checkouts, which follow `dev`; they need no merge into `main` first. Both ask you to type `prod`
(`--yes` skips that).

```bash
# 1. accounts and roles
/srv/gripl/dev/auth/repo/deploy/scripts/clone-dev-to-prod.sh
# 2. GRIPL data and the knowledge graph (only after step 1)
/srv/gripl/dev/gripl/repo/deploy/scripts/clone-dev-to-prod.sh --with-rag
```

What they do: stop the apps of dev and prod, copy, start prod, start dev again, and print counts to compare (users; datasets, test cases,
process models). The GRIPL script also checks that **every owner of copied data is an account in prod** and starts nothing if one is not.

Options: `--force` replaces a prod that already holds data (without it the scripts refuse), `--keep-dev-stopped` leaves dev
stopped afterwards, `--with-rag` copies the knowledge graph.

Implemented in: `deploy/scripts/clone-dev-to-prod.sh` in this repository and in `auth-service`.

## Check it

1. Log in on prod with an old dev account; the sidebar shows what the role allows.
2. The datasets of that user are there; open a test case; run one analysis (with *Use RAG* on).
3. Enter the OpenRouter key under **Account Settings**.

Then switch the prod updater on ([server-operations.md](server-operations.md)) and tell everybody the new address.

## Rehearse first

Prod is empty before the move, so you can run both scripts once, check, and start over. This **deletes prod's data**, only do it
before anybody uses prod:

```bash
cd /srv/gripl/prod/gripl && docker compose down -v && cd /srv/gripl/prod/auth && docker compose down -v
```

## When something goes wrong

| Message | Meaning |
|---|---|
| `prod already holds data` / `prod already has N users` | the first fill is already done; `--force` only to replace prod with dev's state again |
| `owners of copied data are not users in prod` | someone registered on dev after the accounts were copied; run step 1 again with `--force`, then step 2 with `--force` |
| `prod's auth-service has no users` | step 1 was skipped |
| prod does not become healthy after the copy | prod's image is older than dev's schema (Flyway): promote the version dev runs, then `docker compose up -d` in `/srv/gripl/prod/gripl` |
| `NEO4J_PASSWORD` message | with `--with-rag` both `.env` files need the same value |

Afterwards dev keeps working as the test environment. To refresh it with prod's data later, see "Give dev a copy of prod's
data" in [server-operations.md](server-operations.md).
