package com.saga.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "rabbitmq")
@Getter
@Setter
public class RabbitProperties {

    private String exchange;

    private RoutingKeys routingKeys;

    @Getter
    @Setter
    public static class RoutingKeys {
        private String queuePropostaEmissaoAutomovel;
        private String queueEmitirPropostaAutomovel;
        private String queuePropostaAutomovelTransmitida;
        private String queueStatusEmissaoProposta;
    }
}
