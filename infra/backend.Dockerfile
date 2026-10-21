# Image backend multi-étapes. Construire depuis la racine du dépôt :
#   docker build -f infra/backend.Dockerfile -t eduquest-backend .
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src src
RUN ./mvnw -B -q -DskipTests package && cp target/eduquest-backend-*.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S eduquest && adduser -S -G eduquest -H eduquest
WORKDIR /app
COPY --from=build /app.jar app.jar
# Contenus pédagogiques pour l'outil d'import (voir docs/deployment.md).
COPY content content
USER eduquest
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --eduquest.content.path=/app/content \"$@\"", "--"]
