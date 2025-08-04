# Configuração do Saga Orchestration

## ⚙️ Visão Geral

O Saga Orchestration possui um sistema de configuração flexível e extensível, permitindo ajustes para diferentes ambientes e necessidades específicas.

## 📁 Estrutura de Configuração

### Arquivos de Configuração

```
src/main/resources/
├── application.yml              # Configuração principal
├── application-dev.yml          # Configuração de desenvolvimento
├── application-prod.yml         # Configuração de produção
├── application-test.yml         # Configuração de testes
└── logback-spring.xml          # Configuração de logs
```

## 🔧 Configurações Principais

### 1. **Configurações de Saga**

```yaml
saga:
  engine:
    max-retries: 3                    # Número máximo de tentativas
    timeout: 30s                      # Timeout de execução
    batch-size: 100                   # Tamanho do lote
    enable-circuit-breaker: true      # Habilitar circuit breaker
    enable-rate-limiting: true        # Habilitar rate limiting
    enable-sanitization: true         # Habilitar sanitização
  monitoring:
    enabled: true                     # Habilitar monitoramento
    metrics-prefix: "saga"           # Prefixo das métricas
  rate-limit:
    default-requests: 100             # Requisições padrão por janela
    default-window: 60                # Janela de tempo em segundos
  connection-pool:
    mongo:
      max-connections: 100            # Máximo de conexões MongoDB
      min-connections: 5              # Mínimo de conexões MongoDB
    rabbit:
      max-connections: 50             # Máximo de conexões RabbitMQ
      min-connections: 2              # Mínimo de conexões RabbitMQ
  sharding:
    enabled: true                     # Habilitar sharding
    strategy: HASH_BASED              # Estratégia de sharding
  load-balancing:
    enabled: true                     # Habilitar load balancing
    strategy: ROUND_ROBIN             # Estratégia de load balancing
  partitioning:
    enabled: true                     # Habilitar partitioning
    strategy: TENANT_BASED            # Estratégia de partitioning
```

### 2. **Configurações do Spring Boot**

```yaml
spring:
  application:
    name: orchestration               # Nome da aplicação
  data:
    mongodb:
      uri: mongodb://localhost:27017/sagadb  # URI do MongoDB
    redis:
      host: localhost                 # Host do Redis
      port: 6379                      # Porta do Redis
      timeout: 2000ms                 # Timeout do Redis
  rabbitmq: 
    host: localhost                   # Host do RabbitMQ
    port: 5672                        # Porta do RabbitMQ
    username: guest                   # Usuário do RabbitMQ
    password: guest                   # Senha do RabbitMQ
  cache:
    type: redis                       # Tipo de cache
    redis:
      time-to-live: 600000            # TTL do cache (10 minutos)
      cache-null-values: false        # Não cachear valores nulos
  security:
    user:
      name: admin                     # Usuário de segurança
      password: admin123              # Senha de segurança
```

### 3. **Configurações do Resilience4j**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      saga-engine:
        sliding-window-size: 10       # Tamanho da janela deslizante
        minimum-number-of-calls: 5    # Número mínimo de chamadas
        failure-rate-threshold: 50    # Threshold de taxa de falha (%)
        wait-duration-in-open-state: 60s  # Duração em estado aberto
        permitted-number-of-calls-in-half-open-state: 3  # Chamadas em half-open
  ratelimiter:
    instances:
      saga-rate-limiter:
        limit-for-period: 100         # Limite por período
        limit-refresh-period: 60s     # Período de refresh
        timeout-duration: 5s          # Timeout de duração
```

### 4. **Configurações do Actuator**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # Endpoints expostos
  endpoint:
    health:
      show-details: always            # Mostrar detalhes do health check
  metrics:
    export:
      prometheus:
        enabled: true                 # Habilitar exportação Prometheus
```

### 5. **Configurações do Springwolf**

```yaml
springwolf:
  docket:
    info:
      title: Saga Orchestration API   # Título da API
      version: '1.0.0'               # Versão da API
      description: AsyncAPI docs for saga orchestration  # Descrição
    base-package: com.saga.orchestration  # Pacote base
    servers:
      amqp:
        protocol: amqp                # Protocolo AMQP
        host: localhost:5672          # Host do servidor
  plugin:
    amqp:
      publishing: 
        enabled: true                 # Habilitar publicação
```

