# cards-api

API REST (Quarkus) para gestão de Conta, Cliente e Cartões (físico e virtual), com webhooks de transportadora e processadora e simulação local de processadora com CVV somente em memória.

## Premissas de segurança

- O endpoint de consulta de CVV sempre consulta a processadora via adapter.
- Webhooks exigem API Key via header `X-Webhook-Api-Key`.
- Endpoints de conta e cartões exigem autenticação OAuth2 + JWT (Keycloak).
- A aplicação não registra CVV em logs.

## Requisitos

- Java 21
- Maven
- Docker (serviços locais em `docker-compose.yml`: MariaDB e Keycloak opcional/fora dos Dev Services)

## Subindo dependências

> **Credenciais:** Nunca comite `.env`. Gere valores fortes antes do primeiro uso (por exemplo `openssl rand -hex 24`). Para `KEYCLOAK_CLIENT_SECRET`, deve coincidir com o segredo configurado para o client `backend-service` no ficheiro `src/main/resources/quarkus-realm.json` importado pelo Keycloak ao subir via Compose (`docker compose`/Keycloak aceita também rotações feitas só no servidor). Quem alterar passwords ou segredos deve regenerar também o realm conforme política da equipa ou via consola do Keycloak. Valores já expostos em repositório ou histórico devem considerar-se inválidos: substitua todos nos ambientes reais independentemente das alterações feitas ao código-fonte.

Antes do primeiro `docker compose up`, crie `.env` na raiz (este ficheiro está no `.gitignore`):

```bash
cp .env.example .env
```

Consulte `.env.example` para os nomes obrigatórios; preencha cada variável antes de iniciar Compose ou Quarkus (`mvn quarkus:dev` também lê `.env`).

```bash
docker compose up -d
```

Banco local (MariaDB no compose — serviço `mysql`; driver JDBC padrão `jdbc:mariadb://`):
- host: localhost
- porta: 3306
- database: cards_api
- user: `cards` (ou `MARIADB_USER` no `.env`)
- password: o valor de `MARIADB_PASSWORD` / `DB_PASSWORD` no `.env`
- root password: `MARIADB_ROOT_PASSWORD` no `.env`

Keycloak (docker-compose, opcional em dev):
- URL: http://localhost:8180
- Conta de **administração do servidor** (KC bootstrap): defina apenas no `.env`, com credenciais fortes próprias.
- Realm `quarkus` é importado a partir de `src/main/resources/quarkus-realm.json`, incluindo o client confidencial `backend-service`; o seu `.env` deve usar o mesmo `KEYCLOAK_CLIENT_SECRET` (`KEYCLOAK_CLIENT_SECRET` igual ao campo `secret` desse client no JSON). Utilizadores de realm existentes servem apenas para desenvolvimento: altere sempre as passwords antes de usar fora da sua máquina (consola ou novo export/regeneração dos hashes).

Para obter o segredo atual do client `backend-service` no repositório (por exemplo antes de gravar `.env`), pode usar:

```bash
python3 - <<'PY'
import json
from pathlib import Path
realm = Path("src/main/resources/quarkus-realm.json")
with realm.open(encoding="utf-8") as f:
    doc = json.load(f)
secret = next(
    (c["secret"] for c in doc.get("clients", []) if c.get("clientId") == "backend-service"),
    None,
)
assert secret is not None, "realm sem client backend-service"
print(secret)
PY
```

## Configuração

A aplicação **não** usa senhas ou API keys fictícias por omissão em `application.properties`. Carregue um ficheiro `.env` na raiz (o Quarkus lê-o em dev) ou exporte manualmente os mesmos nomes antes de arrancar.

```bash
# Alternativa ao .env na raiz (equivale aos campos relevantes em .env.example)
export DB_PASSWORD=<valor>
export CARRIER_WEBHOOK_API_KEY=<valor>
export PROCESSOR_WEBHOOK_API_KEY=<valor>
export KEYCLOAK_CLIENT_SECRET=<valor>
```

OAuth2 / Keycloak:

- Em **desenvolvimento** (`quarkus:dev`), por omissão: `KEYCLOAK_URL` pode ser omitido desde que utilize `http://localhost:8180` (ver `%dev.quarkus.oidc.auth-server-url`).
- Em **produção** (`-Dquarkus.profile=prod`): `KEYCLOAK_URL` e `KEYCLOAK_CLIENT_SECRET` são obrigatórios.

```bash
export KEYCLOAK_URL=http://localhost:8180   # exemplo; em prod use a URL real do IAM
export KEYCLOAK_REALM=quarkus
export KEYCLOAK_CLIENT_SECRET=<KEYCLOAK_CLIENT_SECRET>
```

Outras JDBC / TTL:

```bash
export DB_JDBC_URL="jdbc:mariadb://localhost:3306/cards_api?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=cards
export DB_PASSWORD=<DB_PASSWORD>
export CVV_DEFAULT_TTL_SECONDS=900
```

## Rodando a aplicação

```bash
mvn quarkus:dev
```

Flyway aplica as migrations automaticamente no startup.

Em modo dev, por omissão podem correr **Dev Services** (por exemplo Keycloak com o realm `quarkus`); se optar por infraestrutura apenas via **Docker Compose**, veja a secção seguinte para evitar sobreposição nas portas **3306** e **8180**.

### Docker Compose versus Dev Services

