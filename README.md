# cards-api

API para gestão de Conta, Cliente e Cartões (físico e virtual), com webhooks de transportadora e processadora e simulação local de processadora com CVV somente em memória.

## Premissas de segurança

- O endpoint de consulta de CVV sempre consulta a processadora via adapter.
- Webhooks exigem API Key via header `X-Webhook-Api-Key`.
- A aplicação não registra CVV em logs.

## Requisitos

- Java 21
- Maven
- Docker (para MySQL local)

## Subindo dependências

```bash
docker compose up -d
```

Banco local (docker-compose):
- host: localhost
- porta: 3306
- database: cards_api
- user: cards
- password: cards
- root password: root

## Configuração

As configurações abaixo possuem default para ambiente local.

```bash
export CARRIER_WEBHOOK_API_KEY=carrier-local-key
export PROCESSOR_WEBHOOK_API_KEY=processor-local-key
```

Opcional:

```bash
export DB_JDBC_URL="jdbc:mysql://localhost:3306/cards_api?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USERNAME=cards
export DB_PASSWORD=cards
export CVV_DEFAULT_TTL_SECONDS=900
```

## Rodando a aplicação

```bash
mvn quarkus:dev
```

Flyway aplica as migrations automaticamente no startup.

OpenAPI:
- http://localhost:8080/openapi
- http://localhost:8080/q/swagger-ui

## Rodando testes

```bash
mvn test
```

## Fluxo principal via curl

### 1) Criar conta (cria customer + account + emite físico e gera tracking)

```bash
curl -s -X POST http://localhost:8080/accounts   -H 'Content-Type: application/json'   -d '{
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
curl -s -X POST http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/validate
```

### 4) Emitir cartão virtual (exige físico entregue e validado)

```bash
curl -s -X POST http://localhost:8080/accounts/ACCOUNT_ID/virtual-cards
```

Resposta contém `virtual_card_id` e `processor_card_id`.

### 5) Consultar CVV do cartão virtual (on demand)

```bash
curl -s http://localhost:8080/virtual-cards/VIRTUAL_CARD_ID/cvv
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
curl -s -X POST http://localhost:8080/physical-cards/PHYSICAL_CARD_ID/reissue   -H 'Content-Type: application/json'   -d '{ "reason": "LOSS" }'
```

### 8) Cancelar conta (desativa conta e cartões)

```bash
curl -s -X POST http://localhost:8080/accounts/ACCOUNT_ID/cancel
```

Após cancelamento, emissão de cartões e consulta de CVV são bloqueadas.
