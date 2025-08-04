package com.saga.orchestration.batch;

import com.saga.orchestration.model.SagaExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Processador de batch para múltiplas sagas
 * Segue o princípio SRP - responsabilidade única de processar batches
 */
@Slf4j
@Component
public class BatchProcessor {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Processa uma lista de itens em batch
     */
    public <T, R> Flux<R> processBatch(List<T> items, Function<T, Mono<R>> processor) {
        return processBatch(items, processor, DEFAULT_BATCH_SIZE, DEFAULT_TIMEOUT);
    }

    /**
     * Processa uma lista de itens em batch com configuração customizada
     */
    public <T, R> Flux<R> processBatch(List<T> items, Function<T, Mono<R>> processor, 
                                       int batchSize, Duration timeout) {
        return Flux.fromIterable(items)
            .buffer(batchSize)
            .flatMap(batch -> processBatchChunk(batch, processor, timeout), 5) // Concurrency de 5
            .doOnNext(result -> log.debug("Processed batch item"))
            .doOnError(error -> log.error("Error processing batch: {}", error.getMessage()));
    }

    /**
     * Processa um chunk de batch
     */
    private <T, R> Flux<R> processBatchChunk(List<T> batch, Function<T, Mono<R>> processor, Duration timeout) {
        return Flux.fromIterable(batch)
            .flatMap(item -> processor.apply(item)
                .timeout(timeout)
                .onErrorResume(error -> {
                    log.warn("Error processing item in batch: {}", error.getMessage());
                    return Mono.empty();
                }), 10); // Concurrency de 10 por chunk
    }

    /**
     * Processa sagas em batch
     */
    public Flux<SagaExecution> processSagaBatch(List<SagaExecution> sagas, 
                                                Function<SagaExecution, Mono<SagaExecution>> processor) {
        return processBatch(sagas, processor, 50, Duration.ofSeconds(60));
    }

    /**
     * Processa com retry em caso de falha
     */
    public <T, R> Flux<R> processBatchWithRetry(List<T> items, Function<T, Mono<R>> processor, 
                                                int maxRetries, Duration retryDelay) {
        return Flux.fromIterable(items)
            .buffer(DEFAULT_BATCH_SIZE)
            .flatMap(batch -> processBatchChunkWithRetry(batch, processor, maxRetries, retryDelay), 5)
            .doOnNext(result -> log.debug("Processed batch item with retry"))
            .doOnError(error -> log.error("Error processing batch with retry: {}", error.getMessage()));
    }

    /**
     * Processa um chunk de batch com retry
     */
    private <T, R> Flux<R> processBatchChunkWithRetry(List<T> batch, Function<T, Mono<R>> processor,
                                                      int maxRetries, Duration retryDelay) {
        return Flux.fromIterable(batch)
            .flatMap(item -> processor.apply(item)
                .retryWhen(reactor.util.retry.Retry.backoff(maxRetries, retryDelay))
                .onErrorResume(error -> {
                    log.warn("Error processing item in batch after retries: {}", error.getMessage());
                    return Mono.empty();
                }), 10);
    }

    /**
     * Processa com backpressure
     */
    public <T, R> Flux<R> processBatchWithBackpressure(List<T> items, Function<T, Mono<R>> processor) {
        return Flux.fromIterable(items)
            .buffer(DEFAULT_BATCH_SIZE)
            .onBackpressureBuffer(1000) // Buffer de 1000 chunks
            .flatMap(batch -> processBatchChunk(batch, processor, DEFAULT_TIMEOUT), 3) // Concurrency reduzida
            .doOnNext(result -> log.debug("Processed batch item with backpressure"))
            .doOnError(error -> log.error("Error processing batch with backpressure: {}", error.getMessage()));
    }

    /**
     * Estatísticas de processamento de batch
     */
    public static class BatchStats {
        private final int totalItems;
        private final int processedItems;
        private final int failedItems;
        private final Duration processingTime;

        public BatchStats(int totalItems, int processedItems, int failedItems, Duration processingTime) {
            this.totalItems = totalItems;
            this.processedItems = processedItems;
            this.failedItems = failedItems;
            this.processingTime = processingTime;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getProcessedItems() {
            return processedItems;
        }

        public int getFailedItems() {
            return failedItems;
        }

        public Duration getProcessingTime() {
            return processingTime;
        }

        public double getSuccessRate() {
            return totalItems > 0 ? (double) processedItems / totalItems : 0.0;
        }
    }
} 