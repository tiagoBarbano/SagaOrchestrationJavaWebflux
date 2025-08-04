# Orchestration Saga Application

Este projeto implementa uma orquestração de sagas distribuídas utilizando Spring Boot, MongoDB, RabbitMQ e documentação AsyncAPI via Springwolf.

## Funcionalidades

- **Orquestração de Sagas**: Gerencia fluxos transacionais distribuídos com suporte a fallback e rollback.
- **Mensageria**: Utiliza RabbitMQ para comunicação entre etapas da saga.
- **Persistência**: Armazena execuções de sagas no MongoDB.
- **Documentação AsyncAPI**: Gera documentação interativa dos canais e mensagens assíncronas usando Springwolf.
- **Observabilidade**: Suporte a Prometheus, Grafana, Loki, Tempo e Jaeger (via Docker Compose).

## Estrutura do Projeto

- `src/main/java/com/saga/orchestration/`
  - `saga/`: Implementação do motor de saga ([SagaEngine.java](src/main/java/com/saga/orchestration/saga/SagaEngine.java))
  - `service/`: Serviços de negócio e etapas da saga ([VendasSaga.java](src/main/java/com/saga/orchestration/service/VendasSaga.java))
  - `model/`: Modelos de dados ([SagaExecution.java](src/main/java/com/saga/orchestration/model/SagaExecution.java))
  - `repository/`: Repositório MongoDB ([SagaExecutionRepository.java](src/main/java/com/saga/orchestration/repository/SagaExecutionRepository.java))
  - `dto/request/`: DTOs de entrada ([Notification.java](src/main/java/com/saga/orchestration/dto/request/Notification.java), [ClienteRequest.java](src/main/java/com/saga/orchestration/dto/request/ClienteRequest.java))
  - `annotations/`: Anotações customizadas para saga ([Step.java](src/main/java/com/saga/orchestration/annotations/Step.java), [Fallback.java](src/main/java/com/saga/orchestration/annotations/Fallback.java), [Rollback.java](src/main/java/com/saga/orchestration/annotations/Rollback.java))
  - `config/`: Configurações do RabbitMQ ([RabbitConfig.java](src/main/java/com/saga/orchestration/config/RabbitConfig.java), [RabbitProperties.java](src/main/java/com/saga/orchestration/config/RabbitProperties.java), [RabbitInfraInitializer.java](src/main/java/com/saga/orchestration/config/RabbitInfraInitializer.java))
- `src/main/resources/application.yml`: Configurações da aplicação.
- `docker/`: Arquivos para observabilidade e infraestrutura (Prometheus, Grafana, Loki, Tempo, Jaeger, Redis, etc).
- `pom.xml`: Dependências Maven, incluindo Springwolf para AsyncAPI.

## Principais Tecnologias

- **Spring Boot 3.5.4**
- **MongoDB** (reactivo)
- **RabbitMQ** (reactivo)
- **Springwolf** (AsyncAPI + UI)
- **Lombok**
- **Prometheus, Grafana, Loki, Tempo, Jaeger** (via Docker Compose)

## Como Executar

### Pré-requisitos

- Java 17+
- Docker e Docker Compose
- MongoDB e RabbitMQ (pode usar os serviços do `docker-compose.yml`)

### Subindo infraestrutura com Docker Compose

```sh
docker-compose -f docker/docker-compose.yml up -d
```

### Executando a aplicação

```sh
./mvnw spring-boot:run
```

### Acessando a documentação AsyncAPI

Após iniciar a aplicação, acesse:

```
http://localhost:8080/asyncapi-ui/index.html
```

## Configuração

Veja [src/main/resources/application.yml](src/main/resources/application.yml) para configurações de MongoDB, RabbitMQ e Springwolf.

## Observabilidade

- **Prometheus**: Métricas em `/actuator/prometheus`
- **Grafana**: Dashboards em `localhost:3001`
- **Loki/Promtail**: Logs centralizados
- **Tempo/Jaeger**: Tracing distribuído

## Estrutura das Sagas

As etapas da saga são definidas em métodos anotados com [`@Step`](src/main/java/com/saga/orchestration/annotations/Step.java), [`@Fallback`](src/main/java/com/saga/orchestration/annotations/Fallback.java) e [`@Rollback`](src/main/java/com/saga/orchestration/annotations/Rollback.java). Veja exemplos em [`VendasSaga`](src/main/java/com/saga/orchestration/service/VendasSaga.java).

## Referências

- [Springwolf Documentation](https://springwolf.github.io/springwolf/)
- [AsyncAPI Specification](https://www.asyncapi.com/docs/reference/specification/v2.6.0)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Project Reactor RabbitMQ](https://projectreactor.io/docs/rabbitmq/release/reference/)

---

**Autor:** Tiago Ventura
**Licença:** Apache