# GRIPL Frontend

## Getting Started

First, install the dependencies:

```bash
npm install --force
```

The force is needed to resolve any issue with the new React 19 version.

Then copy the `.env.example` file to `.env` and configure the required environment variables. In the case of this frontend you don't need to change anything unless your backend is running on a different URL than `http://localhost:8080`.

Login and registration are proxied to [`auth-service`](https://github.com/DBIS-Legal-LLMs/auth-service)
through the `/auth/*` rewrite (`AUTH_SERVICE_INTERNAL_URL`, default
`http://localhost:8100`). `auth-service` must be running for auth to work — see
the repo root README's *Authentication* section.

Then, run the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Labeling and labels

The existing **Labeltools** area on the BPMN model labeling page contains the
active predefined and custom label list. Researchers and admins can create
labels (both name and description are required). Each custom label can have an
individual color, chosen with a color picker or entered as a hex value. If no
color is set, a default color is used. A duplicate active name is reported as a
conflict. Admins can edit labels and archive custom labels. Predefined label
names stay fixed. Archiving removes a label from future choices but does not
remove its existing test-case assignments.

In the BPMN test-case editor, an activity can have multiple labels. The
predefined GDPR label selection remains synchronized with the existing
`classification` enum so current multiclass analysis/evaluation remains
compatible. Custom labels are shown in their own color in the label lists and
on the BPMN canvas: an activity that has only custom labels (no GDPR class) is
highlighted in the label's color instead of the generic privacy tint. Test-case
reads use `expectedValues[].labels`; saves send `expectedValues[].labelIds` (and
the compatible `classification`) without writing the response-only `labels`
property. Label management and assignment use the backend `/labels` API through
the frontend `/api` proxy; backend role checks remain authoritative.