# Stage 1: Build
FROM node:20-alpine AS builder
# Check https://github.com/nodejs/docker-node/tree/b4117f9333da4138b03a546ec926ef50a31506c3#nodealpine to understand why libc6-compat might be needed.
RUN apk add --no-cache libc6-compat
WORKDIR /app

ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL

# next.config.ts's rewrites() destinations (RAG_INTERNAL_URL, AUTH_SERVICE_INTERNAL_URL)
# are resolved by `next build`, not at container runtime — `next start` serves
# the frozen routes-manifest.json from this build. A docker-compose
# `environment:` entry alone does nothing for these; they must be build args.
ARG RAG_INTERNAL_URL
ENV RAG_INTERNAL_URL=$RAG_INTERNAL_URL
ARG AUTH_SERVICE_INTERNAL_URL
ENV AUTH_SERVICE_INTERNAL_URL=$AUTH_SERVICE_INTERNAL_URL

COPY package*.json ./
RUN npm i --force

COPY . .
RUN npm run build

# Stage 2: Run
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production

# 1. Copy dependencies
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

# 2. Copy the build output
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public

# 3. Copy the config using a wildcard to catch .ts, .js, or .mjs
COPY --from=builder /app/next.config* ./

EXPOSE 3000
CMD ["npm", "start"]