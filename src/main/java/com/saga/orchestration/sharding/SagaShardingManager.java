package com.saga.orchestration.sharding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Gerenciador de sharding para distribuir sagas por domínio
 * Segue o princípio SRP - responsabilidade única de gerenciar sharding
 */
@Slf4j
@Component
public class SagaShardingManager {

    private final Map<String, ShardInfo> shards = new ConcurrentHashMap<>();
    private final Map<String, String> sagaToShardMapping = new ConcurrentHashMap<>();

    /**
     * Registra um shard para um domínio específico
     */
    public void registerShard(String domain, String shardId, String queueName) {
        ShardInfo shardInfo = new ShardInfo(shardId, domain, queueName);
        shards.put(shardId, shardInfo);
        log.info("Registered shard {} for domain {}", shardId, domain);
    }

    /**
     * Atribui uma saga a um shard específico
     */
    public void assignSagaToShard(String sagaId, String shardId) {
        if (!shards.containsKey(shardId)) {
            throw new IllegalArgumentException("Shard " + shardId + " not found");
        }
        
        sagaToShardMapping.put(sagaId, shardId);
        log.debug("Assigned saga {} to shard {}", sagaId, shardId);
    }

    /**
     * Obtém o shard para uma saga específica
     */
    public String getShardForSaga(String sagaId) {
        return sagaToShardMapping.getOrDefault(sagaId, getDefaultShard());
    }

    /**
     * Obtém o shard baseado no hash do correlation ID
     */
    public String getShardByCorrelationId(String correlationId) {
        int hash = Math.abs(correlationId.hashCode());
        int shardIndex = hash % shards.size();
        
        String[] shardIds = shards.keySet().toArray(new String[0]);
        return shardIds[shardIndex];
    }

    /**
     * Obtém o shard baseado no domínio
     */
    public String getShardByDomain(String domain) {
        return shards.values().stream()
            .filter(shard -> domain.equals(shard.getDomain()))
            .findFirst()
            .map(ShardInfo::getShardId)
            .orElse(getDefaultShard());
    }

    /**
     * Obtém informações de um shard específico
     */
    public ShardInfo getShardInfo(String shardId) {
        return shards.get(shardId);
    }

    /**
     * Obtém todos os shards
     */
    public Map<String, ShardInfo> getAllShards() {
        return new ConcurrentHashMap<>(shards);
    }

    /**
     * Remove um shard
     */
    public void removeShard(String shardId) {
        ShardInfo removed = shards.remove(shardId);
        if (removed != null) {
            log.info("Removed shard {}", shardId);
        }
    }

    /**
     * Obtém o shard padrão (primeiro shard disponível)
     */
    private String getDefaultShard() {
        return shards.keySet().stream().findFirst().orElse("default");
    }

    /**
     * Informações de um shard
     */
    public static class ShardInfo {
        private final String shardId;
        private final String domain;
        private final String queueName;
        private final long createdAt;

        public ShardInfo(String shardId, String domain, String queueName) {
            this.shardId = shardId;
            this.domain = domain;
            this.queueName = queueName;
            this.createdAt = System.currentTimeMillis();
        }

        public String getShardId() {
            return shardId;
        }

        public String getDomain() {
            return domain;
        }

        public String getQueueName() {
            return queueName;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * Estratégias de sharding
     */
    public enum ShardingStrategy {
        HASH_BASED,      // Baseado em hash do correlation ID
        DOMAIN_BASED,    // Baseado no domínio da saga
        ROUND_ROBIN,     // Round robin entre shards
        LOAD_BASED       // Baseado na carga do shard
    }

    /**
     * Aplica uma estratégia de sharding
     */
    public String applyShardingStrategy(String sagaId, String correlationId, String domain, ShardingStrategy strategy) {
        return switch (strategy) {
            case HASH_BASED -> getShardByCorrelationId(correlationId);
            case DOMAIN_BASED -> getShardByDomain(domain);
            case ROUND_ROBIN -> getShardByRoundRobin(sagaId);
            case LOAD_BASED -> getShardByLoad();
        };
    }

    /**
     * Sharding por round robin
     */
    private String getShardByRoundRobin(String sagaId) {
        int hash = Math.abs(sagaId.hashCode());
        String[] shardIds = shards.keySet().toArray(new String[0]);
        return shardIds[hash % shardIds.length];
    }

    /**
     * Sharding baseado na carga (simplificado)
     */
    private String getShardByLoad() {
        // Implementação simplificada - retorna o primeiro shard disponível
        // Em uma implementação real, seria necessário monitorar a carga de cada shard
        return getDefaultShard();
    }
} 