package com.saga.orchestration.service;

import org.springframework.stereotype.Component;

import com.saga.orchestration.annotations.Fallback;
import com.saga.orchestration.annotations.Rollback;
import com.saga.orchestration.annotations.Step;
// import com.saga.orchestration.config.RabbitProperties;
import com.saga.orchestration.dto.request.ClienteRequest;
import com.saga.orchestration.dto.request.Notification;

import io.github.springwolf.bindings.amqp.annotations.AmqpAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import reactor.core.publisher.Mono;

@Component
public class VendasSaga {
    
    // private final RabbitProperties rabbitProperties;

    private static final String RK_PROPOSTA_EMISSAO_AUTOMOVEL = "proposta-emissao-automovel";
    private static final String RK_EMITIR_PROPOSTA_AUTOMOVEL = "emitir-proposta-automovel";
    private static final String RK_PROPOSTA_AUTOMOVEL_TRAMITIDA = "proposta-automovel-transmitida";
    private static final String RK_STATUS_EMISSAO_PROPOSTA = "status-emissao-proposta";

    @AmqpAsyncOperationBinding(cc = RK_EMITIR_PROPOSTA_AUTOMOVEL)
    @AsyncListener(operation = @AsyncOperation(
        channelName = RK_PROPOSTA_EMISSAO_AUTOMOVEL,
        description = "Emissão de proposta de automóvel",
        payloadType = Notification.class,
        headers = @AsyncOperation.Headers(
            schemaName = "SpringAmpqDefaultHeaders", 
            values = {
                @AsyncOperation.Headers.Header(
                    name = "x-correlation-id", 
                    description = "correlation Id Header"),
            })))
    @AsyncPublisher(operation = @AsyncOperation(
        channelName = RK_PROPOSTA_EMISSAO_AUTOMOVEL,
        description = "Publica a proposta de automóvel emitida",
        payloadType = ClienteRequest.class,
        headers = @AsyncOperation.Headers(
            schemaName = "SpringAmpqDefaultHeaders", 
            values = {
                @AsyncOperation.Headers.Header(
                    name = "x-correlation-id", 
                    description = "correlation Id Header"),
            })))
    @Step(
        consume = RK_PROPOSTA_EMISSAO_AUTOMOVEL, 
        produce = RK_EMITIR_PROPOSTA_AUTOMOVEL, 
        consumeDTO = Notification.class, 
        produceDTO = ClienteRequest.class
    )
    public Mono<ClienteRequest> propostaEmissaoAutomovel(Notification input) {
        // Simula a emissão de uma proposta de automóvel
        System.out.println("VERIFICA OS DADOS DE ENTRADA");
        System.out.println("BUSCA OS DADOS DA APOLICE NO HUBIN");
        System.out.println("Emissão de proposta de automóvel para");
        ClienteRequest cliente = new ClienteRequest("João da Silva", "tiago@tiago.com", "11999999999");
        System.out.println("Cliente: " + cliente.nome() + ", Email: " + cliente.email() + ", Telefone: " + cliente.telefone());
        System.out.println("Proposta emitida com sucesso!");
        // Retorna o cliente como resultado da emissão da proposta
        System.out.println("Proposta emitida com sucesso!");
        System.out.println("Enviando notificação de proposta emitida...");
        System.out.println("Tipo de Evento: " + input.tipoEvento());
        System.out.println("Código Identificador Evento: " + input.codigoIdentificadorEvento());
        
        return Mono.just(cliente);
    }

    /* --------------------------------------------------------------------------------------------------- */

    @AmqpAsyncOperationBinding(cc= RK_PROPOSTA_AUTOMOVEL_TRAMITIDA)
    @AsyncListener(operation = @AsyncOperation(channelName = RK_EMITIR_PROPOSTA_AUTOMOVEL,
            description = "Publica a proposta de automóvel emitida",
            payloadType = ClienteRequest.class,    
            headers = @AsyncOperation.Headers(schemaName = "SpringAmpqDefaultHeaders", values = {
                        @AsyncOperation.Headers.Header(name = "x-correlation-id", description = "correlation Id Header"),
            })))
    @AsyncPublisher(operation = @AsyncOperation(channelName = RK_EMITIR_PROPOSTA_AUTOMOVEL,
            description = "Publica a proposta de automóvel emitida",
            payloadType = String.class,    
            headers = @AsyncOperation.Headers(schemaName = "SpringAmpqDefaultHeaders", values = {
                        @AsyncOperation.Headers.Header(name = "x-correlation-id", description = "correlation Id Header"),
            })))            
    @Step(
        consume = RK_EMITIR_PROPOSTA_AUTOMOVEL, 
        produce = RK_PROPOSTA_AUTOMOVEL_TRAMITIDA,
        consumeDTO = ClienteRequest.class)
    public Mono<String> criarPedido(ClienteRequest input) {
        String msg = "Pedido criado para o cliente: " + input.nome() + 
                    ", Email: " + input.email() + 
                    ", Telefone: " + input.telefone();
        return Mono.just(msg + "->pedido_ok");
    }

    /* --------------------------------------------------------------------------------------------------- */

    @AmqpAsyncOperationBinding()
    @AsyncListener(operation = @AsyncOperation(channelName = RK_PROPOSTA_AUTOMOVEL_TRAMITIDA,
            headers = @AsyncOperation.Headers(schemaName = "SpringAmpqDefaultHeaders", values = {
                        @AsyncOperation.Headers.Header(name = "x-correlation-id", description = "correlation Id Header"),
            })))
    @Step(consume = RK_PROPOSTA_AUTOMOVEL_TRAMITIDA)
    public Mono<String> criarPagamento(String input) {
        return Mono.just(input + "->pagamento_ok");
    }

    /* --------------------------------------------------------------------------------------------------- */


    @Rollback(forStep = "propostaEmissaoAutomovel")
    public Mono<Void> rollbackCliente(String input) {
        System.out.println("Rollback pedido: " + input);
        return Mono.empty();
    }

    /* --------------------------------------------------------------------------------------------------- */


    @Fallback(forStep = "criarPedido")
    public Mono<Void> fallbackPedido(String input) {
        System.out.println("Fallback pedido: " + input);
        return Mono.empty();
    }
}