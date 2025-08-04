# Deploy e Monitoramento do Saga Orchestration

## 🚀 Deploy

### Pré-requisitos

#### **Sistema**
- **Java**: 17 ou superior
- **Maven**: 3.6+ ou Gradle 7+
- **Docker**: 20.10+ e Docker Compose 2.0+
- **Memória**: Mínimo 2GB RAM disponível
- **Disco**: Mínimo 10GB espaço livre

#### **Infraestrutura**
- **MongoDB**: 4.4+ (recomendado 5.0+)
- **RabbitMQ**: 3.8+ (recomendado 3.12+)
- **Redis**: 6.0+ (opcional para cache)
- **Prometheus**: 2.30+ (para métricas)
- **Grafana**: 8.0+ (para dashboards)

### Deploy Local

#### 1. **Clone e Setup**

```bash
# Clone o repositório
git clone <repository-url>
cd SagaOrchestrationJavaWebflux

# Verifique a versão do Java
java -version

# Verifique o Maven
mvn -version
```

#### 2. **Infraestrutura com Docker**

```bash
# Suba a infraestrutura completa
docker-compose -f docker/docker-compose.yml up -d

# Verifique se os serviços estão rodando
docker-compose -f docker/docker-compose.yml ps

# Logs dos serviços
docker-compose -f docker/docker-compose.yml logs -f
```

#### 3. **Build e Execução**

```bash
# Build do projeto
./mvnw clean package -DskipTests

# Execução
./mvnw spring-boot:run

# Ou execute o JAR
java -jar target/orchestration-0.0.1-SNAPSHOT.jar
```

#### 4. **Verificação**

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# AsyncAPI UI
open http://localhost:8080/asyncapi-ui/index.html
```

### Deploy em Produção

#### 1. **Preparação do Ambiente**

```bash
# Variáveis de ambiente
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATA_MONGODB_URI=mongodb://prod-mongo:27017/sagadb
export SPRING_RABBITMQ_HOST=prod-rabbitmq
export SPRING_RABBITMQ_USERNAME=${RABBITMQ_USERNAME}
export SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}
export SAGA_METRICS_ENABLED=true
export SAGA_MONITORING_ENABLED=true
```

#### 2. **Build de Produção**

```bash
# Build otimizado para produção
./mvnw clean package -DskipTests -Pprod

# Verificação do JAR
java -jar target/orchestration-0.0.1-SNAPSHOT.jar --version
```

#### 3. **Execução em Produção**

```bash
# Execução com configurações de produção
java -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  -jar target/orchestration-0.0.1-SNAPSHOT.jar
```

#### 4. **Systemd Service (Linux)**

```ini
# /etc/systemd/system/saga-orchestration.service
[Unit]
Description=Saga Orchestration Service
After=network.target

[Service]
Type=simple
User=saga
ExecStart=/usr/bin/java -Xms1g -Xmx2g -jar /opt/saga/orchestration.jar
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=SPRING_DATA_MONGODB_URI=mongodb://prod-mongo:27017/sagadb
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Ativar e iniciar o serviço
sudo systemctl daemon-reload
sudo systemctl enable saga-orchestration
sudo systemctl start saga-orchestration
sudo systemctl status saga-orchestration
```

### Deploy com Docker

#### 1. **Dockerfile**

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar dependências
COPY target/lib/ lib/
COPY target/orchestration-0.0.1-SNAPSHOT.jar app.jar

# Configurações JVM
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 2. **Docker Compose para Produção**

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  saga-orchestration:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/sagadb
      - SPRING_RABBITMQ_HOST=rabbitmq
    depends_on:
      - mongo
      - rabbitmq
      - redis
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'

  mongo:
    image: mongo:5.0
    environment:
      - MONGO_INITDB_DATABASE=sagadb
    volumes:
      - mongo-data:/data/db
    ports:
      - "27017:27017"

  rabbitmq:
    image: rabbitmq:3.12-management
    environment:
      - RABBITMQ_DEFAULT_USER=admin
      - RABBITMQ_DEFAULT_PASS=admin123
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - grafana-data:/var/lib/grafana

volumes:
  mongo-data:
  rabbitmq-data:
  redis-data:
  grafana-data:
```

#### 3. **Deploy com Docker**

```bash
# Build da imagem
docker build -t saga-orchestration:latest .

# Deploy com docker-compose
docker-compose -f docker-compose.prod.yml up -d

# Verificar status
docker-compose -f docker-compose.prod.yml ps

# Logs
docker-compose -f docker-compose.prod.yml logs -f saga-orchestration
```