### 6. **Configurações de Logging**

```yaml
logging:
  level:
    com.saga.orchestration: INFO     # Log level geral
    com.saga.orchestration.saga: INFO     # Log level do motor de saga
    com.saga.orchestration.exception: ERROR  # Log level de exceções
    com.saga.orchestration.security: INFO   # Log level de segurança
    com.saga.orchestration.resilience: INFO # Log level de resiliência
    com.saga.orchestration.batch: INFO      # Log level de batch
    com.saga.orchestration.sharding: INFO   # Log level de sharding
    com.saga.orchestration.loadbalancer: INFO  # Log level de load balancing
    com.saga.orchestration.partitioning: INFO  # Log level de partitioning
    com.saga.orchestration.cache: DEBUG      # Log level de cache
    com.saga.orchestration.performance: DEBUG  # Log level de performance
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🌍 Configurações por Ambiente

### 1. **Desenvolvimento (application-dev.yml)**

```yaml
spring:
  profiles:
    active: dev
  data:
    mongodb:
      uri: mongodb://localhost:27017/sagadb-dev
  rabbitmq:
    host: localhost
    port: 5672

saga:
  engine:
    max-retries: 1
    timeout: 10s
    batch-size: 10
  rate-limit:
    default-requests: 50
    default-window: 30

logging:
  level:
    com.saga.orchestration: DEBUG
```

### 2. **Produção (application-prod.yml)**

```yaml
spring:
  profiles:
    active: prod
  data:
    mongodb:
      uri: mongodb://prod-mongo:27017/sagadb-prod
  rabbitmq:
    host: prod-rabbitmq
    port: 5672
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

saga:
  engine:
    max-retries: 5
    timeout: 60s
    batch-size: 500
  rate-limit:
    default-requests: 1000
    default-window: 60

logging:
  level:
    com.saga.orchestration: WARN
```

### 3. **Testes (application-test.yml)**

```yaml
spring:
  profiles:
    active: test
  data:
    mongodb:
      uri: mongodb://localhost:27017/sagadb-test
  rabbitmq:
    host: localhost
    port: 5672

saga:
  engine:
    max-retries: 1
    timeout: 5s
    batch-size: 5
  rate-limit:
    default-requests: 10
    default-window: 10

logging:
  level:
    com.saga.orchestration: ERROR
```

## 🔐 Configurações de Segurança

### 1. **Secrets Management**

```yaml
# Configuração de secrets via variáveis de ambiente
spring:
  rabbitmq:
    password: ${RABBITMQ_PASSWORD}
  data:
    mongodb:
      uri: ${MONGODB_URI}

# Configuração de secrets customizados
saga:
  secrets:
    api-key: ${API_KEY}
    encryption-key: ${ENCRYPTION_KEY}
```

### 2. **Rate Limiting por Tenant**

```yaml
saga:
  rate-limit:
    tenants:
      tenant1:
        requests: 1000
        window: 60
      tenant2:
        requests: 500
        window: 60
      default:
        requests: 100
        window: 60
```

### 3. **Circuit Breaker por Serviço**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      saga-engine:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      external-service:
        failure-rate-threshold: 30
        wait-duration-in-open-state: 30s
      database-service:
        failure-rate-threshold: 20
        wait-duration-in-open-state: 120s
```

## 📊 Configurações de Monitoramento

### 1. **Métricas Customizadas**

```yaml
saga:
  metrics:
    enabled: true
    prefix: "saga"
    tags:
      environment: ${ENVIRONMENT:dev}
      version: ${APP_VERSION:1.0.0}
    custom:
      saga-execution-duration:
        buckets: [0.1, 0.5, 1.0, 2.0, 5.0]
      saga-batch-size:
        buckets: [10, 50, 100, 500, 1000]
```

### 2. **Health Checks**

```yaml
management:
  health:
    defaults:
      enabled: true
    mongo:
      enabled: true
    rabbit:
      enabled: true
    redis:
      enabled: true
    custom:
      saga-engine:
        enabled: true
        timeout: 10s
```

