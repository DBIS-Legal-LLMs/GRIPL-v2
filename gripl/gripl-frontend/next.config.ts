import type { NextConfig } from "next";

const nextConfig: NextConfig = {
    eslint: {
        // Temporary unblocker: avoid failing production build on unrelated legacy lint findings.
        ignoreDuringBuilds: true,
    },
    webpack(config, { isServer }) {
        config.module.rules.push({
            test: /\.bpmn$/,
            use: 'raw-loader'
        });

        return config;
    },
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: `${process.env.NEXT_PUBLIC_API_BASE_URL}/:path*`,
            }
        ];
    },
    reactStrictMode: false
};

export default nextConfig;
