FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG UID=1010
ARG GID=1010

RUN groupadd -g "${GID}" fleetapp \
    && useradd --create-home --no-log-init -u "${UID}" -g fleetapp fleetapp

WORKDIR /opt/app

COPY --chown=fleetapp:fleetapp pom.xml .
RUN mvn dependency:go-offline -q

COPY --chown=fleetapp:fleetapp src ./src

RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre AS runtime

ARG BUILD_DATE=unknown
ARG BUILD_VERSION=unknown
ARG IMAGE_DESCRIPTION=unknown
ARG IMAGE_NAME="FleetControl - MS Alerts"
ARG UID=1010
ARG GID=1010

LABEL fleetapp.date=$BUILD_DATE \
      fleetapp.name=$IMAGE_NAME \
      fleetapp.description=$IMAGE_DESCRIPTION \
      fleetapp.base.image="eclipse-temurin:17-jre" \
      fleetapp.version=$BUILD_VERSION \
      maintainer="fleetapp"

RUN groupadd -g "${GID}" fleetapp \
    && useradd --create-home --no-log-init -u "${UID}" -g fleetapp fleetapp

WORKDIR /opt/app

COPY --from=builder --chown=fleetapp:fleetapp /opt/app/target/ms-alerts.jar app.jar

USER fleetapp

EXPOSE 8088
ENTRYPOINT ["java","-jar","app.jar"]
