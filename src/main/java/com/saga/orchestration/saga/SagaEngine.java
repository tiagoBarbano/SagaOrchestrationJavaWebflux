package com.saga.orchestration.saga;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.saga.orchestration.annotations.Fallback;
import com.saga.orchestration.annotations.Rollback;
import com.saga.orchestration.annotations.Step;
import com.saga.orchestration.model.SagaExecution;
import com.saga.orchestration.repository.SagaExecutionRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

@Slf4j
@Component
public class SagaEngine implements ApplicationListener<ApplicationReadyEvent> {

    private final Receiver receiver;
    private final Sender sender;
    private final ApplicationContext context;
    private final SagaExecutionRepository sagaRepository;
    private final ObjectMapper objectMapper;

    public SagaEngine(Receiver receiver, Sender sender, ApplicationContext context,
            SagaExecutionRepository sagaRepository) {
        this.receiver = receiver;
        this.sender = sender;
        this.context = context;
        this.sagaRepository = sagaRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<String, Object> beans = context.getBeansWithAnnotation(Component.class);

        beans.values().forEach(bean -> {
            Method[] methods = bean.getClass().getDeclaredMethods();

            for (Method method : methods) {
                Step step = method.getAnnotation(Step.class);
                if (step != null) {
                    String inputQueue = step.consume();
                    String outputQueue = step.produce();
                    String stepName = method.getName();
                    Class<?> consumeDTO = step.consumeDTO();
                    Class<?> produceDTO = step.produceDTO();
                    

                    receiver.consumeAutoAck(inputQueue).<Map.Entry<String, String>>map(delivery -> {
                        Map<String, Object> headers = delivery.getProperties().getHeaders();
                        String correlationId = (headers != null && headers.containsKey("x-correlation-id"))
                                ? headers.get("x-correlation-id").toString()
                                : UUID.randomUUID().toString();
                        return new AbstractMap.SimpleEntry<>(new String(delivery.getBody()), correlationId);
                    })
                            .flatMap(entry -> {
                                String msg = entry.getKey();
                                String correlationId = entry.getValue();
                                extractedSagaExecution(inputQueue, outputQueue, stepName, "", msg, "", correlationId, "IN_PROGRESS");

                                try {
                                    Object payload = (consumeDTO != Void.class) ? objectMapper.readValue(msg, consumeDTO) : msg;
                                    Object result = method.invoke(bean, payload);
                                    // Mono<String> response = (result instanceof Mono<?> mono) ? mono.map(Object::toString) : Mono.just(result.toString());
                                    Mono<String> response = (result instanceof Mono<?> mono)
                                        ? mono.map(value -> serialize(value, objectMapper, produceDTO))
                                        : Mono.just(serialize(result, objectMapper, produceDTO));
                                    return response
                                            .doOnNext(res -> { 
                                                extractedSagaExecution(inputQueue, outputQueue, stepName, "", msg, "", correlationId, "DONE"); })
                                            .flatMap(res -> {
                                                if (!outputQueue.isEmpty()) {

                                                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                                            .headers(Map.of("x-correlation-id", correlationId))
                                                            .build();

                                                    OutboundMessage outboundMessage = new OutboundMessage(
                                                            "teste",
                                                            outputQueue,
                                                            props,
                                                            res.getBytes() // body
                                                    );
                                                    return sender.send(Flux.just(outboundMessage));
                                                } else
                                                    return Mono.empty();
                                            })
                                            .then();
                                } catch (Exception e) {
                                    extractedSagaExecution(inputQueue, outputQueue, stepName, "fallback", msg, e.toString(), correlationId, "FAILED");
                                    return handleFallbackOrRollback(bean, methods, stepName, e.toString(), msg, inputQueue, outputQueue, correlationId);
                                }
                            })
                            .subscribe();
                }
            }
        });
    }

    private String serialize(Object value, ObjectMapper objectMapper, Class<?> targetType) {
    try {
        if (targetType != null && targetType != Void.class) {
            // Serializa para JSON
            return objectMapper.writeValueAsString(value);
        } else {
            // Usa toString se não tiver tipo alvo
            return value.toString();
        }
    } catch (JsonProcessingException e) {
        throw new RuntimeException("Erro ao serializar resposta", e);
    }
}

    private Mono<Void> handleFallbackOrRollback(Object bean, Method[] allMethods, String failedStep, String error, String msg,
            String inputQueue, String outputQueue, String correlationId) {
        for (Method method : allMethods) {
            Fallback fallback = method.getAnnotation(Fallback.class);

            if (fallback != null && fallback.forStep().equals(failedStep)) {
                try {
                    extractedSagaExecution(inputQueue, outputQueue, failedStep, method.toString(), msg, error, correlationId, "IN_PROGRESS-FALLBACK");
                    return ((Mono<?>) method.invoke(bean, msg)).then();
                } catch (Exception e) {
                    extractedSagaExecution(inputQueue, outputQueue, failedStep, failedStep, msg, error, correlationId,"FAILED-FALLBACK");
                    return Mono.error(e);
                }
            }
        }

        // rollback
        for (Method method : allMethods) {
            Rollback rollback = method.getAnnotation(Rollback.class);

            if (rollback != null && rollback.forStep().equals(failedStep)) {
                try {
                    extractedSagaExecution(inputQueue, outputQueue, failedStep, failedStep, msg, error, correlationId, "IN_PROGRESS-rollback");
                    return ((Mono<?>) method.invoke(bean, msg)).then();
                } catch (Exception e) {
                    extractedSagaExecution(inputQueue, outputQueue, failedStep, failedStep, msg, error, correlationId,"FAILED-rollback");
                    return Mono.error(e);
                }
            }
        }

        return Mono.empty(); // sem fallback ou rollback definido
    }

    private Mono<Void> extractedSagaExecution(String inputQueue, String outputQueue, String stepName, String fallback, String payload, String message,
            String correlationId, String status) {
        SagaExecution sagaExecution = new SagaExecution();
        sagaExecution.setCorrelationId(correlationId);
        sagaExecution.setStepName(stepName);
        sagaExecution.setInputQueue(inputQueue);
        sagaExecution.setOutputQueue(outputQueue);
        sagaExecution.setPayload(payload);
        sagaExecution.setStatus(status);
        sagaExecution.setFallback(fallback);
        sagaExecution.setCreatedAt(Instant.now());
        sagaExecution.setMessage(message);
        sagaRepository.save(sagaExecution)
                .doOnSuccess(saved -> log.info("Saga salva com id: {}", saved.getId()))
                .doOnError(e -> log.error("Erro ao salvar saga", e))
                .subscribe();
        return Mono.empty();
    }
}