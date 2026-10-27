# Matriz de fechamento — hardening V4 (cards-api)

Modalidade entregável: **sandbox/kit** (sem processadora/transportadora contratada).
Branch de trabalho: `feature/melhoria-refactor`. Git/publicação são do proprietário.
Baseline e final: `mvn test` verde; final = **78 testes** (unitários; integração em grupo à parte). Empacotamento `-Dquarkus.profile=prod package`: exit 0.

Legenda de estado: **corrigido** (nesta execução) · **já atendido** · **não aplicável** · **bloqueio externo** · **pendência local**.

## P0/P1 — escopo de implementação

| ID | Requisito | Evidência inicial | Implementação (arquivos) | Prova | Estado |
|----|-----------|-------------------|--------------------------|-------|--------|
| 1 | Permissões por objeto (IDOR) em listar/consultar/CVV/reemitir/validar; titular derivado da identidade; admin explícito; não vazar existência de recurso alheio | `@RolesAllowed("user")` sem checagem de posse; `list(null)`=`listAll()`; use cases recebem só UUID; sem uso de identidade no código | `CallerContext`/`JwtCallerContext`; `OwnershipPolicy`; `Customer.subject`+`V3`; ownership em Get CVV, Query/Reissue virtual, Query/Validate/Reissue físico; contas/clientes → `@RolesAllowed("admin")`; mismatch → 404 (mesma resposta de not-found) | `OwnershipPolicyTest` (7), `QueryVirtualCardsUseCaseTest` (6, inclui query `accountId` de outro), negativos A-vs-B em Get CVV/Reissue virtual/Validate/Reissue físico | **corrigido** |
| 2 | CVV: sem persistência/log/trace/cache; `Cache-Control` anti-cache; dados sintéticos; sem alegar PCI | Sem coluna CVV/PAN (só expiração); CVV só in-memory no simulador; sem log de CVV/PAN; CVV sem header de cache | `GET /virtual-cards/{id}/cvv` → `RestResponse` com `Cache-Control: no-store` + `Pragma: no-cache` | `VirtualCardsResourceTest.getCvv_setsNoStoreCacheHeaders` | **corrigido** (persistência/log já atendidos) |
| 3 | Processadora real: contrato provisionar/ativar/bloquear/reemitir/consultar com timeout/idempotência/retry/reconciliação; prod sem adaptador real recusa início | Simuladores `@ApplicationScoped` ativos em qualquer perfil; sem timeout/idempotência; prod rodava com simulador | Simuladores `@UnlessBuildProfile("prod")`; `UnconfiguredProcessorPort`/`UnconfiguredCarrierPort` (prod, fail-closed); `ProductionReadinessGuard` bloqueia início em prod sem adaptador real; flags `cards.processor.adapter`/`cards.carrier.adapter` | `ProductionReadinessTest` (5); prod `package` exit 0 (wiring) | **corrigido** (bloqueio+contrato); timeout/idempotência/reconciliação do adaptador real: **bloqueio externo** (sem provedor) |
| 4 | Máquinas de estado; proteger reemissão duplicada, conta encerrada, dupla ativação, webhook antigo regredindo estado | Entrega sobrescrevia incondicional; expiração de CVV podia retroceder; sem guarda de ordem | `PhysicalCard.applyCarrierDeliveryUpdate` com guarda de staleness (ordem por timestamp) + terminalidade (DELIVERED/RETURNED não regridem); `VirtualCard.applyCvvRotation` só-para-frente | `HandleCarrierDeliveryWebhookUseCaseTest` (out-of-order, duplicado), `HandleProcessorCvvRotationWebhookUseCaseTest` (rotação não-mais-nova = no-op) | **corrigido** (parcial — ver 6/Bloqueios para constraint de idempotência persistida) |
| 5 | Webhooks: idempotência/replay; validar corpo/tamanho/ordem; responder a evento desconhecido com segurança | API key timing-safe (já existia); sem dedupe; sem limite de corpo; estado regredia | Reenvio de evento igual/antigo = no-op (idempotente por ordem); `quarkus.http.limits.max-body-size=256K`; API key preservada | Testes de webhook acima; `WebhookApiKeyFilterTest` preservado (3) | **corrigido** (parcial); assinatura HMAC/nonce/event-id persistido: **bloqueio externo** (contrato do provedor) |
| 6 | Operação/auditoria; migrações, backup/restore, health/readiness, redaction; expor /q/* em prod | `/q/*` `permit`; swagger sempre incluído; sem auditoria | `/q/*` não é mais público; health `/q/health/*` público (probes); swagger só em dev; migração V3 compatível; **trilha de auditoria** (`audit_logs` V5 + `AuditService`) | `application.properties`; `V3`/`V5`; `AuditServiceTest` | **corrigido** |
| 7 | Contratos/SDK: OpenAPI, erros estáveis, paginação, exemplos sintéticos; limites/sandbox | OpenAPI presente; listagens sem paginação | OpenAPI preservado; **paginação** (`page`/`size`, teto 100); limites de corpo; kit documentado | `PageRequestTest`, `QueryVirtualCardsUseCaseTest` | **corrigido** / **já atendido** (OpenAPI, erros) |
| 8 | Qualidade: CI compila+testa; cenários de autorização e webhook; deps/licenças | Sem CI; só sync destrutivo | `.github/workflows/ci.yml` (`mvn verify`, `contents:read`); sync → só-leitura; Docker digest+non-root+HEALTHCHECK | CI YAML válido; imagem build (ver EVIDENCIAS) | **corrigido** (CI/gates); integração com banco real em CI: **pendência local** |

## Cenários mínimos de aceite

| # | Cenário | Estado | Prova |
|---|---------|--------|-------|
| 1 | Usuário A (user) não acessa lista/dados/CVV/reemissão de B, inclusive por `accountId` | **corrigido** | `OwnershipPolicyTest`, `QueryVirtualCardsUseCaseTest.list_whenOwnerRequestsAnotherAccountId...`, negativos nos use cases |
| 2 | CVV fora de banco/log/trace/cache; exemplos sintéticos | **corrigido/já atendido** | sem coluna CVV; `no-store`; sem log |
| 3 | Prod sem provedor/segredos recusa início; demo funcional e identificada | **corrigido** | `ProductionReadinessGuard`; defaults só em dev/test; `ProductionReadinessTest` |
| 4 | Repetição/concorrência de reemissão não gera dois cartões remotos; timeout reconciliado | **parcial/bloqueio externo** | constraint local + ordem; idempotência remota depende do provedor |
| 5 | Webhook repetido não repete efeito; assinatura inválida/evento antigo/payload incompatível tratados | **corrigido (parcial)** | testes de webhook; HMAC = bloqueio externo |
| 6 | Reinício e duas instâncias preservam estado; CI reproduzível em checkout limpo | **parcial** | estado persiste em MariaDB (não in-memory para dados de negócio); CI adicionada |

## Reconciliação V3

- **CARDS-API-01** (IDOR CVV/ownership): **corrigido** — ver linha 1.
- **CARDS-API-02** (simuladores in-memory em qualquer perfil): **corrigido** — `@UnlessBuildProfile("prod")` + fail-closed.
- **CARDS-API-03** (defaults de segredo): **corrigido** — defaults só dev/test; prod sem default.
- **CARDS-API-04** (sem CI; divergência de LICENSE): CI **corrigida**; divergência de LICENSE **registrada para o proprietário** (não alterada).

## Bloqueios externos e pendências locais

**Bloqueios externos (precisam de insumo do proprietário/terceiro):**
1. **Adaptador real de processadora** — insumo: `PROCESSOR_ADAPTER=<provedor homologado>` + implementação do adaptador + credenciais sandbox. Capacidade afetada: emissão/CVV/reemissão/consulta reais. Hoje: contrato `ProcessorPort` pronto, simulador para dev/test, prod bloqueado (fail-closed).
2. **Adaptador real de transportadora** — insumo: `CARRIER_ADAPTER=<provedor>` + implementação. Mesma estrutura.
3. **Assinatura de webhook (HMAC/mTLS, nonce/event-id, janela)** — depende do esquema do provedor (não copiar de outro). Hoje: API key timing-safe + idempotência por ordem.
4. **Audience OIDC em prod** — requer mapper de audience no IdP (`OIDC_AUDIENCE`).

**Pendências locais — resolvidas na 2a rodada (incrementos E–H):**
- ✅ **Paginação** nas listagens (`page`/`size`, teto 100) — `PageRequest` + `listAll(offset,limit)` nos 4 repos + resources. Tests: `PageRequestTest`, `QueryVirtualCardsUseCaseTest`.
- ✅ **Idempotência persistida de webhooks** — tabela `webhook_events` (V4) + `WebhookEventRepository` + fingerprint (sem CVV); dedupe duravel sobrevive a restart/instancias. Tests: dedupe em carrier e processor.
- ✅ **Trilha de auditoria** — `audit_logs` (V5) + `AuditService` em criar/cancelar conta e emitir cartao (ator, acao, target, correlation id; sem material sensivel). Tests: `AuditServiceTest`.
- ✅ **Healthcheck do `cards-api`** no `docker-compose` + `cards-admin` depende de `service_healthy`.
- ◻️ **Teste de integração HTTP** (@QuarkusTest + MariaDB Dev Services + @TestSecurity/@JwtSecurity) escrito e **compilando** (`CardsFlowIntegrationTest`, grupo `integration`): jornada criar→entregar→validar→emitir→CVV + IDOR (A vs B) + header no-store. **Não executável neste sandbox** (Testcontainers nao alcança o daemon Docker a partir da JVM de teste); **roda na CI** (`ci.yml`, passo dedicado).

**Pendências locais — também resolvidas:**
- ✅ **Idempotência de reemissão** — `UNIQUE` em `previous_virtual_card_id`/`previous_physical_card_id` (V6): cada cartão é predecessor de no máximo um novo cartão (barreira de concorrência). Retry sequencial já era barrado pelo estado (cartão antigo inativo → 403). Prova: `CardsFlowIntegrationTest.reissueIsNotDuplicatedForTheSameCard` (CI).

**Remanescente (documentado, fora de escopo imediato):**
- Segredos default no `docker-compose` permanecem para dev (documentado); produção usa env sem default.
- Polimento opcional: mapear violação de constraint de concorrência para HTTP 409 (hoje a duplicata é impedida; o perdedor da corrida recebe erro genérico). Requer teste de integração para validar o tipo de exceção — adiado.

Ver `PACOTE-COMERCIAL.md` para a oferta e `EVIDENCIAS.md` para comandos/versões/provas.
