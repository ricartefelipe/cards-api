# Cards Desk Admin Implementation Plan

**Goal:** Extender Cards API com GET/list e publicar admin Vite “Cards Desk” na porta 9085 com Keycloak.

**Architecture:** Quarkus query use-cases + resources; Vite/React SPA com password grant; nginx serve + proxy `/api` e `/auth`.

**Tech Stack:** Java 21 / Quarkus, Vite 6, React 19, TypeScript, nginx, Docker Compose.

## Global Constraints

- GitFlow: `feature/*` → `develop` → `release/*` → `master`
- Sem trailers ou menções a ferramentas de autoria em commits, PRs ou código versionado
- CVV nunca em list/detail; só endpoint dedicado
- Visual Ledger Steel (Syne + Figtree; não roxo/cream/verde AssinaFlow)
- Porta admin: 9085

---

### Task 1: Repositórios + DTOs de leitura

**Files:**
- Modify ports: `AccountRepository`, `CustomerRepository`, `PhysicalCardRepository`, `VirtualCardRepository`
- Modify JPA impls
- Create DTOs em `application/dto/`

- [ ] Add `List<Account> listAll()`, `Optional<Customer> findById`, `List<Customer> listAll()`, `List<PhysicalCard> listAll()`, `List<PhysicalCard> listByAccountId`, `List<VirtualCard> listAll()`, `List<VirtualCard> listByAccountId`
- [ ] Implement JPQL ordered by `createdAt DESC`
- [ ] Create response records (snake via Jackson naming)
- [ ] Commit: `Expõe consultas de listagem nos repositórios de domínio.`

### Task 2: Use cases + REST GET

**Files:**
- Create use cases `List*` / `Get*`
- Extend `AccountsResource`, `PhysicalCardsResource`, `VirtualCardsResource`
- Create `CustomersResource`
- Update `application.properties` CORS + paths auth

- [ ] Wire GET endpoints per design spec
- [ ] Enable `quarkus.http.cors` for admin origins
- [ ] Add `/customers*` to authenticated paths
- [ ] Unit/integration smoke via existing test style if cheap; else manual curl after build
- [ ] Commit: `Adiciona endpoints GET de contas, clientes e cartões.`

### Task 3: Keycloak client `cards-admin`

**Files:**
- Modify `quarkus-realm.json` (add public client)

- [ ] Public client, direct grants, webOrigins `+` / admin URLs
- [ ] Commit: `Adiciona client público cards-admin para o painel.`

### Task 4: Scaffold admin Vite

**Files:**
- Create `admin/` (package.json, vite, tsconfig, index.html, Dockerfile, nginx.conf)

- [ ] Scaffold React+TS+vite-router
- [ ] AuthContext password grant via `/auth/token` (proxied) or Keycloak URL
- [ ] API client Bearer
- [ ] Styles Ledger Steel + login brand-first
- [ ] Pages: dashboard, accounts, customers, physical, virtual, webhooks
- [ ] Commit: `Adiciona painel Cards Desk com autenticação Keycloak.`

### Task 5: Compose + scripts deploy

**Files:**
- Modify `docker-compose.yml` / portfolio scripts
- Create `admin/Dockerfile`, `admin/nginx.conf`

- [ ] Service `cards-admin` port 9085
- [ ] Proxy `/api/` → cards-api, `/auth/` → keycloak token path
- [ ] Commit: `Empacota o admin na porta 9085 com proxy de API.`

### Task 6: GitFlow PRs

- [ ] Branch `feature/cards-desk-admin` from `develop`
- [ ] PR → develop; merge
- [ ] `release/cards-desk-admin` → master; merge
- [ ] Deploy EC2; update hubs

### Task 7: Smoke

- [ ] Login alice/alice
- [ ] Criar conta, listar, validar fluxo básico
- [ ] Reportar URL, credenciais, PRs, gaps
