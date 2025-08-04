package com.saga.orchestration.partitioning;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de partitioning de filas por tenant
 * Segue o princípio SRP - responsabilidade única de gerenciar partitioning de filas
 */
@Slf4j
@Component
public class QueuePartitioningManager {

    private final Map<String, PartitionInfo> partitions = new ConcurrentHashMap<>();
    private final Map<String, String> tenantToPartitionMapping = new ConcurrentHashMap<>();

    /**
     * Registra uma partição para um tenant
     */
    public void registerPartition(String tenantId, String partitionId, String queuePrefix) {
        PartitionInfo partitionInfo = new PartitionInfo(partitionId, tenantId, queuePrefix);
        partitions.put(partitionId, partitionInfo);
        tenantToPartitionMapping.put(tenantId, partitionId);
        log.info("Registered partition {} for tenant {} with queue prefix {}", partitionId, tenantId, queuePrefix);
    }

    /**
     * Obtém a partição para um tenant específico
     */
    public String getPartitionForTenant(String tenantId) {
        return tenantToPartitionMapping.getOrDefault(tenantId, getDefaultPartition());
    }

    /**
     * Obtém o nome da fila particionada para um tenant
     */
    public String getPartitionedQueueName(String tenantId, String baseQueueName) {
        String partitionId = getPartitionForTenant(tenantId);
        PartitionInfo partition = partitions.get(partitionId);
        
        if (partition != null) {
            return partition.getQueuePrefix() + "." + baseQueueName;
        }
        
        return baseQueueName; // Fallback para fila não particionada
    }

    /**
     * Obtém informações de uma partição específica
     */
    public PartitionInfo getPartitionInfo(String partitionId) {
        return partitions.get(partitionId);
    }

    /**
     * Obtém todas as partições
     */
    public Map<String, PartitionInfo> getAllPartitions() {
        return new ConcurrentHashMap<>(partitions);
    }

    /**
     * Remove uma partição
     */
    public void removePartition(String partitionId) {
        PartitionInfo removed = partitions.remove(partitionId);
        if (removed != null) {
            // Remove mapeamento de tenant para esta partição
            tenantToPartitionMapping.entrySet().removeIf(entry -> partitionId.equals(entry.getValue()));
            log.info("Removed partition {}", partitionId);
        }
    }

    /**
     * Obtém a partição padrão (primeira partição disponível)
     */
    private String getDefaultPartition() {
        return partitions.keySet().stream().findFirst().orElse("default");
    }

    /**
     * Obtém estatísticas de partitioning
     */
    public PartitioningStats getPartitioningStats() {
        int totalPartitions = partitions.size();
        int totalTenants = tenantToPartitionMapping.size();
        
        return new PartitioningStats(totalPartitions, totalTenants);
    }

    /**
     * Informações de uma partição
     */
    public static class PartitionInfo {
        private final String partitionId;
        private final String tenantId;
        private final String queuePrefix;
        private final long createdAt;

        public PartitionInfo(String partitionId, String tenantId, String queuePrefix) {
            this.partitionId = partitionId;
            this.tenantId = tenantId;
            this.queuePrefix = queuePrefix;
            this.createdAt = System.currentTimeMillis();
        }

        public String getPartitionId() {
            return partitionId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getQueuePrefix() {
            return queuePrefix;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * Estatísticas de partitioning
     */
    public static class PartitioningStats {
        private final int totalPartitions;
        private final int totalTenants;

        public PartitioningStats(int totalPartitions, int totalTenants) {
            this.totalPartitions = totalPartitions;
            this.totalTenants = totalTenants;
        }

        public int getTotalPartitions() {
            return totalPartitions;
        }

        public int getTotalTenants() {
            return totalTenants;
        }
    }

    /**
     * Estratégias de partitioning
     */
    public enum PartitioningStrategy {
        TENANT_BASED,    // Baseado no tenant
        HASH_BASED,      // Baseado em hash do tenant
        ROUND_ROBIN      // Round robin entre partições
    }

    /**
     * Aplica uma estratégia de partitioning
     */
    public String applyPartitioningStrategy(String tenantId, PartitioningStrategy strategy) {
        return switch (strategy) {
            case TENANT_BASED -> getPartitionForTenant(tenantId);
            case HASH_BASED -> getPartitionByHash(tenantId);
            case ROUND_ROBIN -> getPartitionByRoundRobin(tenantId);
        };
    }

    /**
     * Partitioning por hash do tenant
     */
    private String getPartitionByHash(String tenantId) {
        int hash = Math.abs(tenantId.hashCode());
        String[] partitionIds = partitions.keySet().toArray(new String[0]);
        return partitionIds[hash % partitionIds.length];
    }

    /**
     * Partitioning por round robin
     */
    private String getPartitionByRoundRobin(String tenantId) {
        int hash = Math.abs(tenantId.hashCode());
        String[] partitionIds = partitions.keySet().toArray(new String[0]);
        return partitionIds[hash % partitionIds.length];
    }

    /**
     * Obtém todas as filas de um tenant
     */
    public java.util.List<String> getTenantQueues(String tenantId) {
        String partitionId = getPartitionForTenant(tenantId);
        PartitionInfo partition = partitions.get(partitionId);
        
        if (partition != null) {
            // Em uma implementação real, seria necessário consultar as filas existentes
            // Por enquanto, retorna uma lista vazia
            return java.util.List.of();
        }
        
        return java.util.List.of();
    }

    /**
     * Verifica se uma fila pertence a um tenant
     */
    public boolean isQueueForTenant(String queueName, String tenantId) {
        String partitionId = getPartitionForTenant(tenantId);
        PartitionInfo partition = partitions.get(partitionId);
        
        if (partition != null) {
            return queueName.startsWith(partition.getQueuePrefix());
        }
        
        return false;
    }
} 