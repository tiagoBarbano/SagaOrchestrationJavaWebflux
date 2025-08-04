# Saga Orchestration - Documentação Técnica

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Componentes](#componentes)
4. [Configuração](#configuração)
5. [API Reference](#api-reference)
6. [Deploy](#deploy)
7. [Monitoramento](#monitoramento)
8. [Troubleshooting](#troubleshooting)
9. [Desenvolvimento](#desenvolvimento)

## 🎯 Visão Geral

O **Saga Orchestration** é um motor de orquestração de sagas distribuídas desenvolvido em Spring Boot com WebFlux (reativo), MongoDB, RabbitMQ e documentação AsyncAPI via Springwolf.

### Objetivos

- **Orquestração de Sagas**: Gerencia fluxos transacionais distribuídos com suporte a fallback e rollback
- **Mensageria Assíncrona**: Utiliza RabbitMQ para comunicação entre etapas da saga
- **Persistência Reativa**: Armazena execuções de sagas no MongoDB de forma reativa
- **Documentação Automática**: Gera documentação interativa dos canais e mensagens assíncronas
- **Observabilidade Completa**: Suporte a Prometheus, Grafana, Loki, Tempo e Jaeger

### Tecnologias Principais

- **Spring Boot 3.5.4** - Framework base
- **WebFlux** - Programação reativa
- **MongoDB** - Persistência reativa
- **RabbitMQ** - Mensageria reativa
- **Springwolf** - Documentação AsyncAPI
- **Resilience4j** - Circuit breaker e rate limiting
- **Micrometer** - Métricas e observabilidade

## 🏗️ Arquitetura

### Diagrama de Arquitetura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Saga Engine   │    │   Batch Proc    │    │   Load Balancer │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Sharding  │ │    │ │   Circuit   │ │    │ │   Workers   │ │
│ │   Manager   │ │    │ │   Breaker   │ │    │ │             │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   RabbitMQ      │    │   MongoDB       │    │   Redis Cache   │
│   (Mensageria)  │    │   (Persistência)│    │   (Cache)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Prometheus    │    │   Grafana       │    │   Springwolf    │
│   (Métricas)    │    │   (Dashboards)  │    │   (AsyncAPI)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Princípios Arquiteturais

#### 1. **Reatividade**
- Programação não-bloqueante
- Backpressure control
- Event-driven architecture

#### 2. **Resiliência**
- Circuit breaker pattern
- Rate limiting
- Retry mechanisms
- Fallback strategies

#### 3. **Escalabilidade**
- Horizontal scaling
- Sharding por domínio
- Load balancing
- Partitioning por tenant

#### 4. **Observabilidade**
- Métricas customizadas
- Logs estruturados
- Distributed tracing
- Health checks

## 🔧 Componentes

### Core Components

#### 1. **SagaEngine**
- Motor principal de orquestração
- Processamento de mensagens assíncronas
- Rastreamento com correlation ID
- Execução de fallback e rollback

#### 2. **DataSanitizer**
- Sanitização de dados de entrada
- Proteção contra SQL Injection
- Proteção contra XSS
- Sanitização de HTML tags

#### 3. **RateLimiter**
- Controle de taxa de requisições
- Buckets configuráveis
- Rate limiting distribuído
- Monitoramento de tokens

#### 4. **CircuitBreakerManager**
- Circuit breaker para chamadas externas
- Estados automáticos (closed, open, half-open)
- Configuração flexível de thresholds
- Métricas de circuit breaker

### Performance Components

#### 1. **ConnectionPoolConfig**
- Pool de conexões MongoDB
- Pool de conexões RabbitMQ
- Health checks automáticos
- Métricas de utilização

#### 2. **SagaCacheManager**
- Cache de configurações
- Cache de metadados de saga
- Cache local com TTL
- Integração com Spring Cache

#### 3. **BatchProcessor**
- Processamento em lote de sagas
- Configuração de batch size
- Retry automático em caso de falha
- Backpressure para controle de carga

### Scalability Components

#### 1. **SagaShardingManager**
- Distribuição de sagas por domínio
- Estratégias de sharding (hash, domínio, round-robin)
- Mapeamento de sagas para shards
- Balanceamento de carga entre shards

#### 2. **WorkerLoadBalancer**
- Balanceamento de carga para workers
- Estratégias de load balancing
- Monitoramento de workers
- Controle de disponibilidade

#### 3. **QueuePartitioningManager**
- Partitioning de filas por tenant
- Isolamento por tenant
- Prefixos de fila configuráveis
- Estratégias de partitioning

### Security Components

#### 1. **SecretsManager**
- Gerenciamento de secrets
- Armazenamento seguro de credenciais
- Integração com configurações do Spring
- APIs para gerenciar secrets dinamicamente

### Monitoring Components

#### 1. **SagaMetrics**
- Métricas customizadas para sagas
- Execuções, erros e fallbacks
- Rate limiting e circuit breaker
- Cache operations e batch processing

## ⚙️ Configuração

### Configurações Principais

```yaml
# Configurações de saga
saga:
  engine:
    max-retries: 3
    timeout: 30s
    batch-size: 100
    enable-circuit-breaker: true
    enable-rate-limiting: true
    enable-sanitization: true
  monitoring:
    enabled: true
    metrics-prefix: "saga"
  rate-limit:
    default-requests: 100
    default-window: 60
  connection-pool:
    mongo:
      max-connections: 100
      min-connections: 5
    rabbit:
      max-connections: 50
      min-connections: 2
  sharding:
    enabled: true
    strategy: HASH_BASED
  load-balancing:
    enabled: true
    strategy: ROUND_ROBIN
  partitioning:
    enabled: true
    strategy: TENANT_BASED
```

### Configurações de Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      saga-engine:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
  ratelimiter:
    instances:
      saga-rate-limiter:
        limit-for-period: 100
        limit-refresh-period: 60s
        timeout-duration: 5s
```

### Configurações de Logging

```yaml
logging:
  level:
    com.saga.orchestration: INFO
    com.saga.orchestration.saga: INFO
    com.saga.orchestration.exception: ERROR
    com.saga.orchestration.security: INFO
    com.saga.orchestration.resilience: INFO
    com.saga.orchestration.batch: INFO
    com.saga.orchestration.sharding: INFO
    com.saga.orchestration.loadbalancer: INFO
    com.saga.orchestration.partitioning: INFO
    com.saga.orchestration.cache: DEBUG
    com.saga.orchestration.performance: DEBUG
```

## 📚 API Reference

### Anotações de Saga

#### @Step
```java
@Step(
    consume = "input-queue",
    produce = "output-queue",
    consumeDTO = InputDTO.class,
    produceDTO = OutputDTO.class
)
public Mono<OutputDTO> processStep(InputDTO input) {
    // Lógica do step
}
```

#### @Fallback
```java
@Fallback(forStep = "processStep")
public Mono<Void> fallbackStep(String input) {
    // Lógica de fallback
}
```

#### @Rollback
```java
@Rollback(forStep = "processStep")
public Mono<Void> rollbackStep(String input) {
    // Lógica de rollback
}
```

### Exemplos de Uso

#### 1. Saga Simples
```java
@Component
public class VendasSaga {
    
    @Step(
        consume = "proposta-emissao-automovel",
        produce = "emitir-proposta-automovel",
        consumeDTO = Notification.class,
        produceDTO = ClienteRequest.class
    )
    public Mono<ClienteRequest> propostaEmissaoAutomovel(Notification input) {
        // Lógica de emissão de proposta
        return Mono.just(cliente);
    }

    @Fallback(forStep = "propostaEmissaoAutomovel")
    public Mono<Void> fallbackProposta(String input) {
        // Lógica de fallback
        return Mono.empty();
    }
}
```

#### 2. Uso de Circuit Breaker
```java
@Autowired
private CircuitBreakerManager circuitBreakerManager;

public Mono<Result> processWithCircuitBreaker(Input input) {
    return circuitBreakerManager.executeWithCircuitBreaker(
        "saga-processor",
        processStep(input)
    );
}
```

#### 3. Uso de Rate Limiter
```java
@Autowired
private RateLimiter rateLimiter;

public Mono<Result> processWithRateLimit(String key, Input input) {
    if (rateLimiter.tryConsume(key)) {
        return processStep(input);
    } else {
        return Mono.error(new RateLimitExceededException());
    }
}
```

## 🚀 Deploy

### Pré-requisitos

- Java 17+
- Docker e Docker Compose
- MongoDB
- RabbitMQ
- Redis (opcional para cache)

### Deploy Local

1. **Clone o repositório**
```bash
git clone <repository-url>
cd SagaOrchestrationJavaWebflux
```

2. **Suba a infraestrutura**
```bash
docker-compose -f docker/docker-compose.yml up -d
```

3. **Execute a aplicação**
```bash
./mvnw spring-boot:run
```

### Deploy em Produção

1. **Build da aplicação**
```bash
./mvnw clean package -DskipTests
```

2. **Configuração de ambiente**
```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATA_MONGODB_URI=mongodb://prod-mongo:27017/sagadb
export SPRING_RABBITMQ_HOST=prod-rabbitmq
```

3. **Execução**
```bash
java -jar target/orchestration-0.0.1-SNAPSHOT.jar
```

## 📊 Monitoramento

### Endpoints de Monitoramento

- **Health Check**: `http://localhost:8080/actuator/health`
- **Métricas**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`
- **AsyncAPI**: `http://localhost:8080/asyncapi-ui/index.html`

### Métricas Principais

#### Saga Metrics
- `saga.executions` - Contador de execuções de saga
- `saga.execution.duration` - Timer de duração de execução
- `saga.errors` - Contador de erros
- `saga.fallbacks` - Contador de fallbacks
- `saga.rollbacks` - Contador de rollbacks

#### Performance Metrics
- `saga.rate_limit.exceeded` - Rate limit exceeded
- `saga.circuit_breaker.state_change` - Circuit breaker state changes
- `saga.cache.operations` - Cache hit/miss
- `saga.batch.processing` - Batch processing metrics

#### Infrastructure Metrics
- `saga.sharding.operations` - Sharding operations
- `saga.load_balancing.operations` - Load balancing operations
- `saga.partitioning.operations` - Partitioning operations

### Dashboards Recomendados

1. **Saga Overview Dashboard**
   - Execuções por minuto
   - Taxa de sucesso
   - Duração média de execução
   - Erros por tipo

2. **Performance Dashboard**
   - Rate limiting stats
   - Circuit breaker states
   - Cache hit ratio
   - Batch processing stats

3. **Infrastructure Dashboard**
   - Connection pool utilization
   - Worker load distribution
   - Shard distribution
   - Partition utilization

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. **Saga não executa**
```bash
# Verificar logs
tail -f logs/saga-executions.json

# Verificar health check
curl http://localhost:8080/actuator/health

# Verificar RabbitMQ
curl -u guest:guest http://localhost:15672/api/queues
```

#### 2. **Rate limit exceeded**
```bash
# Verificar configuração
curl http://localhost:8080/actuator/metrics/saga.rate_limit.exceeded

# Ajustar configuração
saga.rate-limit.default-requests: 200
```

#### 3. **Circuit breaker open**
```bash
# Verificar estado
curl http://localhost:8080/actuator/metrics/saga.circuit_breaker.state_change

# Verificar configuração
resilience4j.circuitbreaker.instances.saga-engine.failure-rate-threshold: 70
```

#### 4. **Performance issues**
```bash
# Verificar métricas de performance
curl http://localhost:8080/actuator/metrics/saga.execution.duration

# Verificar connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### Logs de Debug

```bash
# Ativar logs de debug
logging.level.com.saga.orchestration.saga=DEBUG
logging.level.com.saga.orchestration.performance=DEBUG

# Verificar logs estruturados
tail -f logs/saga-orchestration.json | jq
```

## 👨‍💻 Desenvolvimento

### Estrutura do Projeto

```
src/main/java/com/saga/orchestration/
├── saga/                    # Motor de saga
├── service/                 # Serviços de negócio
├── model/                   # Modelos de dados
├── repository/              # Repositórios
├── dto/                     # DTOs
├── annotations/             # Anotações customizadas
├── config/                  # Configurações
├── security/                # Segurança
├── exception/               # Exceções
├── cache/                   # Cache
├── resilience/              # Circuit breaker
├── batch/                   # Batch processing
├── sharding/                # Sharding
├── loadbalancer/            # Load balancing
├── partitioning/            # Partitioning
└── metrics/                 # Métricas
```

### Convenções de Código

#### 1. **Nomenclatura**
- Classes: PascalCase
- Métodos: camelCase
- Constantes: UPPER_SNAKE_CASE
- Pacotes: lowercase

#### 2. **Anotações**
- `@Component` para serviços
- `@Configuration` para configurações
- `@Slf4j` para logging
- `@Value` para propriedades

#### 3. **Tratamento de Erros**
- Usar hierarquia de exceções
- Incluir correlation ID
- Logar contexto completo
- Implementar fallback/rollback

#### 4. **Métricas**
- Registrar todas as operações importantes
- Usar tags para categorização
- Manter consistência nos nomes

### Testes

#### 1. **Testes Unitários**
```java
@ExtendWith(MockitoExtension.class)
class SagaEngineTest {
    
    @Test
    @DisplayName("given valid saga step when processing should execute successfully")
    void shouldProcessSagaStepSuccessfully() {
        // Test implementation
    }
}
```

#### 2. **Testes de Integração**
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/test",
    "spring.rabbitmq.host=localhost"
})
class SagaIntegrationTest {
    // Test implementation
}
```

#### 3. **Testes de Performance**
```java
@Test
@DisplayName("should process 1000 sagas in under 30 seconds")
void shouldProcessBatchEfficiently() {
    // Performance test implementation
}
```

### Contribuição

1. **Fork o repositório**
2. **Crie uma branch feature**
3. **Implemente as mudanças**
4. **Adicione testes**
5. **Atualize documentação**
6. **Crie pull request**

### Checklist de Qualidade

- [ ] Código segue convenções
- [ ] Testes unitários implementados
- [ ] Testes de integração implementados
- [ ] Documentação atualizada
- [ ] Métricas implementadas
- [ ] Logs estruturados
- [ ] Tratamento de erros robusto
- [ ] Configuração externa
- [ ] Performance otimizada

---

**Versão**: 1.0.0  
**Última atualização**: Dezembro 2024  
**Autor**: Tiago Ventura  
**Licença**: Apache 2.0 