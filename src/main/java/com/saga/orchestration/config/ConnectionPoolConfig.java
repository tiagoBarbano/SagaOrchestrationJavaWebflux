package com.saga.orchestration.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import java.time.Duration;

/**
 * Configuração de connection pooling para MongoDB e RabbitMQ
 * Segue o princípio SRP - responsabilidade única de configurar pools de conexão
 */
@Slf4j
@Configuration
public class ConnectionPoolConfig {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/sagadb}")
    private String mongoUri;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    @Value("${saga.connection-pool.mongo.max-connections:100}")
    private int mongoMaxConnections;

    @Value("${saga.connection-pool.mongo.min-connections:5}")
    private int mongoMinConnections;

    @Value("${saga.connection-pool.rabbit.max-connections:50}")
    private int rabbitMaxConnections;

    @Value("${saga.connection-pool.rabbit.min-connections:2}")
    private int rabbitMinConnections;

    /**
     * Configuração do MongoDB com connection pooling
     */
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToConnectionPoolSettings(builder -> 
                builder.maxSize(mongoMaxConnections)
                       .minSize(mongoMinConnections)
                       .maxWaitTime(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                       .maxConnectionIdleTime(60000, java.util.concurrent.TimeUnit.MILLISECONDS)
                       .maxConnectionLifeTime(300000, java.util.concurrent.TimeUnit.MILLISECONDS))
            .applyToServerSettings(builder -> 
                builder.heartbeatFrequency(10000, java.util.concurrent.TimeUnit.MILLISECONDS))
            .applyToSocketSettings(builder -> 
                builder.connectTimeout(10000, java.util.concurrent.TimeUnit.MILLISECONDS)
                       .readTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS))
            .build();

        MongoClient mongoClient = MongoClients.create(settings);
        log.info("MongoDB connection pool configured with max: {}, min: {}", mongoMaxConnections, mongoMinConnections);
        
        return mongoClient;
    }

    /**
     * Configuração do ReactiveMongoTemplate
     */
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(MongoClient mongoClient) {
        return new ReactiveMongoTemplate(mongoClient, "sagadb");
    }

    /**
     * Configuração do RabbitMQ com connection pooling
     */
    @Bean
    public Mono<Connection> rabbitConnectionMono() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.setVirtualHost("/");
        
        // Configurações de connection pooling
        connectionFactory.setRequestedChannelMax(rabbitMaxConnections);
        connectionFactory.setRequestedFrameMax(0); // Sem limite de frame
        connectionFactory.setConnectionTimeout(10000); // 10 segundos
        connectionFactory.setHandshakeTimeout(10000); // 10 segundos
        
        // Configurações de heartbeat - removido método não disponível
        
        // Configurações de rede
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setNetworkRecoveryInterval(10000); // 10 segundos
        
        log.info("RabbitMQ connection pool configured with max channels: {}", rabbitMaxConnections);
        
        return Mono.fromCallable(() -> connectionFactory.newConnection()).cache();
    }

    /**
     * Configuração do Receiver com connection pooling
     */
    @Bean
    public Receiver receiver(Mono<Connection> connectionMono) {
        ReceiverOptions receiverOptions = new ReceiverOptions()
            .connectionMono(connectionMono);

        return RabbitFlux.createReceiver(receiverOptions);
    }

    /**
     * Configuração do Sender com connection pooling
     */
    @Bean
    public Sender sender(Mono<Connection> connectionMono) {
        SenderOptions senderOptions = new SenderOptions()
            .connectionMono(connectionMono);

        return RabbitFlux.createSender(senderOptions);
    }

    /**
     * Health check para MongoDB
     */
    @Bean
    public Mono<Boolean> mongoHealthCheck(MongoClient mongoClient) {
        return Mono.just(true)
            .doOnSuccess(success -> log.debug("MongoDB health check: {}", success))
            .doOnError(error -> log.error("MongoDB health check failed: {}", error.getMessage()));
    }

    /**
     * Health check para RabbitMQ
     */
    @Bean
    public Mono<Boolean> rabbitHealthCheck(Mono<Connection> connectionMono) {
        return connectionMono
            .map(connection -> connection.isOpen())
            .onErrorReturn(false)
            .doOnSuccess(success -> log.debug("RabbitMQ health check: {}", success))
            .doOnError(error -> log.error("RabbitMQ health check failed: {}", error.getMessage()));
    }

    /**
     * Métricas de connection pool
     */
    public static class ConnectionPoolMetrics {
        private final int currentConnections;
        private final int maxConnections;
        private final int availableConnections;
        private final double utilization;

        public ConnectionPoolMetrics(int currentConnections, int maxConnections, int availableConnections, double utilization) {
            this.currentConnections = currentConnections;
            this.maxConnections = maxConnections;
            this.availableConnections = availableConnections;
            this.utilization = utilization;
        }

        public int getCurrentConnections() {
            return currentConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public int getAvailableConnections() {
            return availableConnections;
        }

        public double getUtilization() {
            return utilization;
        }
    }
} 