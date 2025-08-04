package com.saga.orchestration.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * Sistema de métricas customizadas para sagas
 * Segue o princípio SRP - responsabilidade única de gerenciar métricas
 */
@Slf4j
@Component
public class SagaMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> sagaCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> sagaTimers = new ConcurrentHashMap<>();

    public SagaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Registra execução de saga
     */
    public void recordSagaExecution(String sagaType, Duration duration, boolean success) {
        String counterName = "saga.executions";
        String timerName = "saga.execution.duration";
        
        // Contador de execuções
        Counter counter = sagaCounters.computeIfAbsent(counterName, 
            name -> Counter.builder(name)
                .tag("saga_type", sagaType)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));
        
        counter.increment();
        
        // Timer de duração
        Timer timer = sagaTimers.computeIfAbsent(timerName,
            name -> Timer.builder(name)
                .tag("saga_type", sagaType)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry));
        
        timer.record(duration);
        
        log.debug("Recorded saga execution: type={}, duration={}, success={}", 
            sagaType, duration, success);
    }

    /**
     * Registra erro de saga
     */
    public void recordSagaError(String sagaType, String errorType) {
        String counterName = "saga.errors";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            counterNameKey -> Counter.builder(counterNameKey)
                .tag("saga_type", sagaType)
                .tag("error_type", errorType)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded saga error: type={}, error={}", sagaType, errorType);
    }

    /**
     * Registra fallback de saga
     */
    public void recordSagaFallback(String sagaType, String stepName) {
        String counterName = "saga.fallbacks";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("saga_type", sagaType)
                .tag("step_name", stepName)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded saga fallback: type={}, step={}", sagaType, stepName);
    }

    /**
     * Registra rollback de saga
     */
    public void recordSagaRollback(String sagaType, String stepName) {
        String counterName = "saga.rollbacks";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("saga_type", sagaType)
                .tag("step_name", stepName)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded saga rollback: type={}, step={}", sagaType, stepName);
    }

    /**
     * Registra rate limit exceeded
     */
    public void recordRateLimitExceeded(String key) {
        String counterName = "saga.rate_limit.exceeded";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("key", key)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded rate limit exceeded: key={}", key);
    }

    /**
     * Registra circuit breaker state change
     */
    public void recordCircuitBreakerStateChange(String name, String fromState, String toState) {
        String counterName = "saga.circuit_breaker.state_change";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            counterNameKey -> Counter.builder(counterNameKey)
                .tag("circuit_breaker", name)
                .tag("from_state", fromState)
                .tag("to_state", toState)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded circuit breaker state change: name={}, from={}, to={}", 
            name, fromState, toState);
    }

    /**
     * Registra cache hit/miss
     */
    public void recordCacheOperation(String cacheName, String operation, boolean hit) {
        String counterName = "saga.cache.operations";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("cache_name", cacheName)
                .tag("operation", operation)
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded cache operation: cache={}, operation={}, hit={}", 
            cacheName, operation, hit);
    }

    /**
     * Registra batch processing
     */
    public void recordBatchProcessing(String batchType, int totalItems, int processedItems, Duration duration) {
        String counterName = "saga.batch.processing";
        String timerName = "saga.batch.duration";
        
        // Contador de itens processados
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            counterNameKey -> Counter.builder(counterNameKey)
                .tag("batch_type", batchType)
                .register(meterRegistry));
        
        counter.increment(processedItems);
        
        // Timer de duração
        Timer timer = sagaTimers.computeIfAbsent(timerName,
            name -> Timer.builder(name)
                .tag("batch_type", batchType)
                .register(meterRegistry));
        
        timer.record(duration);
        
        log.debug("Recorded batch processing: type={}, total={}, processed={}, duration={}", 
            batchType, totalItems, processedItems, duration);
    }

    /**
     * Registra sharding operation
     */
    public void recordShardingOperation(String operation, String shardId) {
        String counterName = "saga.sharding.operations";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("operation", operation)
                .tag("shard_id", shardId)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded sharding operation: operation={}, shard={}", operation, shardId);
    }

    /**
     * Registra load balancing operation
     */
    public void recordLoadBalancingOperation(String operation, String workerId) {
        String counterName = "saga.load_balancing.operations";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("operation", operation)
                .tag("worker_id", workerId)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded load balancing operation: operation={}, worker={}", operation, workerId);
    }

    /**
     * Registra partitioning operation
     */
    public void recordPartitioningOperation(String operation, String partitionId) {
        String counterName = "saga.partitioning.operations";
        
        Counter counter = sagaCounters.computeIfAbsent(counterName,
            name -> Counter.builder(name)
                .tag("operation", operation)
                .tag("partition_id", partitionId)
                .register(meterRegistry));
        
        counter.increment();
        
        log.debug("Recorded partitioning operation: operation={}, partition={}", operation, partitionId);
    }

    /**
     * Obtém estatísticas de métricas
     */
    public MetricsStats getMetricsStats() {
        return new MetricsStats(
            sagaCounters.size(),
            sagaTimers.size(),
            meterRegistry.getMeters().size()
        );
    }

    /**
     * Estatísticas de métricas
     */
    public static class MetricsStats {
        private final int counterCount;
        private final int timerCount;
        private final int totalMeters;

        public MetricsStats(int counterCount, int timerCount, int totalMeters) {
            this.counterCount = counterCount;
            this.timerCount = timerCount;
            this.totalMeters = totalMeters;
        }

        public int getCounterCount() {
            return counterCount;
        }

        public int getTimerCount() {
            return timerCount;
        }

        public int getTotalMeters() {
            return totalMeters;
        }
    }
} 