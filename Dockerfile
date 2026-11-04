# syntax=docker/dockerfile:1

# Imagens fixadas por digest de indice multi-arch (reproduzivel). Atualizacao: rode
#   docker buildx imagetools inspect <imagem>:<tag>
# e substitua o digest. Tag mantida no comentario apenas como referencia legivel.
# maven:3.9.16-eclipse-temurin-21
FROM maven@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

# Testes sao executados no gate de CI (mvn verify); o build da imagem apenas empacota.
RUN mvn -ntp -DskipTests package

# eclipse-temurin:21-jre
FROM eclipse-temurin@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# Correcoes de seguranca da base publicadas depois da imagem entram em cada build.
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY --from=build /workspace/target/quarkus-app /app

# Usuario nao-root (defesa em profundidade).
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --home-dir /app app
USER 1001

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS=""

# Liveness: porta aceitando conexao. A readiness (HTTP /q/health/ready) e verificada
# pelo healthcheck do Compose e deve ser usada por orquestradores.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080}' || exit 1

ENTRYPOINT ["sh","-c","exec java ${JAVA_OPTS} -Dquarkus.http.port=${PORT:-8080} -jar /app/quarkus-run.jar"]
