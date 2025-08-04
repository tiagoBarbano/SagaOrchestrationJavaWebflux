package com.saga.orchestration.exception;

/**
 * Exceção específica para erros de processamento em sagas
 */
public class SagaProcessingException extends SagaException {
    
    public SagaProcessingException(String message, String correlationId, String stepName) {
        super(message, correlationId, stepName, ErrorType.PROCESSING_ERROR);
    }

    public SagaProcessingException(String message, String correlationId, String stepName, String sagaId) {
        super(message, correlationId, stepName, ErrorType.PROCESSING_ERROR, sagaId);
    }

    public SagaProcessingException(String message, String correlationId, String stepName, Throwable cause) {
        super(message, correlationId, stepName, ErrorType.PROCESSING_ERROR, cause);
    }
} 