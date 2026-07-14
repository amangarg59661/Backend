# syntax=docker/dockerfile:1.7
# ============================================================================
# self-backend production image
#
# Two stages:
#   1. builder   — maven + JDK 17, runs `mvn package -DskipTests` and extracts
#                  the fat jar into Spring Boot's layered format so the runtime
#                  image caches classes / snapshot-libs / dependencies as
#                  separate layers.  Rebuilds after a code change re-pull only
#                  the top layer.
#   2. runtime   — eclipse-temurin 17-jre-alpine, non-root user, exposes 8080,
#                  runs the layered jar in exec form so signals reach the JVM.
#
# The image is registry-agnostic: same tag works on Render (docker deploy),
# GHCR (`docker pull ghcr.io/<owner>/self-backend:<sha>`), or a VPS behind
# systemd.  No platform-specific hooks live in this file.
# ============================================================================

# ---------- Stage 1: build --------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Prime the dependency cache in a separate layer.  If pom.xml is unchanged
# subsequent builds skip the download step entirely.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

# Now copy sources and build the jar.  Tests skipped in the image build —
# they run in CI.  This also skips OWASP + SpotBugs; the release workflow
# already gates on both before the image is pushed.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests -Dspotbugs.skip=true -Ddependency-check.skip=true

# Explode the fat jar into Spring Boot's layered format.
RUN mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/self-backend-*.jar extract --destination target/extracted


# ---------- Stage 2: runtime ------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Non-root user + writable temp dir for Spring's file uploads / Tomcat work dir.
RUN addgroup -S edss && adduser -S -G edss edss && \
    mkdir -p /app /tmp/edss && chown -R edss:edss /app /tmp/edss

WORKDIR /app

# Copy layers in cache-friendliest order — least-changing first.
COPY --from=builder --chown=edss:edss /workspace/target/extracted/dependencies/         ./
COPY --from=builder --chown=edss:edss /workspace/target/extracted/spring-boot-loader/   ./
COPY --from=builder --chown=edss:edss /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=edss:edss /workspace/target/extracted/application/          ./

USER edss

# JVM tuning.  MaxRAMPercentage=60 leaves ~200 MB for the OS + zombie
# reaper on Render's 512 MB free plan (60% of 512 = ~307 MB heap).  Bump
# it (JAVA_TOOL_OPTIONS at runtime, e.g. `-XX:MaxRAMPercentage=75`) as
# soon as the service moves to the 2 GB starter plan or higher.
# ExitOnOutOfMemoryError so Render sees the container die and restarts
# instead of running degraded.  SerialGC keeps the resident-set small
# for the free plan — G1 is faster on paid tiers, override with
# JAVA_TOOL_OPTIONS `-XX:+UseG1GC` when the plan is upgraded.
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=60 \
               -XX:+UseSerialGC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Djava.io.tmpdir=/tmp/edss"

# Spring Boot honours SERVER_PORT — 8080 is the .env default.
ENV SERVER_PORT=8080
EXPOSE 8080

# Actuator readiness endpoint is the platform's ready signal.  Docker HEALTHCHECK
# is a fallback for orchestrators that don't hit /actuator directly.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health/readiness || exit 1

# exec form so SIGTERM reaches the JVM cleanly for Spring's graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
