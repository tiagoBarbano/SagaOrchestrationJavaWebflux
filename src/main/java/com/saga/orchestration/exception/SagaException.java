package com.saga.orchestration.exception;

import lombok.Getter;

/**
 * Exceção base para todas as exceções relacionadas a sagas
 * Segue o princípio SRP - responsabilidade única de representar erros de saga
 */
@Getter
public class SagaException extends RuntimeException {
    
    private final String correlationId;
    private final String stepName;
    private final ErrorType errorType;
    private final String sagaId;

    public SagaException(String message, String correlationId, String stepName, ErrorType errorType) {
        super(message);
        this.correlationId = correlationId;
        this.stepName = stepName;
        this.errorType = errorType;
        this.sagaId = null;
    }

    public SagaException(String message, String correlationId, String stepName, ErrorType errorType, String sagaId) {
        super(message);
        this.correlationId = correlationId;
        this.stepName = stepName;
        this.errorType = errorType;
        this.sagaId = sagaId;
    }

    public SagaException(String message, String correlationId, String stepName, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.stepName = stepName;
        this.errorType = errorType;
        this.sagaId = null;
    }

    public SagaException(String message, String correlationId, String stepName, ErrorType errorType, String sagaId, Throwable cause) {
        super(message, cause);
        this.correlationId = correlationId;
        this.stepName = stepName;
        this.errorType = errorType;
        this.sagaId = sagaId;
    }

    /**
     * Tipos de erro para categorização
     */
    public enum ErrorType {
        VALIDATION_ERROR("Erro de validação"),
        PROCESSING_ERROR("Erro de processamento"),
        TIMEOUT_ERROR("Erro de timeout"),
        NETWORK_ERROR("Erro de rede"),
        DATABASE_ERROR("Erro de banco de dados"),
        MESSAGING_ERROR("Erro de mensageria"),
        CIRCUIT_BREAKER_ERROR("Erro de circuit breaker"),
        RATE_LIMIT_ERROR("Erro de rate limit"),
        SANITIZATION_ERROR("Erro de sanitização"),
        UNKNOWN_ERROR("Erro desconhecido");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
} 