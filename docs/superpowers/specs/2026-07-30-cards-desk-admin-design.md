# Cards Desk — Admin UI + GET/list API

**Data:** 2026-07-30  
**Decisão de produto:** opção A — estender a Cards API com leitura e construir painel admin completo.

## Contexto

A Cards API (Quarkus) hoje só expõe mutações e CVV. O painel de portfólio precisa listar/gerir contas, clientes e cartões contra a API real + Keycloak no host `54.94.163.136`.

AssinaFlow admin (verde / Sora / IBM Plex) não deve ser clonado visualmente.

## Approaches consideradas

| # | Approach | Prós | Contras |
|---|----------|------|---------|
| 1 | **API GET + Vite/React admin no monorepo** | Alinha com AssinaFlow (Vite); nginx estático na 9085; CORS/proxy simples | Não é Next.js |
| 2 | Next.js admin separado | SSR/BFF para token | Mais RAM no EC2; stack nova no repo |
| 3 | Só UI com IDs locais | Zero mudança na API | Demo frágil; rejeitado (opção A) |

**Escolhido: #1** — Vite + React + TypeScript em `admin/`, GETs na API Quarkus, deploy nginx `:9085`.

## Direção visual — “Ledger Steel”

- **Marca:** Cards Desk (AltBank issuer ops)
- **Fundo:** grafite profundo `#0a0e14` com malha/gradiente frio (não flat)
- **Acento:** aço ciano `#2ec4b6` + detalhe chip ouro `#c4a35a` (cartão)
- **Tipografia:** Syne (display/marca) + Figtree (UI)
- **Login brand-first:** nome Cards Desk dominante; um headline; CTA; plano de cartão full-bleed como âncora visual
- **Evitar:** purple/indigo, cream+terracotta, broadsheet, clone verde AssinaFlow

## API — endpoints de leitura

Todos autenticados (`@RolesAllowed("user")`), snake_case Jackson já ativo.

| Método | Path | Resposta |
|--------|------|----------|
| GET | `/accounts` | lista resumida (id, status, customer_id, customer_name, document, created_at, cancelled_at) |
| GET | `/accounts/{accountId}` | detalhe + customer embutido + ids dos cartões ativos |
| GET | `/customers` | lista (id, full_name, document, email, phone, created_at) |
| GET | `/customers/{customerId}` | detalhe + address + account_id se houver |
| GET | `/physical-cards` | lista (filtros opcionais `accountId`) |
| GET | `/physical-cards/{cardId}` | detalhe (sem segredos) |
| GET | `/virtual-cards` | lista (filtros opcionais `accountId`) |
| GET | `/virtual-cards/{cardId}` | detalhe **sem CVV** (CVV continua só em GET `/virtual-cards/{id}/cvv`) |

Webhooks: sem store de status na API. UI oferece consola operacional (POST carrier/processor com `X-Webhook-Api-Key`) e documentação dos endpoints.

## Auth

- Keycloak realm `quarkus`, utilizador demo `alice` / `alice`
- Client público `cards-admin` (direct access grants) no `quarkus-realm.json`
- Admin: password grant → Bearer JWT nas chamadas à API
- CORS Quarkus: origem do admin (`http://54.94.163.136:9085`, localhost)
- Nginx admin pode proxy `/api/` → `cards-api:8080` e `/auth/` → Keycloak para evitar CORS no browser (preferido no deploy)

## Telas do admin

1. **Login** — brand-first
2. **Dashboard** — contagens + atalhos
3. **Contas** — lista + criar + detalhe (cancelar, emitir virtual)
4. **Clientes** — lista + detalhe
5. **Cartões físicos** — lista + detalhe (validar, reemitir, simular entrega via webhook)
6. **Cartões virtuais** — lista + detalhe (CVV sob demanda, reemitir)
7. **Webhooks** — formulários carrier / processor

## Deploy

- Porta **9085** (9084 reservada AssinaFlow admin se subir)
- Container `cards-admin` (nginx) no compose portfolio
- Rebuild `cards-api` com novos GETs
- Atualizar `/opt/portfolio/index.html` e hub Pages

## Fora de escopo

- Paginação avançada / filtros complexos
- CRUD de clientes independente (cliente nasce com conta)
- Histórico persistido de webhooks
- MFA / authorization code flow completo

## Riscos

- Realm Keycloak no EC2 já importado: pode precisar reimport ou criar client via Admin API
- Sem dados seed: painel cria conta na demo
