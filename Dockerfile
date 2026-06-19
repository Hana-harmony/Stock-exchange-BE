FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app \
    && mkdir -p /app/data \
    && chown -R app:app /app/data

COPY --from=builder /workspace/build/libs/*.jar app.jar

USER app

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
