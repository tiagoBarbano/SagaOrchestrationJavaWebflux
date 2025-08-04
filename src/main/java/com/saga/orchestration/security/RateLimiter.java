package com.saga.orchestration.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstração para Rate Limiting que pode ser configurada para diferentes necessidades
 * Segue o princípio SRP - responsabilidade única de controle de taxa
 */
@Slf4j
@Component
public class RateLimiter {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @Value("${saga.rate-limit.default-requests:100}")
    private int defaultRequests;

    @Value("${saga.rate-limit.default-window:60}")
    private int defaultWindowSeconds;

    /**
     * Verifica se a requisição pode ser processada baseada no rate limit
     */
    public boolean tryConsume(String key) {
        return tryConsume(key, 1);
    }

    /**
     * Verifica se a requisição pode ser processada baseada no rate limit
     */
    public boolean tryConsume(String key, int tokens) {
        Bucket bucket = getBucket(key);
        boolean consumed = bucket.tryConsume(tokens);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        
        return consumed;
    }

    /**
     * Obtém bucket local ou cria um novo
     */
    private Bucket getBucket(String key) {
        return localBuckets.computeIfAbsent(key, this::createBucket);
    }

    /**
     * Cria um novo bucket com configuração padrão
     */
    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(defaultRequests, 
            Refill.greedy(defaultRequests, Duration.ofSeconds(defaultWindowSeconds)));
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Configura rate limit customizado para uma chave específica
     */
    public void configureRateLimit(String key, int requests, Duration window) {
        Bandwidth limit = Bandwidth.classic(requests, Refill.greedy(requests, window));
        
        Bucket bucket = Bucket.builder()
            .addLimit(limit)
            .build();
            
        localBuckets.put(key, bucket);
    }

    /**
     * Remove rate limit para uma chave específica
     */
    public void removeRateLimit(String key) {
        localBuckets.remove(key);
    }

    /**
     * Obtém informações sobre o bucket
     */
    public RateLimitInfo getRateLimitInfo(String key) {
        Bucket bucket = getBucket(key);
        return RateLimitInfo.builder()
            .availableTokens(bucket.getAvailableTokens())
            .consumedTokens(bucket.getAvailableTokens())
            .build();
    }

    /**
     * Classe para informações do rate limit
     */
    public static class RateLimitInfo {
        private final long availableTokens;
        private final long consumedTokens;

        public RateLimitInfo(long availableTokens, long consumedTokens) {
            this.availableTokens = availableTokens;
            this.consumedTokens = consumedTokens;
        }

        public long getAvailableTokens() {
            return availableTokens;
        }

        public long getConsumedTokens() {
            return consumedTokens;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private long availableTokens;
            private long consumedTokens;

            public Builder availableTokens(long availableTokens) {
                this.availableTokens = availableTokens;
                return this;
            }

            public Builder consumedTokens(long consumedTokens) {
                this.consumedTokens = consumedTokens;
                return this;
            }

            public RateLimitInfo build() {
                return new RateLimitInfo(availableTokens, consumedTokens);
            }
        }
    }
} 