- O Compose deste repositório usa usualmente **MariaDB na porta 3306** e **Keycloak na 8180** (`docker compose up -d`).
- Com **`mvn quarkus:dev`**, o Quarkus pode iniciar **contentores próprios** (Dev Services) para base de dados e OIDC, consoante as extensões e se já há algo à escuta nas portas envolvidas.
- Para **evitar duplicar serviços ou falhas por porta em uso**:
  - Se quiser usar **só** o MariaDB e o Keycloak do **Compose**, desative no perfil `dev` os Dev Services que repetem o mesmo papel, por exemplo `quarkus.datasource.devservices.enabled=false` e `quarkus.keycloak.devservices.enabled=false`.
  - Alinhe o **OIDC** ao Keycloak que está a usar (URL do realm). Em **dev** o URL padrão é `http://localhost:8180` (ver `application.properties`); em **produção** `KEYCLOAK_URL` tem de ser definido explicitamente.
- Na prática: **não combine** dois Keycloaks ou duas bases a disputar **a mesma porta** (3306, 8180) sem configurar uma das partes explicitamente para outra porta ou para ficar desligada.

OpenAPI:
- http://localhost:8080/openapi
- http://localhost:8080/q/swagger-ui

## Rodando testes

```bash
mvn test
```

Após os testes, o JaCoCo gera relatório HTML de cobertura em `target/site/jacoco/index.html`.

## Autenticação OAuth2 + JWT

Os endpoints `/accounts`, `/physical-cards` e `/virtual-cards` exigem Bearer token JWT válido. Os webhooks continuam usando API Key.

### Obter token (Keycloak)

```bash
export ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/quarkus/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "backend-service:${KEYCLOAK_CLIENT_SECRET}" \
  -d "username=alice&password=${ALICE_PASSWORD}&grant_type=password" | jq -r '.access_token')
```

Defina `KEYCLOAK_CLIENT_SECRET` conforme `.env`; `ALICE_PASSWORD` deve ser uma password válida configurada para o utilizador `alice` no Keycloak utilizado pela sua cópia do realm ou equivalente atualizado.

### Chamadas autenticadas

```bash
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/accounts
```

## Fluxo principal via curl

### 1) Criar conta (cria customer + account + emite físico e gera tracking)

```bash
curl -s -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "customer": {
      "full_name": "Fulano de Tal",
      "document": "12345678900",
      "email": "fulano@example.com",
      "phone": "+55 85 99999-9999"
    },
    "address": {
      "street": "Rua A",
      "number": "100",
      "city": "Fortaleza",
      "state": "CE",
      "zip_code": "60000-000",
      "country": "BR"
    }
  }'
```

Resposta contém `account_id`, `customer_id`, `physical_card_id`, `tracking_id`.

### 2) Webhook da transportadora (marcar entrega)

```bash
curl -s -X POST http://localhost:8080/webhooks/carrier/delivery   -H 'Content-Type: application/json'   -H "X-Webhook-Api-Key: ${CARRIER_WEBHOOK_API_KEY}"   -d '{
    "tracking_id": "TRACKING_DO_RESPONSE",
    "delivery_status": "DELIVERED",
    "delivery_date": "2026-01-31T12:00:00",
    "delivery_return_reason": null,
    "delivery_address": "Rua A, 100, Fortaleza-CE, 60000-000, BR"
  }'
```

### 3) Validar cartão físico (após entregue)

```bash
curl -s -X POST http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/validate \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 4) Emitir cartão virtual (exige físico entregue e validado)

```bash
curl -s -X POST http://localhost:8080/accounts/ACCOUNT_ID/virtual-cards \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resposta contém `virtual_card_id` e `processor_card_id`.

### 5) Consultar CVV do cartão virtual (on demand)

```bash
curl -s http://localhost:8080/virtual-cards/VIRTUAL_CARD_ID/cvv \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Resposta contém `cvv` e `expiration_date`.

### 6) Webhook da processadora (rotação automática de CVV)

```bash
curl -s -X POST http://localhost:8080/webhooks/processor/cvv-rotation   -H 'Content-Type: application/json'   -H "X-Webhook-Api-Key: ${PROCESSOR_WEBHOOK_API_KEY}"   -d '{
    "account_id": "PROCESSOR_ACCOUNT_ID",
    "card_id": "PROCESSOR_CARD_ID",
    "next_cvv": 123,
    "expiration_date": "2026-01-31T12:30:00"
  }'
```

O CVV recebido é mantido apenas na memória do processo, com TTL até `expiration_date`.

### 7) Reemitir físico (perda/roubo/dano)

```bash
curl -s -X POST http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/reissue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "reason": "LOSS" }'
```

Motivos válidos (`reason`): `LOSS`, `THEFT`, `DAMAGE`.

### 8) Reemitir cartão virtual (perda/roubo/dano)

```bash
curl -s -X POST http://localhost:8080/virtual-cards/VIRTUAL_CARD_ID/reissue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "reason": "THEFT" }'
```

Motivos válidos (`reason`): `LOSS`, `THEFT`, `DAMAGE`.

### 9) Cancelar conta (desativa conta e cartões)

```bash
curl -s -X POST http://localhost:8080/accounts/ACCOUNT_ID/cancel \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Após cancelamento, emissão de cartões e consulta de CVV são bloqueadas.
