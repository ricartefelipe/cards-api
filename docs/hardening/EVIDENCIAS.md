# Evidências de execução — hardening V4 (cards-api)

## Ambiente

- Java: OpenJDK 21.0.12.1 (Temurin/Ubuntu) · Maven 3.9.12 · Docker 29.8.2
- Quarkus 3.8.3 · JUnit 5 + Mockito 5.10 · JaCoCo 0.8.12
- Branch de trabalho: `feature/melhoria-refactor` · HEAD inicial: `2264057620393...`
- Working tree com alterações **não commitadas** (git/publicação são do proprietário).

## Gates executados (local)

| Gate | Comando | Resultado |
|------|---------|-----------|
| Build + testes unitários | `mvn -ntp test` | **BUILD SUCCESS — 78 testes, 0 falhas/erros** (grupo `integration` excluído) |
| Teste de integração HTTP | `mvn test -Dgroups=integration -DexcludedGroups=` | **compila**; execução **bloqueada no sandbox** (Testcontainers nao alcança o daemon Docker da JVM de teste); roda na CI |
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
- Scan de CVEs da imagem: **pendência local** (scanner não executado nesta sessão; recomendado em CI contra os digests acima).

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
