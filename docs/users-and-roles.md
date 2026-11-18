# Users and roles

Accounts live in **auth-service** (MongoDB, one instance per environment). GRIPL only reads the signed login token (JWT). Run
all commands below on the server in the auth stack folder of the environment you mean:

```bash
cd /srv/gripl/<env>/auth        # <env> = dev or prod
```

## How roles work

- Every application registered in auth-service has its own roles. GRIPL has `admin`, `dpo`, `researcher`, `user`.
- Someone without an explicit role gets the application's default role (`user`). A **superuser** is `admin` in every application.
- The role is written into the token at login. After a role change the person has to **log out and in again**.
- In GRIPL, `admin` and `researcher` see everything, including *Labeling* and *Evaluation*. `dpo` and `user` only see *Process Analysis*
  and *Custom Endpoints*.

Implemented in: role resolution `app/services/role_resolution.py` and login `app/api/routes/auth_routes.py` (auth-service repo);
GRIPL checks the role in `gripl/gripl-backend/.../security/AuthenticatedUser.kt` (`PRIVILEGED_GRIPL_ROLES`, the real
enforcement) and mirrors it in `gripl/gripl-frontend/src/lib/gripl-role.ts` and `middleware.ts` (only hides pages). To change who sees what, change
both sets.

## Create the first superuser

Register in the web app first, then:

```bash
docker compose exec auth-service python scripts/promote_superuser.py <username>
```

This works **only while no superuser exists**; afterwards use the next task.

Implemented in: `scripts/promote_superuser.py` (auth-service repo).

## Make or remove further superusers

You must be a superuser yourself; the script asks for your username and password.

```bash
docker compose exec auth-service python scripts/admin_user.py superuser <user>
docker compose exec auth-service python scripts/admin_user.py superuser <user> --revoke
```

`<user>` is a username or an e-mail address.

## Change a user's role

You must be a superuser or `admin` of that application.

```bash
docker compose exec auth-service python scripts/admin_user.py role <user> gripl researcher
docker compose exec auth-service python scripts/admin_user.py show <user>
```

The second command prints the current roles (no login needed). Valid roles are the ones the application registered
(see the next task, `--list`).

Implemented in: `scripts/admin_user.py` calls `PUT /users/{id}/roles/{app}` (`app/api/routes/user_routes.py`).

## Register an application (or change its roles)

Needed once per auth-service instance, and again when an application gets a new role. Every application must define an `admin` and a `user` role.

```bash
docker compose exec auth-service python scripts/register_application.py gripl --name GRIPL \
  --role admin=Admin --role dpo="Data Protection Officer" --role researcher=Researcher --role user="End User"
docker compose exec auth-service python scripts/register_application.py --list
```

Registering an application that exists needs `--overwrite`. Registration is deliberately not an HTTP endpoint.

Implemented in: `scripts/register_application.py`, model and validation `app/models/application_models.py`.

**Future plan (not decided, not built):** applications register themselves at startup through an interface of auth-service, instead of
this script. Related issues: auth-service#22 (design), GRIPL#48, RAGulate#152.

## Reset a user's OpenRouter key

Every user stores their own key under **Account Settings**; saving a new key overwrites the old one. To remove it for a user
(for example when it leaked), clear it in the database:

```bash
docker compose exec auth-mongo sh -c 'mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin auth_db --eval "db.users.updateOne({username: \"<username>\"}, {\$unset: {openrouter_api_key_encrypted: \"\"}})"'
```

The user then gets "Set your OpenRouter API key..." on the next analysis. Keys are stored encrypted with `SECRETS_ENCRYPTION_KEY`
from the stack's `.env`; if that value is lost or changed, **all** stored keys become unreadable and everybody has to enter theirs again.
Dev and prod use different keys on purpose.

Implemented in: `app/core/secrets.py`, `app/services/user_service.py` (auth-service repo).

## Change the token lifetime

In `/srv/gripl/<env>/auth/.env`:

```
ACCESS_TOKEN_EXPIRE_MINUTES=480     # default 15
```

When a token expires the user logs in again (there are no refresh tokens yet, auth-service#13). Apply the change (only auth-service
restarts; existing tokens keep their old expiry):

```bash
docker compose up -d
```

Implemented in: `app/config.py` (auth-service repo).
