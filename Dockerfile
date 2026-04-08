FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache dependencies first for faster incremental builds.
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /build/target/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
	JAVA_OPTS="" \
	SERVER_PORT=8082

EXPOSE 8082

USER spring

# Reads credentials from .env when mounted at /app/.env
# (application.properties already includes spring.config.import=optional:file:.env[.properties]).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
