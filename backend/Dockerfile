# syntax=docker/dockerfile:1

# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system app && useradd --system --gid app --home /app app

# Media upload dir (mount a volume here in prod).
RUN mkdir -p /data/media && chown -R app:app /data
VOLUME ["/data/media"]

COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
