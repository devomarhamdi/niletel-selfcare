# syntax=docker/dockerfile:1

# ---- build: compile the executable jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- run: Java runtime only ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home spring
COPY --from=build /build/target/selfcare-api-*.jar /app/app.jar
USER spring
EXPOSE 8081
# Settings (database URL, user, password) come from environment variables at run time.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8081; printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3; grep -q "\"status\":\"UP\"" <&3' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
