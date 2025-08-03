package com.saga.orchestration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import reactor.core.publisher.Mono;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@Configuration
public class RabbitConfig {


    @Bean
    public Mono<Connection> connectionMono() {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost("localhost");
        cf.setPort(5672);
        cf.setUsername("guest");
        cf.setPassword("guest");
        cf.useNio();
        return Mono.fromCallable(() -> cf.newConnection()).cache();
    }

    @Bean
    public Receiver receiver(Mono<Connection> connMono) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connMono));
    }

    @Bean
    public Sender sender(Mono<Connection> connMono) {
        return RabbitFlux.createSender(new SenderOptions().connectionMono(connMono));
    }
 
}