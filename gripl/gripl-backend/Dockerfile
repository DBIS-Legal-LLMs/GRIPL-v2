
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline dependency:resolve-plugins -q

COPY src ./src
RUN mvn package -DskipTests

FROM mcr.microsoft.com/playwright:latest AS runtime
USER root
RUN apt-get update && apt-get install -y openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# CA of the DBIS infrastructure (LLM gateway https://134.60.71.198:4443). update-ca-certificates adds it
# to the system store and, via ca-certificates-java, to Java's cacerts; public CAs stay, so OpenRouter
# keeps working. Do not set -Djavax.net.ssl.trustStore, that would replace the public CAs.
# On CA rotation, update certs/dbis-infrastruktur-ca.crt and the pinned SHA-256 fingerprint below
# together; otherwise the grep fails and the build aborts.
COPY certs/dbis-infrastruktur-ca.crt /usr/local/share/ca-certificates/dbis-infrastruktur-ca.crt
RUN openssl x509 -in /usr/local/share/ca-certificates/dbis-infrastruktur-ca.crt -noout -fingerprint -sha256 \
      | grep -q '1C:3C:21:0B:BC:A0:5D:55:B6:15:AF:86:08:DA:05:AE:05:3D:B8:E6:08:46:7C:9A:CF:2A:78:21:B8:05:DC:2F' \
    && update-ca-certificates

WORKDIR /app

# build version (set by CI), served at /actuator/info; last so it does not invalidate cached layers
ARG APP_VERSION=dev
ENV APP_VERSION=$APP_VERSION
LABEL org.opencontainers.image.version=$APP_VERSION

COPY --from=builder /app/target/*.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
