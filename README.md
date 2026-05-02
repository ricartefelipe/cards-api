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

```bash
docker compose up -d
```

Banco local (MariaDB no compose — serviço `mysql`; driver JDBC padrão `jdbc:mariadb://`):
- host: localhost
- porta: 3306
- database: cards_api
- user: cards
- password: cards
- root password: root

Keycloak (docker-compose, opcional em dev):
- URL: http://localhost:8180
- Admin: admin/admin
- Realm `quarkus` importado automaticamente com client `backend-service` e usuários alice, admin

## Configuração

As configurações abaixo possuem default para ambiente local.

```bash
export CARRIER_WEBHOOK_API_KEY=carrier-local-key
export PROCESSOR_WEBHOOK_API_KEY=processor-local-key
```

OAuth2/Keycloak (produção):

```bash
export KEYCLOAK_URL=http://localhost:8180
export KEYCLOAK_REALM=quarkus
export KEYCLOAK_CLIENT_SECRET=secret
```

Opcional:

```bash
export DB_JDBC_URL="jdbc:mariadb://localhost:3306/cards_api?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=cards
export DB_PASSWORD=cards
export CVV_DEFAULT_TTL_SECONDS=900
```

## Rodando a aplicação

```bash
mvn quarkus:dev
```

Em modo dev, o **Dev Services** inicia automaticamente um Keycloak e importa o realm `quarkus` com usuários pré-configurados.

Flyway aplica as migrations automaticamente no startup.

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
  -u "backend-service:secret" \
  -d "username=alice&password=alice&grant_type=password" | jq -r '.access_token')
```

Usuários de exemplo: `alice`/`alice` (role user), `admin`/`admin` (roles user, admin).

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
