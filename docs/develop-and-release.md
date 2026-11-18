# Develop and release

## Branches and pull requests

- Work on a feature branch, open a pull request into **`dev`**. CI runs the backend tests, the frontend type check and
  builds all images (nothing is published from a pull request).
- **`main`** only ever receives `dev` (see [deploy-to-prod.md](deploy-to-prod.md)). Do not push or open feature PRs against it.
- Merge feature PRs into `dev` with **Squash and merge**: the PR title becomes the commit message, which is what the
  version is computed from. Merge `dev` into `main` with a **merge commit**, never a squash.

## Commit messages and PR titles

Messages follow [Conventional Commits](https://www.conventionalcommits.org). The PR title is checked by the
`PR title` workflow.

| Prefix | Meaning | Version |
|---|---|---|
| `fix:` / `perf:` | bug fix, performance | patch (1.2.3 -> 1.2.4) |
| `feat:` | new feature | minor (1.2.3 -> 1.3.0) |
| `feat!:` (any type with `!`) or a `BREAKING CHANGE:` line | incompatible change | major (-> 2.0.0) |
| `chore:` `docs:` `refactor:` `test:` `ci:` `build:` | everything else | no release |

An optional scope is fine: `fix(backend): ...`. A commit that does not follow this is ignored for versioning, so a
change that should be released but is named `update stuff` ends up in no release.

Implemented in: `.github/workflows/pr-title.yml`, `.github/scripts/next-version.sh` (the rules above).

## What happens when a PR is merged into `dev`

1. CI computes the next version from the commits since the last tag.
2. If there is a `fix:`/`feat:` commit: the three images (`gripl-frontend`, `gripl-backend`, `gripl-rag`) are built with the
   version inside, published as `:X.Y.Z` (never overwritten later) and `:dev`, and then the git tag `vX.Y.Z` and a GitHub Release
   (marked **pre-release**) are created.
3. Otherwise only `:dev` is rebuilt, and the version stays the same.
4. The timer on the server pulls `:dev` and restarts what changed (a few minutes).

Implemented in: `.github/workflows/ci.yml` (jobs `version`, `images`, `release`).

## Where to see the version

- GitHub: **Releases** (pre-release = only on dev so far, normal release = promoted to prod once).
- In the app: the sidebar footer shows `dev-v1.4.0` / `prod-v1.4.0` (the environment comes from the host name).
- Backend: `GET /actuator/info` on the backend container (port 8080, inside the Docker network).
- Registry: the image tags `ghcr.io/dbis-legal-llms/<image>:X.Y.Z`.

Implemented in: footer `gripl/gripl-frontend/src/components/navigation/app-sidebar.tsx`; the version is passed as the
build argument `APP_VERSION`.

## Change the database schema (add a migration)

The schema is managed by Flyway; the backend applies new migrations when it starts, on dev and on prod.

1. Add a file `gripl/gripl-backend/src/main/resources/db/migration/V<next>__<short_description>.sql`. `<next>` is the highest
   existing number plus one.
2. **Never edit, rename or delete an existing migration.** Flyway stores a checksum of every applied file and the backend
   refuses to start otherwise. Fix mistakes with a new migration.
3. Open the PR. The `Migrations` check fails on an edited/renamed/deleted migration or a number that is not the highest, and
   **warns** about statements that can lose data (`DROP TABLE/COLUMN`, `TRUNCATE`, `DELETE FROM`, column type change, rename).
4. If you get that warning, try the migration on dev against a copy of prod's data before the release goes to prod
   (`deploy/scripts/clone-prod-to-dev.sh`, see [server-operations.md](server-operations.md)).
5. A migration cannot be undone by going back to an older image. The updater takes a database backup before every update, so
   the data can be restored ([server-operations.md](server-operations.md), "Restore a backup").

Implemented in: `.github/scripts/check-migrations.sh`, `.github/workflows/migrations.yml`; Flyway is enabled in
`gripl/gripl-backend/src/main/resources/application.properties`.

## Documentation

Update the pages in `docs/` that your change touches (and the READMEs), in a separate commit of the same pull request.
