# Cards API

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io/)
[![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2%2FOIDC-4D4D4D?logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![MariaDB](https://img.shields.io/badge/MariaDB-Flyway-C3362D?logo=mariadb&logoColor=white)](https://mariadb.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](docker-compose.yml)

API financeira para gestão de contas, clientes e cartões físicos e virtuais.

O projeto concentra o fluxo de emissão, entrega, validação, reemissão e cancelamento de cartões, com autenticação OAuth2/OIDC via Keycloak e integrações por webhook com transportadora e processadora.

Uma das decisões centrais é não persistir CVV: a consulta é feita sob demanda por adapter e o valor recebido permanece apenas em memória pelo período definido.

## Visão geral

| Área | Responsabilidade |
|------|------------------|
| **Contas e clientes** | Cadastro e criação da conta |
| **Cartão físico** | Emissão, tracking, entrega, validação e reemissão |
| **Cartão virtual** | Emissão, consulta de CVV e reemissão |
| **Segurança** | OAuth2/OIDC, JWT e API keys para webhooks |
| **Integrações** | Transportadora e processadora |
| **Persistência** | MariaDB com Flyway |
| **Testes** | JUnit e relatório de cobertura JaCoCo |

## Premissas de segurança

- Endpoints de contas e cartões exigem Bearer token JWT válido.
- Webhooks exigem API Key via `X-Webhook-Api-Key`.
- O CVV não é persistido no banco.
- O CVV não deve aparecer em logs.
- Segredos e credenciais não devem ser commitados.
- Configuração de produção deve usar um provedor OIDC externo e credenciais próprias.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Runtime | Java 21, Quarkus 3 |
| Segurança | Keycloak, OAuth2/OIDC, JWT |
| Dados | MariaDB, Flyway |
| API | REST, OpenAPI, Swagger UI |
| Infra local | Docker Compose |
| Testes | JUnit, JaCoCo |

## Requisitos

- Java 21
- Maven
- Docker e Docker Compose

## Quick Start

Crie o arquivo local de variáveis:

```bash
cp .env.example .env
```

Preencha os segredos antes de iniciar o ambiente.

Depois:

```bash
docker compose up -d
mvn quarkus:dev
```

Endpoints locais:

- API: `http://localhost:8080`
- OpenAPI: `http://localhost:8080/openapi`
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Keycloak: `http://localhost:8180`

Flyway aplica as migrations automaticamente no startup.

## Banco local

O serviço MariaDB do Compose utiliza:

- host: `localhost`
- porta: `3306`
- database: `cards_api`
- usuário: `cards` ou `MARIADB_USER`
- senha: `MARIADB_PASSWORD` / `DB_PASSWORD`

As credenciais devem permanecer no `.env`.

## Keycloak

O realm `quarkus` é importado de:

```text
src/main/resources/quarkus-realm.json
```

O client confidencial utilizado pela API é `backend-service`.

Em desenvolvimento, a aplicação usa por padrão:

```text
http://localhost:8180
```

Em produção, `KEYCLOAK_URL` e `KEYCLOAK_CLIENT_SECRET` devem ser definidos explicitamente.

Variáveis principais:

```bash
export KEYCLOAK_URL=http://localhost:8180
export KEYCLOAK_REALM=quarkus
export KEYCLOAK_CLIENT_SECRET=<valor>
```

Configuração JDBC:

```bash
export DB_JDBC_URL="jdbc:mariadb://localhost:3306/cards_api?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=cards
export DB_PASSWORD=<valor>
export CVV_DEFAULT_TTL_SECONDS=900
```

Webhooks:

```bash
export CARRIER_WEBHOOK_API_KEY=<valor>
export PROCESSOR_WEBHOOK_API_KEY=<valor>
```

## Docker Compose e Dev Services

O Compose disponibiliza normalmente MariaDB em `3306` e Keycloak em `8180`.

Ao executar `mvn quarkus:dev`, o Quarkus pode tentar iniciar Dev Services próprios. Para evitar duplicidade ou conflito de portas quando estiver usando o Compose:

```properties
quarkus.datasource.devservices.enabled=false
quarkus.keycloak.devservices.enabled=false
```

Use apenas uma instância de banco e uma instância de Keycloak por ambiente, ou configure portas diferentes explicitamente.

## Autenticação

Os endpoints `/accounts`, `/physical-cards` e `/virtual-cards` exigem JWT.

Exemplo de obtenção de token:

```bash
export ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/quarkus/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "backend-service:${KEYCLOAK_CLIENT_SECRET}" \
  -d "username=alice&password=${ALICE_PASSWORD}&grant_type=password" | jq -r '.access_token')
```

Exemplo de chamada autenticada:

```bash
curl -s \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/accounts
```

## Fluxo principal

### 1. Criar conta

A criação da conta também cria o cliente, emite o cartão físico e gera o tracking.

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

A resposta contém `account_id`, `customer_id`, `physical_card_id` e `tracking_id`.

### 2. Confirmar entrega pela transportadora

```bash
curl -s -X POST http://localhost:8080/webhooks/carrier/delivery \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Api-Key: ${CARRIER_WEBHOOK_API_KEY}" \
  -d '{
    "tracking_id": "TRACKING_DO_RESPONSE",
    "delivery_status": "DELIVERED",
    "delivery_date": "2026-01-31T12:00:00",
    "delivery_return_reason": null,
    "delivery_address": "Rua A, 100, Fortaleza-CE, 60000-000, BR"
  }'
```

### 3. Validar cartão físico

```bash
curl -s -X POST \
  http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/validate \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 4. Emitir cartão virtual

A emissão exige cartão físico entregue e validado.

```bash
curl -s -X POST \
  http://localhost:8080/accounts/ACCOUNT_ID/virtual-cards \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 5. Consultar CVV

```bash
curl -s \
  http://localhost:8080/virtual-cards/VIRTUAL_CARD_ID/cvv \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

O retorno contém `cvv` e `expiration_date`. O valor não é persistido.

### 6. Rotação de CVV pela processadora

```bash
curl -s -X POST http://localhost:8080/webhooks/processor/cvv-rotation \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Api-Key: ${PROCESSOR_WEBHOOK_API_KEY}" \
  -d '{
    "account_id": "PROCESSOR_ACCOUNT_ID",
    "card_id": "PROCESSOR_CARD_ID",
    "next_cvv": 123,
    "expiration_date": "2026-01-31T12:30:00"
  }'
```

O CVV recebido permanece apenas em memória, respeitando o TTL informado.

### 7. Reemitir cartão físico

```bash
curl -s -X POST \
  http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/reissue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "reason": "LOSS" }'
```

Motivos aceitos:

- `LOSS`
- `THEFT`
- `DAMAGE`

### 8. Reemitir cartão virtual

```bash
curl -s -X POST \
  http://localhost:8080/virtual-cards/VIRTUAL_CARD_ID/reissue \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{ "reason": "THEFT" }'
```

### 9. Cancelar conta

```bash
curl -s -X POST \
  http://localhost:8080/accounts/ACCOUNT_ID/cancel \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

O cancelamento desativa a conta e bloqueia novas emissões e consultas de CVV.

## Testes

```bash
mvn test
```

O relatório JaCoCo é gerado em:

```text
target/site/jacoco/index.html
```

## Licença

MIT — ver [LICENSE](LICENSE).
