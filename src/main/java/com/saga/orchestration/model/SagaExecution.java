package com.saga.orchestration.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Document(collection = "sagas")
public class SagaExecution {

    @Id
    private String id;  
    private String correlationId;
    private String stepName;
    private String inputQueue;
    private String outputQueue;
    private String fallback;
    private String rollback;
    private String payload;
    private String message;
    private String status;
    private Instant createdAt;

    public SagaExecution() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
    
}