# syntax=docker/dockerfile:1

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/quarkus-app /app

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh","-c","exec java ${JAVA_OPTS} -Dquarkus.http.port=${PORT:-8080} -jar /app/quarkus-run.jar"]
