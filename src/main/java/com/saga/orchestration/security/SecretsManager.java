package com.saga.orchestration.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de secrets simples
 * Segue o princípio SRP - responsabilidade única de gerenciar secrets
 */
@Slf4j
@Component
public class SecretsManager {

    private final Map<String, String> secrets = new ConcurrentHashMap<>();
    
    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;
    
    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/sagadb}")
    private String mongoUri;

    /**
     * Obtém um secret por chave
     */
    public String getSecret(String key) {
        String secret = secrets.get(key);
        if (secret == null) {
            log.warn("Secret not found for key: {}", key);
            return null;
        }
        return secret;
    }

    /**
     * Define um secret
     */
    public void setSecret(String key, String value) {
        secrets.put(key, value);
        log.debug("Secret set for key: {}", key);
    }

    /**
     * Remove um secret
     */
    public void removeSecret(String key) {
        secrets.remove(key);
        log.debug("Secret removed for key: {}", key);
    }

    /**
     * Obtém o password do RabbitMQ
     */
    public String getRabbitPassword() {
        return rabbitPassword;
    }

    /**
     * Obtém a URI do MongoDB
     */
    public String getMongoUri() {
        return mongoUri;
    }

    /**
     * Verifica se um secret existe
     */
    public boolean hasSecret(String key) {
        return secrets.containsKey(key);
    }

    /**
     * Obtém todos os secrets (apenas para debug)
     */
    public Map<String, String> getAllSecrets() {
        return new ConcurrentHashMap<>(secrets);
    }

    /**
     * Limpa todos os secrets
     */
    public void clearSecrets() {
        secrets.clear();
        log.info("All secrets cleared");
    }

    /**
     * Obtém o número de secrets armazenados
     */
    public int getSecretCount() {
        return secrets.size();
    }
} 