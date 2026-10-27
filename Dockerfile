# syntax=docker/dockerfile:1

# Imagens fixadas por digest de indice multi-arch (reproduzivel). Atualizacao: rode
#   docker buildx imagetools inspect <imagem>:<tag>
# e substitua o digest. Tag mantida no comentario apenas como referencia legivel.
# maven:3.9.6-eclipse-temurin-21
FROM maven@sha256:8d63d4c1902cb12d9e79a70671b18ebe26358cb592561af33ca1808f00d935cb AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

# Testes sao executados no gate de CI (mvn verify); o build da imagem apenas empacota.
RUN mvn -ntp -DskipTests package

# eclipse-temurin:21-jre
FROM eclipse-temurin@sha256:ff92a063dac03a29fca3987bcfc66aa5f17769542df802bbf7d2ebaba4df8ebe
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
# pela plataforma (railway.toml) e deve ser usada por orquestradores.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080}' || exit 1

ENTRYPOINT ["sh","-c","exec java ${JAVA_OPTS} -Dquarkus.http.port=${PORT:-8080} -jar /app/quarkus-run.jar"]