### Deploy em Kubernetes

#### 1. **ConfigMap**

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: saga-orchestration-config
data:
  application.yml: |
    spring:
      application:
        name: orchestration
      data:
        mongodb:
          uri: mongodb://mongo:27017/sagadb
      rabbitmq:
        host: rabbitmq
        port: 5672
    saga:
      engine:
        max-retries: 3
        timeout: 30s
        batch-size: 100
      monitoring:
        enabled: true
        metrics-prefix: "saga"
```

#### 2. **Secret**

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: saga-orchestration-secret
type: Opaque
data:
  rabbitmq-password: YWRtaW4xMjM=  # admin123 em base64
  mongodb-password: YWRtaW4xMjM=   # admin123 em base64
```

#### 3. **Deployment**

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: saga-orchestration
  labels:
    app: saga-orchestration
spec:
  replicas: 3
  selector:
    matchLabels:
      app: saga-orchestration
  template:
    metadata:
      labels:
        app: saga-orchestration
    spec:
      containers:
      - name: saga-orchestration
        image: saga-orchestration:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: saga-orchestration-secret
              key: rabbitmq-password
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: config-volume
        configMap:
          name: saga-orchestration-config
```

#### 4. **Service**

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: saga-orchestration-service
spec:
  selector:
    app: saga-orchestration
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer
```

#### 5. **Deploy no Kubernetes**

```bash
# Aplicar configurações
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml

# Verificar status
kubectl get pods
kubectl get services
kubectl logs -f deployment/saga-orchestration
```

## 📊 Monitoramento

### Endpoints de Monitoramento

#### **Health Checks**
```bash
# Health check geral
curl http://localhost:8080/actuator/health

# Health check detalhado
curl http://localhost:8080/actuator/health -H "Accept: application/json"

# Health check específico
curl http://localhost:8080/actuator/health/mongo
curl http://localhost:8080/actuator/health/rabbit
```

#### **Métricas**
```bash
# Métricas gerais
curl http://localhost:8080/actuator/metrics

# Métricas específicas
curl http://localhost:8080/actuator/metrics/saga.executions
curl http://localhost:8080/actuator/metrics/saga.execution.duration
curl http://localhost:8080/actuator/metrics/saga.errors

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

#### **Info e Configuração**
```bash
# Informações da aplicação
curl http://localhost:8080/actuator/info

# Configurações
curl http://localhost:8080/actuator/configprops

# Environment
curl http://localhost:8080/actuator/env
```

### Dashboards do Grafana

#### 1. **Saga Overview Dashboard**

```json
{
  "dashboard": {
    "title": "Saga Orchestration Overview",
    "panels": [
      {
        "title": "Saga Executions per Minute",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(saga_executions_total[5m])",
            "legendFormat": "{{saga_type}}"
          }
        ]
      },
      {
        "title": "Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "saga_executions_total{status=\"success\"} / saga_executions_total",
            "legendFormat": "Success Rate"
          }
        ]
      },
      {
        "title": "Average Execution Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(saga_execution_duration_seconds_bucket[5m]))",
            "legendFormat": "95th Percentile"
          }
        ]
      }
    ]
  }
}
```

#### 2. **Performance Dashboard**

```json
{
  "dashboard": {
    "title": "Performance Metrics",
    "panels": [
      {
        "title": "Rate Limiting",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(saga_rate_limit_exceeded_total[5m])",
            "legendFormat": "Rate Limit Exceeded"
          }
        ]
      },
      {
        "title": "Circuit Breaker States",
        "type": "stat",
        "targets": [
          {
            "expr": "saga_circuit_breaker_state_change_total",
            "legendFormat": "State Changes"
          }
        ]
      },
      {
        "title": "Cache Hit Ratio",
        "type": "graph",
        "targets": [
          {
            "expr": "saga_cache_operations_total{result=\"hit\"} / saga_cache_operations_total",
            "legendFormat": "Cache Hit Ratio"
          }
        ]
      }
    ]
  }
}
```

#### 3. **Infrastructure Dashboard**

```json
{
  "dashboard": {
    "title": "Infrastructure Metrics",
    "panels": [
      {
        "title": "Connection Pool Utilization",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active / hikaricp_connections_max",
            "legendFormat": "MongoDB Pool"
          }
        ]
      },
      {
        "title": "Worker Load Distribution",
        "type": "graph",
        "targets": [
          {
            "expr": "saga_load_balancing_operations_total",
            "legendFormat": "{{worker_id}}"
          }
        ]
      },
      {
        "title": "Shard Distribution",
        "type": "pie",
        "targets": [
          {
            "expr": "saga_sharding_operations_total",
            "legendFormat": "{{shard_id}}"
          }
        ]
      }
    ]
  }
}
```

### Alertas

#### 1. **Alertas de Performance**

```yaml
# prometheus-alerts.yaml
groups:
- name: saga-orchestration
  rules:
  - alert: HighErrorRate
    expr: rate(saga_errors_total[5m]) > 0.1
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"

  - alert: CircuitBreakerOpen
    expr: saga_circuit_breaker_state_change_total{to_state="OPEN"} > 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Circuit breaker is open"
      description: "Circuit breaker {{ $labels.circuit_breaker }} is open"

  - alert: HighLatency
    expr: histogram_quantile(0.95, rate(saga_execution_duration_seconds_bucket[5m])) > 30
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High latency detected"
      description: "95th percentile latency is {{ $value }} seconds"
