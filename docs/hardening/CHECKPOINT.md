# Checkpoint de execução — hardening V4 (cards-api)

Retomada segura de tarefa. Branch de trabalho: `feature/melhoria-refactor` (não trocar).
Baseline: `mvn test` verde (exit 0) no início. Java 21, Quarkus 3.8.3, Maven 3.9.12.

Regras: sem operações git (do proprietário); sem atribuição de IA em código/docs; modalidade = **sandbox/kit** (sem processadora contratada → contrato + adaptador de teste + bloqueio de produção).

## Estado dos incrementos

- [x] **A — Config fail-closed:** defaults de segredo só em dev/test (prod sem default → falha start); simuladores `@UnlessBuildProfile("prod")`; adaptadores prod fail-closed (`Unconfigured*Port`); `ProductionReadinessGuard`; audience OIDC em prod; `/q/*` e swagger restritos a dev. Tests: ProductionReadinessTest (5).
- [x] **B — IDOR / posse por objeto:** `Customer.subject` (+ migração V3); `CallerContext`/`JwtCallerContext`; `OwnershipPolicy`; posse nos use cases de cartão (virtual+físico): list/get/cvv/reissue/validate; contas/clientes → `@RolesAllowed("admin")`; `Cache-Control: no-store` no CVV. Tests: OwnershipPolicyTest (7), QueryVirtualCardsUseCaseTest (6), VirtualCardsResourceTest (1), negativos A-vs-B nos use cases.
- [x] **C — Webhooks:** anti-regressão de estado (entrega: ordem por timestamp + terminalidade; CVV: só-para-frente); idempotência a reenvio (eventos iguais/antigos viram no-op); `max-body-size`. Tests: carrier out-of-order/duplicate, CVV stale.
- [x] **D — CI + Docker:** `.github/workflows/ci.yml` (mvn verify, contents:read); sync → verificação só-leitura (sem force-push); Docker digest multi-arch + non-root + HEALTHCHECK.

### 2a rodada (pendências locais escolhidas)

- [x] **E — Paginação:** `PageRequest` + `listAll(offset,limit)` nos 4 repos + resources (`page`/`size`, teto 100). Tests: PageRequestTest, QueryVirtualCardsUseCaseTest.
- [x] **F — Idempotência persistida de webhooks:** `webhook_events` (V4) + `WebhookEventRepository` + fingerprint sem CVV; dedupe duravel. Tests: dedupe carrier/processor.
- [x] **H — Auditoria + healthcheck compose:** `audit_logs` (V5) + `AuditService` (criar/cancelar conta, emitir); healthcheck do `cards-api` no compose. Tests: AuditServiceTest.
- [~] **G — Integração HTTP:** `CardsFlowIntegrationTest` (@QuarkusTest, grupo `integration`) escrito e compilando; execução local bloqueada (Testcontainers sem Docker no sandbox); roda na CI (`ci.yml`).

- [x] **Idempotência de reemissão:** `UNIQUE` em `previous_*_card_id` (V6) + anotações de entidade; cenário no teste de integração.

Estado final de testes: **78 verdes** (`mvn test`, integração excluída). Migrações V1→V6. Empacotamento prod: exit 0.
Pendências/bloqueios externos: ver `MATRIZ.md` secao "Bloqueios".

## Provas
Ver `MATRIZ.md` para ID→evidência→implementação→prova→estado.
