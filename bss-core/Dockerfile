# syntax=docker/dockerfile:1

# ---- build: compile the WAR ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- run: WildFly (Jakarta EE 10), full profile ----
FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk17
ADD --chown=jboss:root --chmod=644 --checksum=sha256:188976721ead8e8627eb6d8389d500dccc0c9bebd885268a3047180274a6031e \
    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar /tmp/postgresql.jar
COPY --chown=jboss:root docker/configure-wildfly.cli /tmp/configure-wildfly.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configure-wildfly.cli \
 && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history /tmp/postgresql.jar /tmp/configure-wildfly.cli
COPY --from=build --chown=jboss:root /build/target/bss-core.war /opt/jboss/wildfly/standalone/deployments/bss-core.war

EXPOSE 8080
# Healthy only when bss-core answers UP, which means its database answers SELECT 1.
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET /bss-core/health HTTP/1.0\r\n\r\n" >&3; grep -q "^UP" <&3' || exit 1
# The full profile holds the data source and the queue. Listen on all interfaces; the management port stays private.
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0", "-bmanagement", "127.0.0.1"]
