package com.saga.orchestration.dto.request;

public record Notification(
    String tipoEvento,
    String codigoIdentificadorEvento,
    String nomeRotaRecurso,
    int quantidadeTentativa,
    String dataHoraEvento
) {}
