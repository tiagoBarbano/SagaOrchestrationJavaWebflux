package com.saga.orchestration.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.saga.orchestration.model.SagaExecution;

@Repository
public interface SagaExecutionRepository extends ReactiveMongoRepository<SagaExecution, String> {
}
