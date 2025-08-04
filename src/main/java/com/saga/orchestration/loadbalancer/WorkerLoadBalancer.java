package com.saga.orchestration.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancer para workers de saga
 * Segue o princípio SRP - responsabilidade única de balancear carga
 */
@Slf4j
@Component
public class WorkerLoadBalancer {

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * Registra um worker
     */
    public void registerWorker(String workerId, String host, int port, int maxConcurrency) {
        WorkerInfo workerInfo = new WorkerInfo(workerId, host, port, maxConcurrency);
        workers.put(workerId, workerInfo);
        log.info("Registered worker {} at {}:{} with max concurrency {}", workerId, host, port, maxConcurrency);
    }

    /**
     * Remove um worker
     */
    public void unregisterWorker(String workerId) {
        WorkerInfo removed = workers.remove(workerId);
        if (removed != null) {
            log.info("Unregistered worker {}", workerId);
        }
    }

    /**
     * Seleciona um worker usando round robin
     */
    public WorkerInfo selectWorkerRoundRobin() {
        List<WorkerInfo> availableWorkers = workers.values().stream()
            .filter(WorkerInfo::isAvailable)
            .toList();

        if (availableWorkers.isEmpty()) {
            return null;
        }

        int index = roundRobinCounter.getAndIncrement() % availableWorkers.size();
        return availableWorkers.get(index);
    }

    /**
     * Seleciona um worker usando least connections
     */
    public WorkerInfo selectWorkerLeastConnections() {
        return workers.values().stream()
            .filter(WorkerInfo::isAvailable)
            .min((w1, w2) -> Integer.compare(w1.getCurrentConnections(), w2.getCurrentConnections()))
            .orElse(null);
    }

    /**
     * Seleciona um worker usando weighted round robin
     */
    public WorkerInfo selectWorkerWeightedRoundRobin() {
        List<WorkerInfo> availableWorkers = workers.values().stream()
            .filter(WorkerInfo::isAvailable)
            .toList();

        if (availableWorkers.isEmpty()) {
            return null;
        }

        // Calcula peso total baseado na capacidade disponível
        int totalWeight = availableWorkers.stream()
            .mapToInt(w -> w.getMaxConcurrency() - w.getCurrentConnections())
            .sum();

        if (totalWeight <= 0) {
            return null;
        }

        int currentWeight = 0;
        int targetWeight = roundRobinCounter.getAndIncrement() % totalWeight;

        for (WorkerInfo worker : availableWorkers) {
            currentWeight += worker.getMaxConcurrency() - worker.getCurrentConnections();
            if (currentWeight > targetWeight) {
                return worker;
            }
        }

        return availableWorkers.get(0);
    }

    /**
     * Incrementa o contador de conexões de um worker
     */
    public void incrementWorkerConnections(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.incrementConnections();
            log.debug("Incremented connections for worker {}: {}", workerId, worker.getCurrentConnections());
        }
    }

    /**
     * Decrementa o contador de conexões de um worker
     */
    public void decrementWorkerConnections(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.decrementConnections();
            log.debug("Decremented connections for worker {}: {}", workerId, worker.getCurrentConnections());
        }
    }

    /**
     * Marca um worker como indisponível
     */
    public void markWorkerUnavailable(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAvailable(false);
            log.warn("Marked worker {} as unavailable", workerId);
        }
    }

    /**
     * Marca um worker como disponível
     */
    public void markWorkerAvailable(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAvailable(true);
            log.info("Marked worker {} as available", workerId);
        }
    }

    /**
     * Obtém estatísticas de todos os workers
     */
    public Map<String, WorkerStats> getWorkerStats() {
        return workers.entrySet().stream()
            .collect(ConcurrentHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue().getStats()),
                ConcurrentHashMap::putAll);
    }

    /**
     * Informações de um worker
     */
    public static class WorkerInfo {
        private final String workerId;
        private final String host;
        private final int port;
        private final int maxConcurrency;
        private final AtomicInteger currentConnections;
        private volatile boolean available;

        public WorkerInfo(String workerId, String host, int port, int maxConcurrency) {
            this.workerId = workerId;
            this.host = host;
            this.port = port;
            this.maxConcurrency = maxConcurrency;
            this.currentConnections = new AtomicInteger(0);
            this.available = true;
        }

        public String getWorkerId() {
            return workerId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public int getCurrentConnections() {
            return currentConnections.get();
        }

        public boolean isAvailable() {
            return available && currentConnections.get() < maxConcurrency;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void incrementConnections() {
            currentConnections.incrementAndGet();
        }

        public void decrementConnections() {
            currentConnections.decrementAndGet();
        }

        public WorkerStats getStats() {
            return new WorkerStats(workerId, host, port, maxConcurrency, 
                currentConnections.get(), available);
        }
    }

    /**
     * Estatísticas de um worker
     */
    public static class WorkerStats {
        private final String workerId;
        private final String host;
        private final int port;
        private final int maxConcurrency;
        private final int currentConnections;
        private final boolean available;

        public WorkerStats(String workerId, String host, int port, int maxConcurrency, 
                         int currentConnections, boolean available) {
            this.workerId = workerId;
            this.host = host;
            this.port = port;
            this.maxConcurrency = maxConcurrency;
            this.currentConnections = currentConnections;
            this.available = available;
        }

        public String getWorkerId() {
            return workerId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public int getCurrentConnections() {
            return currentConnections;
        }

        public boolean isAvailable() {
            return available;
        }

        public double getUtilization() {
            return maxConcurrency > 0 ? (double) currentConnections / maxConcurrency : 0.0;
        }
    }

    /**
     * Estratégias de load balancing
     */
    public enum LoadBalancingStrategy {
        ROUND_ROBIN,
        LEAST_CONNECTIONS,
        WEIGHTED_ROUND_ROBIN
    }

    /**
     * Seleciona um worker usando a estratégia especificada
     */
    public WorkerInfo selectWorker(LoadBalancingStrategy strategy) {
        return switch (strategy) {
            case ROUND_ROBIN -> selectWorkerRoundRobin();
            case LEAST_CONNECTIONS -> selectWorkerLeastConnections();
            case WEIGHTED_ROUND_ROBIN -> selectWorkerWeightedRoundRobin();
        };
    }
} 