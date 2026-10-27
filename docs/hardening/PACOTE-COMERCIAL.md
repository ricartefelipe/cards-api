# Cards API — Kit de integração de cartões (sandbox)

> Modalidade desta entrega: **kit/sandbox licenciável**. Operação real de emissão de
> cartões depende de processadora e transportadora contratadas e homologadas — não
> incluídas e não simuláveis como reais.

## Para quem é / problema que resolve

Times de produto e engenharia de instituições (ou fintechs) que precisam de uma base
de **gestão de cartões virtuais e físicos** (emissão, consulta, CVV sob demanda,
reemissão, entrega e webhooks) com arquitetura limpa e segurança por objeto, pronta
para plugar uma processadora real quando contratada.

## O que o kit entrega

- API REST (Quarkus/Java 21) com arquitetura hexagonal: domínio, casos de uso, portas e adaptadores.
- **Autorização por objeto (anti-IDOR):** titular só acessa os próprios recursos; `admin`/suporte com acesso explícito.
- **Higiene de dados sensíveis:** CVV nunca persistido/logado; resposta com `Cache-Control: no-store`; sem PAN no sistema.
- **Fail-closed de produção:** sem adaptador real e sem segredos válidos, a aplicação recusa iniciar; simuladores restritos a dev/test.
- **Webhooks** com API key em tempo constante, idempotência a reenvio e proteção contra regressão de estado.
- **Contrato de processadora/transportadora** (`ProcessorPort`/`CarrierPort`) + **adaptador de teste** para dev/demo.
- Migrações versionadas (Flyway/MariaDB), OpenAPI, CI de build/testes, imagem Docker endurecida (non-root, digest fixo, healthcheck).

## O que NÃO está incluído (depende de contratação/homologação)

- Integração com processadora/emissor real (provisionamento/CVV/reemissão reais).
- Integração com transportadora real.
- Esquema de assinatura de webhook do provedor (HMAC/mTLS/nonce) — específico de cada parceiro.
- Certificação PCI-DSS ou equivalente (exige avaliação independente — **não declarada**).

## Modalidade e licença — a decidir pelo proprietário

- Licença do repositório: ver `LICENSE` (há divergência histórica de termos entre branches, **registrada para decisão do proprietário**, não alterada aqui).
- Escolha explícita entre **kit/sandbox** (esta entrega) e **integração operacional homologada** (requer parceiros). Um caminho não autoriza anunciar o outro.

## Custos e dependências

- Runtime: JVM 21, MariaDB, IdP OIDC (Keycloak ou equivalente). Processadora/transportadora e credenciais sandbox quando for integrar.
- Sem custos de terceiros assumidos nesta entrega; contratação/credenciais/hardware seguem como dependências reais.

## Onboarding / offboarding

- **Onboarding:** subir MariaDB + IdP, aplicar migrações (automático no start via Flyway), configurar segredos (sem defaults em prod), provisionar clientes/contas vinculando o `subject` do IdP.
- **Offboarding:** exportação via API/`mysqldump`; remoção de credenciais; sem lock-in de formato proprietário.

## Demo sintética

- Perfil dev/test com simuladores de processadora/transportadora e dados sintéticos (nenhum CVV/PAN real). Identificada como demo e **inacessível no perfil de produção** (fail-closed).

## Suporte e piloto

- Suporte e SLAs: **a definir pelo proprietário** (não precificados aqui).
- Piloto sugerido: ambiente isolado com sandbox do provedor, dados sintéticos, critérios de aceite dos cenários de segurança (ver `MATRIZ.md`).

## Itens adiáveis (P2/P3) — benefício × esforço

| Item | Prioridade | Benefício | Esforço | Dependência |
|------|-----------|-----------|---------|-------------|
| Paginação nas listagens | P2 | Escala/custo | Baixo | — |
| Trilha de auditoria persistida | P2 | Compliance/suporte | Médio | — |
| SDKs gerados + portal do integrador | P2 | Adoção | Médio | OpenAPI estável |
| Step-up de autenticação em operações sensíveis | P2 | Segurança | Médio | Contrato do produto |
| Processador/banco completo embutido | P3 | — | Alto | Fora de escopo |
| Persistir CVV para facilitar demo | P3 | — | — | **Proibido** (segurança) |
