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