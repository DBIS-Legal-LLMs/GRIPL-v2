# Deploy to prod

Prod never builds anything. It runs an image that was already built and tested on dev: **promoting** a version only points the
tag `:prod` at the existing `:X.Y.Z` images. So what you tested on dev is byte for byte what runs on prod.

## Put a version on prod

Only after the version has been tested on dev and approved.

1. Check on GitHub (**Releases**) that the newest release is the version you approved, and that nobody merged anything
   into `dev` after it that you do not want in prod.
2. Open a pull request **`dev` -> `main`**, title like `chore: promote v1.4.0 to prod`.
3. Merge it with **Create a merge commit** (not squash).
4. The `Promote` workflow starts on `main`. It
   - finds the newest release tag in `main`'s history (the version),
   - stops with an error if there is a `fix:`/`feat:` commit newer than that tag without a release (the release build on dev is still running;
     wait for it and re-run the workflow),
   - checks that all three images `:X.Y.Z` exist,
   - points `:prod` at them (no rebuild),
   - marks that GitHub Release as the normal, **Latest** release, and clears the pre-release flag of every older version,
   - warns if there are commits after the tag that are **not** in the images (`chore:`, `docs:`, non-conventional ones).
5. The prod timer on the server pulls `:prod` within a few minutes and restarts what changed, after taking a backup
   ([server-operations.md](server-operations.md)).

Implemented in: `.github/workflows/promote.yml`.

## Check that prod got the version

```bash
gh run list --workflow Promote --limit 2
```

The newest run must be successful; its summary says "Prod now runs vX.Y.Z". To compare the images yourself (replace `X.Y.Z`),
the digest under `Manifests:` of `:prod` must equal the `Digest:` of `:X.Y.Z`:

```bash
for i in gripl-frontend gripl-backend gripl-rag; do docker buildx imagetools inspect ghcr.io/dbis-legal-llms/$i:prod | grep -E '@sha256'; docker buildx imagetools inspect ghcr.io/dbis-legal-llms/$i:X.Y.Z | grep -E '^Digest'; done
```

(`:prod` is a one-entry index that wraps the image, so its own `Digest:` differs; ignore it.) In the app, the sidebar footer
on prod shows `prod-vX.Y.Z`.

## Roll prod back

Two ways, use the first one if the schema did not change.

**A. Promote an older version (no server access needed).** GitHub -> Actions -> **Promote** -> *Run workflow*, branch `main`,
enter the version, e.g. `1.3.0`. It points `:prod` at that version again, and the timer pulls it. The commit warning in
the run is expected here.

**B. Pin a version on the server.** In `/srv/gripl/prod/gripl/.env` set `IMAGE_TAG=1.3.0` (the file ships with
`IMAGE_TAG=prod`), then:

```bash
cd /srv/gripl/prod/gripl && docker compose up -d
```

Set it back to `prod` to follow promotions again. The same works for `/srv/gripl/prod/auth`.

**If the bad version applied a database migration**, the old image will not fix it (migrations are not undone, and Flyway in the old
backend may refuse a database that is ahead of it). Restore the backup the updater took just before that update
([server-operations.md](server-operations.md), "Restore a backup"), then roll back.
