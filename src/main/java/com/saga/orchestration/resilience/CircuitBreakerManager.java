package com.saga.orchestration.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de circuit breaker para chamadas externas
 * Segue o princípio SRP - responsabilidade única de gerenciar circuit breakers
 */
@Slf4j
@Component
public class CircuitBreakerManager {

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Executa uma operação com circuit breaker
     */
    public <T> Mono<T> executeWithCircuitBreaker(String name, Mono<T> operation) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(name);
        return operation.transform(CircuitBreakerOperator.of(circuitBreaker))
            .doOnSuccess(result -> log.debug("Circuit breaker {} executed successfully", name))
            .doOnError(error -> log.warn("Circuit breaker {} failed with error: {}", name, error.getMessage()));
    }

    /**
     * Obtém ou cria um circuit breaker
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name, this::createCircuitBreaker);
    }

    /**
     * Cria um circuit breaker com configuração padrão
     */
    private CircuitBreaker createCircuitBreaker(String name) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f) // 50% de falhas
            .waitDurationInOpenState(Duration.ofSeconds(60)) // 60s em estado aberto
            .slidingWindowSize(10) // 10 chamadas para calcular taxa de falha
            .minimumNumberOfCalls(5) // Mínimo de 5 chamadas antes de abrir
            .permittedNumberOfCallsInHalfOpenState(3) // 3 chamadas em half-open
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
        
        // Event listeners
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("Circuit breaker {} state changed from {} to {}", 
                name, event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
        
        return circuitBreaker;
    }

    /**
     * Cria um circuit breaker com configuração customizada
     */
    public CircuitBreaker createCustomCircuitBreaker(String name, CircuitBreakerConfig config) {
        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
        circuitBreakers.put(name, circuitBreaker);
        return circuitBreaker;
    }

    /**
     * Obtém estatísticas do circuit breaker
     */
    public CircuitBreakerStats getCircuitBreakerStats(String name) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(name);
        if (circuitBreaker == null) {
            return null;
        }

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        return CircuitBreakerStats.builder()
            .name(name)
            .state(circuitBreaker.getState())
            .failureRate(metrics.getFailureRate())
            .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
            .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
            .numberOfNotPermittedCalls(metrics.getNumberOfNotPermittedCalls())
            .build();
    }

    /**
     * Remove um circuit breaker
     */
    public void removeCircuitBreaker(String name) {
        CircuitBreaker circuitBreaker = circuitBreakers.remove(name);
        if (circuitBreaker != null) {
            log.info("Removed circuit breaker: {}", name);
        }
    }

    /**
     * Estatísticas do circuit breaker
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final CircuitBreaker.State state;
        private final float failureRate;
        private final long numberOfFailedCalls;
        private final long numberOfSuccessfulCalls;
        private final long numberOfNotPermittedCalls;

        public CircuitBreakerStats(String name, CircuitBreaker.State state, float failureRate,
                                 long numberOfFailedCalls, long numberOfSuccessfulCalls, long numberOfNotPermittedCalls) {
            this.name = name;
            this.state = state;
            this.failureRate = failureRate;
            this.numberOfFailedCalls = numberOfFailedCalls;
            this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
            this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
        }

        public String getName() {
            return name;
        }

        public CircuitBreaker.State getState() {
            return state;
        }

        public float getFailureRate() {
            return failureRate;
        }

        public long getNumberOfFailedCalls() {
            return numberOfFailedCalls;
        }

        public long getNumberOfSuccessfulCalls() {
            return numberOfSuccessfulCalls;
        }

        public long getNumberOfNotPermittedCalls() {
            return numberOfNotPermittedCalls;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private CircuitBreaker.State state;
            private float failureRate;
            private long numberOfFailedCalls;
            private long numberOfSuccessfulCalls;
            private long numberOfNotPermittedCalls;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder state(CircuitBreaker.State state) {
                this.state = state;
                return this;
            }

            public Builder failureRate(float failureRate) {
                this.failureRate = failureRate;
                return this;
            }

            public Builder numberOfFailedCalls(long numberOfFailedCalls) {
                this.numberOfFailedCalls = numberOfFailedCalls;
                return this;
            }

            public Builder numberOfSuccessfulCalls(long numberOfSuccessfulCalls) {
                this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
                return this;
            }

            public Builder numberOfNotPermittedCalls(long numberOfNotPermittedCalls) {
                this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
                return this;
            }

            public CircuitBreakerStats build() {
                return new CircuitBreakerStats(name, state, failureRate, numberOfFailedCalls, 
                    numberOfSuccessfulCalls, numberOfNotPermittedCalls);
            }
        }
    }
} 