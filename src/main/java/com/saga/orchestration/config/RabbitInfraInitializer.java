package com.saga.orchestration.config;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Sender;

@Component
public class RabbitInfraInitializer {

    private final Sender sender;
    private final RabbitProperties rabbitProperties;

    public RabbitInfraInitializer(Sender sender, RabbitProperties rabbitProperties) {
        this.sender = sender;
        this.rabbitProperties = rabbitProperties;
        declareInfra();
    }

    public void declareInfra() {
        Mono.when(
            sender.declareExchange(ExchangeSpecification.exchange(rabbitProperties.getExchange()).type("topic").durable(true)),
            sender.declareQueue(QueueSpecification.queue(rabbitProperties.getRoutingKeys().getQueueEmitirPropostaAutomovel()).durable(true)),
            sender.declareQueue(QueueSpecification.queue(rabbitProperties.getRoutingKeys().getQueuePropostaAutomovelTransmitida()).durable(true)),
            sender.declareQueue(QueueSpecification.queue(rabbitProperties.getRoutingKeys().getQueuePropostaEmissaoAutomovel()).durable(true)),
            sender.declareQueue(QueueSpecification.queue(rabbitProperties.getRoutingKeys().getQueueStatusEmissaoProposta()).durable(true)),
            sender.bind(BindingSpecification.binding().exchange(rabbitProperties.getExchange()).queue(rabbitProperties.getRoutingKeys().getQueueEmitirPropostaAutomovel()).routingKey(rabbitProperties.getRoutingKeys().getQueueEmitirPropostaAutomovel())),
            sender.bind(BindingSpecification.binding().exchange(rabbitProperties.getExchange()).queue(rabbitProperties.getRoutingKeys().getQueuePropostaAutomovelTransmitida()).routingKey(rabbitProperties.getRoutingKeys().getQueuePropostaAutomovelTransmitida())),
            sender.bind(BindingSpecification.binding().exchange(rabbitProperties.getExchange()).queue(rabbitProperties.getRoutingKeys().getQueuePropostaEmissaoAutomovel()).routingKey(rabbitProperties.getRoutingKeys().getQueuePropostaEmissaoAutomovel())),
            sender.bind(BindingSpecification.binding().exchange(rabbitProperties.getExchange()).queue(rabbitProperties.getRoutingKeys().getQueueStatusEmissaoProposta()).routingKey(rabbitProperties.getRoutingKeys().getQueueStatusEmissaoProposta()))
        )
        .doOnSuccess(v -> System.out.println("RabbitMQ infra criada"))
        .doOnError(Throwable::printStackTrace)
        .subscribe();
    }
}
