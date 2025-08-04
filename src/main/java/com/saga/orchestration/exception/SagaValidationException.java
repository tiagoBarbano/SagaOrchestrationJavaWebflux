package com.saga.orchestration.exception;

/**
 * Exceção específica para erros de validação em sagas
 */
public class SagaValidationException extends SagaException {
    
    public SagaValidationException(String message, String correlationId, String stepName) {
        super(message, correlationId, stepName, ErrorType.VALIDATION_ERROR);
    }

    public SagaValidationException(String message, String correlationId, String stepName, String sagaId) {
        super(message, correlationId, stepName, ErrorType.VALIDATION_ERROR, sagaId);
    }

    public SagaValidationException(String message, String correlationId, String stepName, Throwable cause) {
        super(message, correlationId, stepName, ErrorType.VALIDATION_ERROR, cause);
    }
} 