```

#### 2. **Alertas de Infraestrutura**

```yaml
  - alert: HighMemoryUsage
    expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High memory usage"
      description: "Memory usage is {{ $value | humanizePercentage }}"

  - alert: HighCPUUsage
    expr: rate(process_cpu_seconds_total[5m]) > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High CPU usage"
      description: "CPU usage is {{ $value | humanizePercentage }}"

  - alert: DatabaseConnectionPoolExhausted
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "Database connection pool exhausted"
      description: "Connection pool usage is {{ $value | humanizePercentage }}"
```

### Logs

#### 1. **Configuração de Logs**

```yaml
# logback-spring.xml
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <loggerName/>
        <message/>
        <mdc/>
        <stackTrace/>
      </providers>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/saga-orchestration.json</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>logs/saga-orchestration.%d{yyyy-MM-dd}.json</fileNamePattern>
      <maxHistory>30</maxHistory>
      <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <loggerName/>
        <message/>
        <mdc/>
        <stackTrace/>
      </providers>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>
</configuration>
```

#### 2. **Análise de Logs**

```bash
# Logs em tempo real
tail -f logs/saga-orchestration.json | jq

# Logs de erro
tail -f logs/saga-orchestration.json | jq 'select(.level == "ERROR")'

# Logs por correlation ID
tail -f logs/saga-orchestration.json | jq 'select(.correlationId == "abc123")'

# Logs de performance
tail -f logs/saga-orchestration.json | jq 'select(.logger | contains("performance"))'
```

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. **Aplicação não inicia**

```bash
# Verificar logs de inicialização
tail -f logs/saga-orchestration.json | jq 'select(.logger | contains("Application"))'

# Verificar configuração
curl http://localhost:8080/actuator/configprops

# Verificar health checks
curl http://localhost:8080/actuator/health
```

#### 2. **Conexão com MongoDB falha**

```bash
# Verificar conectividade
telnet localhost 27017

# Verificar logs do MongoDB
docker logs mongo

# Verificar configuração
curl http://localhost:8080/actuator/health/mongo
```

#### 3. **Conexão com RabbitMQ falha**

```bash
# Verificar conectividade
telnet localhost 5672

# Verificar logs do RabbitMQ
docker logs rabbitmq

# Verificar configuração
curl http://localhost:8080/actuator/health/rabbit
```

#### 4. **Performance degradada**

```bash
# Verificar métricas de performance
curl http://localhost:8080/actuator/metrics/saga.execution.duration

# Verificar uso de memória
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Verificar connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### Comandos Úteis

#### **Debug**

```bash
# Ativar logs de debug
export LOGGING_LEVEL_COM_SAGA_ORCHESTRATION=DEBUG

# Verificar configuração ativa
curl http://localhost:8080/actuator/env

# Verificar beans carregados
curl http://localhost:8080/actuator/beans
```

#### **Performance**

```bash
# Profiling com JFR
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr -jar app.jar

# Heap dump
jmap -dump:format=b,file=heap.hprof <pid>

# Thread dump
jstack <pid> > thread-dump.txt
```

#### **Monitoramento**

```bash
# Métricas em tempo real
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/saga.executions'

# Health check contínuo
watch -n 5 'curl -s http://localhost:8080/actuator/health'

# Logs filtrados
tail -f logs/saga-orchestration.json | jq 'select(.level == "ERROR" or .level == "WARN")'
```

---

**Versão**: 1.0.0  
**Última atualização**: Dezembro 2024  
**Autor**: Tiago Ventura 