# Evidências de execução — hardening V4 (cards-api)

## Ambiente

- Java: OpenJDK 21.0.12.1 (Temurin/Ubuntu) · Maven 3.9.12 · Docker 29.8.2
- Quarkus 3.8.3 · JUnit 5 + Mockito 5.10 · JaCoCo 0.8.12
- Branch de trabalho: `feature/melhoria-refactor` · HEAD inicial: `2264057620393...`
- Working tree com alterações **não commitadas** (git/publicação são do proprietário).

## Gates executados (local)

| Gate | Comando | Resultado |
|------|---------|-----------|
| Build + testes unitários | `mvn -ntp -B clean verify` | **BUILD SUCCESS — 83 testes, 0 falhas/erros** (grupo `integration` excluído) |
| Teste de integração HTTP (banco real) | `mvn -ntp -B test -Dgroups=integration -Dsurefire.excluded.groups=` | **BUILD SUCCESS — 6 testes, 0 falhas** (MariaDB dos Dev Services). O comando antigo (`-DexcludedGroups=`) executava **zero** testes |
| Augmentação/empacotamento prod (wiring) | `mvn -ntp -DskipTests -Dquarkus.profile=prod package` | **exit 0** (beans fail-closed de prod resolvem injeção) |
| Lint YAML workflows | `python3 -c "yaml.safe_load(...)"` | ok |
| Whitespace do diff | `git diff --check` | sem erros |
| Imagem Docker (digest+non-root+healthcheck) | `docker build -t cards-api:hardening-check .` | ver abaixo |

> Observação de ambiente: o contexto Docker ativo (`desktop-linux`) estava indisponível;
> o build foi executado via contexto `default` (`/var/run/docker.sock`, server 29.8.2).

### Resultado do build da imagem

- `docker build` concluído com **exit 0** (via contexto `default`).
- Imagem `cards-api:hardening-check` (546MB). Inspeção: **User=1001 (não-root)**, **HEALTHCHECK presente**.
- Bases fixadas por digest de índice multi-arch:
  - `maven:3.9.6-eclipse-temurin-21` → `sha256:8d63d4c1902cb12d9e79a70671b18ebe26358cb592561af33ca1808f00d935cb`
  - `eclipse-temurin:21-jre` → `sha256:ff92a063dac03a29fca3987bcfc66aa5f17769542df802bbf7d2ebaba4df8ebe`
- Scan de CVEs da imagem: **executado** com trivy (`--severity HIGH,CRITICAL --scanners vuln`,
  sem `ignore-unfixed`) em 2026-10-29.
  - Imagem anterior (Quarkus 3.8.3): **33 HIGH + 1 CRITICAL**, todos em JARs da aplicação
    (`lang-pkgs`) e **todos com correção disponível** — jackson-core/databind 2.16.1, netty
    4.1.107.Final (inclui o CRITICAL CVE-2026-75595 em netty-handler), jose4j 0.9.3,
    quarkus-core e quarkus-vertx-http 3.8.3. A base Ubuntu 26.04 não contribuiu com nenhum achado.
  - Os CVEs de `quarkus-vertx-http` não têm correção na linha 3.8, o que exigiu subir a
    plataforma para **3.40.1.1** (e renomear `quarkus-resteasy-reactive-jackson` →
    `quarkus-rest-jackson` e `quarkus-junit5` → `quarkus-junit`).
  - Imagem final após a correção: **0 HIGH, 0 CRITICAL** (`trivy --exit-code 1` devolve 0).
  - O gate passou a ser **bloqueante** na CI (job `image-scan`): mesma imagem da entrega, mesmo
    Dockerfile, `exit-code: '1'`, sem `continue-on-error`, sem `ignore-unfixed` e sem exclusões.

## Cobertura de testes nova (cenários de segurança)

- `application.security.OwnershipPolicyTest` — admin, dono, mismatch, subject ausente, resolução de conta (7).
- `application.usecase.QueryVirtualCardsUseCaseTest` — escopo de posse em list/get, `accountId` de outro = vazio (6).
- `interfaces.rest.VirtualCardsResourceTest` — `Cache-Control: no-store` no CVV (1).
- `infrastructure.config.ProductionReadinessTest` — fail-closed de prod (5).
- Negativos A-vs-B adicionados em: Get CVV, Reissue virtual, Validate físico, Reissue físico.
- Webhooks: out-of-order e duplicado (carrier); rotação não-mais-nova = no-op (processor).

## Inventário de alterações

**Arquivos novos (main):**
- `application/security/CallerContext.java`, `application/security/OwnershipPolicy.java`
- `infrastructure/security/JwtCallerContext.java`
- `infrastructure/config/ProductionReadiness.java`, `ProductionReadinessGuard.java`
- `infrastructure/external/UnconfiguredProcessorPort.java`, `UnconfiguredCarrierPort.java`
- `resources/db/migration/V3__customer_subject.sql`

**Arquivos novos (test):** `OwnershipPolicyTest`, `QueryVirtualCardsUseCaseTest`, `VirtualCardsResourceTest`, `ProductionReadinessTest`.

**Novos (infra/docs):** `.github/workflows/ci.yml`, `docs/hardening/*`.

**Modificados (destaques):** `application.properties` (fail-closed/segredos/perfis/CORS/limites), 6 use cases de cartão (posse), `Customer`/`PhysicalCard`/`VirtualCard` (subject, anti-regressão, rotação), `AccountsResource`/`CustomersResource` (admin), `VirtualCardsResource` (CVV no-store), `AccountRepository`+`JpaAccountRepository` (findBySubject), `CreateAccountRequest`/`CreateAccountUseCase` (subject), `Dockerfile`, `sync-master-with-develop.yml`.

Inventário completo: `git status` (ver seção no CHECKPOINT/README). O build não depende de arquivo oculto fora do conjunto entregável. Nenhuma credencial/dump/artefato de cache incluído.

## Separação de escopos

- **Local:** tudo acima (verde).
- **CI remota:** depende de publicação pelo proprietário (não executada aqui).
- **Sandbox/produção:** adaptadores reais e credenciais pendentes (bloqueio externo — ver `MATRIZ.md`).
