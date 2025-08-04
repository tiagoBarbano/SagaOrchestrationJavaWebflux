package com.saga.orchestration.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de cache para configurações e metadados de saga
 * Segue o princípio SRP - responsabilidade única de gerenciar cache
 */
@Slf4j
@Component
public class SagaCacheManager {

    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    
    /**
     * Cache de configurações de saga
     */
    @Cacheable(value = "sagaConfigs", key = "#configKey")
    public String getSagaConfig(String configKey) {
        log.debug("Cache miss for config key: {}", configKey);
        return null; // Será implementado pelo Spring Cache
    }

    /**
     * Cache de metadados de saga
     */
    @Cacheable(value = "sagaMetadata", key = "#sagaType")
    public SagaMetadata getSagaMetadata(String sagaType) {
        log.debug("Cache miss for saga metadata: {}", sagaType);
        return null; // Será implementado pelo Spring Cache
    }

    /**
     * Cache local para dados temporários
     */
    public void putLocalCache(String key, Object value, Duration ttl) {
        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis());
        localCache.put(key, entry);
        log.debug("Added to local cache: {}", key);
    }

    public Object getLocalCache(String key) {
        CacheEntry entry = localCache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for key: {}", key);
            return entry.getValue();
        }
        
        if (entry != null && entry.isExpired()) {
            localCache.remove(key);
            log.debug("Removed expired cache entry: {}", key);
        }
        
        return null;
    }

    public void removeLocalCache(String key) {
        localCache.remove(key);
        log.debug("Removed from local cache: {}", key);
    }

    public void clearLocalCache() {
        localCache.clear();
        log.debug("Cleared local cache");
    }

    /**
     * Entrada de cache com TTL
     */
    private static class CacheEntry {
        private final Object value;
        private final long expirationTime;

        public CacheEntry(Object value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    /**
     * Metadados de saga
     */
    public static class SagaMetadata {
        private final String sagaType;
        private final int maxRetries;
        private final Duration timeout;
        private final boolean enableCircuitBreaker;

        public SagaMetadata(String sagaType, int maxRetries, Duration timeout, boolean enableCircuitBreaker) {
            this.sagaType = sagaType;
            this.maxRetries = maxRetries;
            this.timeout = timeout;
            this.enableCircuitBreaker = enableCircuitBreaker;
        }

        public String getSagaType() {
            return sagaType;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public boolean isEnableCircuitBreaker() {
            return enableCircuitBreaker;
        }
    }
} 