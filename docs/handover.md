# Take over the system

What the person who runs GRIPL, auth-service and the server needs to own. Everything here is a checklist of **access and
secrets**; how to do things is in the other pages ([README.md](README.md)).

## Access

| What | Why you need it | Check |
|---|---|---|
| Account on the GitHub organisation `DBIS-Legal-LLMs`, **admin** on `GRIPL-v2`, `auth-service`, `RAGulate_v2` | merge pull requests, run workflows, create releases, change branch rules | open *Settings* of each repository |
| Write access to the container registry packages (`gripl-frontend`, `gripl-backend`, `gripl-rag`, `auth-service`) | CI publishes the images, the server pulls them | GitHub -> organisation -> *Packages* |
| SSH account on the server, in the groups `docker` and `project-gripl`, with `sudo` | everything in [server-operations.md](server-operations.md) | `ssh <user>@<server>`, then `id` and `sudo -l` |
| VPN or university network | the server and both environments are only reachable from there | open the prod address |
| A superuser account in auth-service | change roles ([users-and-roles.md](users-and-roles.md)) | log in, then `scripts/admin_user.py show <user>` |

Remove accounts of people who leave: server user, GitHub organisation, superuser flag.

## Secrets

Each stack keeps its secrets in files next to its compose file. They are never committed to Git.

| File | Holds |
|---|---|
| `/srv/gripl/<env>/auth/.env` | `SECRETS_ENCRYPTION_KEY`, MongoDB root password |
| `/srv/gripl/<env>/gripl/.env` | Postgres, Neo4j and LLM gateway credentials |
| `/srv/gripl/<env>/gripl/rag.env` | settings of the knowledge graph service |
| `/srv/gripl/traefik/` | the proxy's own configuration |

`<env>` is `dev` and `prod`, and every environment has **its own** values. Two of them need care:

- `SECRETS_ENCRYPTION_KEY` encrypts the stored OpenRouter keys. If it is lost or replaced, every stored key becomes unreadable and each user has to enter theirs again.
- The database passwords must stay the same as the ones the volumes were created with ([troubleshooting.md](troubleshooting.md)).

Keep a copy of these files in a place the successor can reach and nobody else can, and update it whenever a value changes.

## Documents that are not in the repository

The setup of the server itself (how it was installed, firewall, users, Traefik) and the original rollout plan were written outside
the repository, since they contain addresses and credentials. They have to be handed over together with the secrets.

## Check that the handover worked

The successor should be able to do each of these alone, with the page named:

1. Release a change and put it on prod ([develop-and-release.md](develop-and-release.md), [deploy-to-prod.md](deploy-to-prod.md)).
2. Roll prod back ([deploy-to-prod.md](deploy-to-prod.md)).
3. Restore a backup ([server-operations.md](server-operations.md)); practise it on dev.
4. Change a user's role ([users-and-roles.md](users-and-roles.md)).