### 3. **Logs Estruturados**

```yaml
logging:
  pattern:
    json: '{"timestamp":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%level","logger":"%logger{36}","message":"%msg","correlationId":"%X{correlationId:-}","sagaId":"%X{sagaId:-}"}'
  file:
    name: logs/saga-orchestration.json
    max-size: 100MB
    max-history: 30
```

## 🚀 Configurações de Performance

### 1. **Connection Pooling**

```yaml
saga:
  connection-pool:
    mongo:
      max-connections: 100
      min-connections: 5
      max-wait-time: 30000ms
      max-connection-idle-time: 60000ms
      max-connection-life-time: 300000ms
    rabbit:
      max-connections: 50
      min-connections: 2
      connection-timeout: 10000ms
      handshake-timeout: 10000ms
```

### 2. **Cache Configuration**

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000
      cache-null-values: false
      key-prefix: "saga:"
    caffeine:
      spec: "maximumSize=500,expireAfterWrite=600s"
```

### 3. **Batch Processing**

```yaml
saga:
  batch:
    enabled: true
    size: 100
    timeout: 30s
    concurrency: 5
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000ms
        multiplier: 2.0
        max-interval: 10000ms
```

## 🔧 Configurações Avançadas

### 1. **Sharding Configuration**

```yaml
saga:
  sharding:
    enabled: true
    strategy: HASH_BASED
    shards:
      - id: "shard-1"
        domain: "vendas"
        queue-prefix: "vendas"
      - id: "shard-2"
        domain: "financeiro"
        queue-prefix: "fin"
      - id: "shard-3"
        domain: "logistica"
        queue-prefix: "log"
```

### 2. **Load Balancing Configuration**

```yaml
saga:
  load-balancing:
    enabled: true
    strategy: ROUND_ROBIN
    workers:
      - id: "worker-1"
        host: "worker1.example.com"
        port: 8080
        max-concurrency: 100
      - id: "worker-2"
        host: "worker2.example.com"
        port: 8080
        max-concurrency: 100
```

### 3. **Partitioning Configuration**

```yaml
saga:
  partitioning:
    enabled: true
    strategy: TENANT_BASED
    partitions:
      - id: "partition-1"
        tenant-id: "tenant1"
        queue-prefix: "t1"
      - id: "partition-2"
        tenant-id: "tenant2"
        queue-prefix: "t2"
```

## 🔍 Validação de Configuração

### 1. **Verificação de Configuração**

```bash
# Verificar configuração ativa
curl http://localhost:8080/actuator/configprops

# Verificar health checks
curl http://localhost:8080/actuator/health

# Verificar métricas
curl http://localhost:8080/actuator/metrics
```

### 2. **Logs de Configuração**

```bash
# Ativar logs de configuração
logging.level.org.springframework.boot.autoconfigure=DEBUG

# Verificar logs de inicialização
tail -f logs/saga-orchestration.json | jq 'select(.logger | contains("Configuration"))'
```

## 📝 Exemplos de Configuração

### 1. **Configuração Mínima**

```yaml
spring:
  application:
    name: orchestration
  data:
    mongodb:
      uri: mongodb://localhost:27017/sagadb
  rabbitmq:
    host: localhost
    port: 5672

saga:
  engine:
    max-retries: 3
    timeout: 30s
```

### 2. **Configuração Completa**

```yaml
spring:
  application:
    name: orchestration
  data:
    mongodb:
      uri: mongodb://localhost:27017/sagadb
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
  cache:
    type: redis
  security:
    user:
      name: admin
      password: admin123

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

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

springwolf:
  docket:
    info:
      title: Saga Orchestration API
      version: '1.0.0'
      description: AsyncAPI docs for saga orchestration
    base-package: com.saga.orchestration
    servers:
      amqp:
        protocol: amqp
        host: localhost:5672
  plugin:
    amqp:
      publishing: 
        enabled: true

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

---

**Versão**: 1.0.0  
**Última atualização**: Dezembro 2024  
**Autor**: Tiago Ventura 