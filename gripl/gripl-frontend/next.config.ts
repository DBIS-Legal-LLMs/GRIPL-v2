import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    experimental: {
        proxyTimeout: 1_800_000, // 30 min
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
        return [
            {
                source: '/api/:path*',
                destination: `${process.env.NEXT_PUBLIC_API_BASE_URL}/:path*`,
            },
            {
                source: '/rag/:path*',
                destination: `${ragBase}/:path*`,
            }
        ];
    },
    reactStrictMode: false
};

export default nextConfig;
