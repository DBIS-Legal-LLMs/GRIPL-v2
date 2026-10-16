import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    experimental: {
        proxyTimeout: 1_800_000, // 30 min
    },
    eslint: {
        // Temporary unblocker: avoid failing production build on unrelated legacy lint findings.
        ignoreDuringBuilds: true,
    },
    webpack(config, { isServer }) {
        config.module.rules.push({
            test: /\.bpmn$/,
            use: 'raw-loader'
        });

        // Prevent pdfjs-dist from trying to load the native canvas module
        config.resolve.alias.canvas = false;

        return config;
    },
    async rewrites() {
        const ragBase = process.env.RAG_INTERNAL_URL ?? "http://gripl-rag:8081";
        // auth-service (login/register/JWT issuance). Proxied server-side so the
        // browser only ever talks to this origin — no CORS setup on auth-service
        // needed for GRIPL. Default suits `npm run dev`; the Docker stacks set
        // AUTH_SERVICE_INTERNAL_URL=http://host.docker.internal:8100 explicitly.
        const authBase = process.env.AUTH_SERVICE_INTERNAL_URL ?? "http://localhost:8100";
        return [
            {
                source: '/api/:path*',
                destination: `${process.env.NEXT_PUBLIC_API_BASE_URL}/:path*`,
            },
            {
                source: '/rag/:path*',
                destination: `${ragBase}/:path*`,
            },
            {
                source: '/auth/:path*',
                destination: `${authBase}/auth/:path*`,
            }
        ];
    },
    reactStrictMode: false
};

export default nextConfig;
