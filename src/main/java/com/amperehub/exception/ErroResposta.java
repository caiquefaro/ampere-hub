package com.amperehub.exception;

import java.time.Instant;
import java.util.List;

public record ErroResposta(
        int status,
        String mensagem,
        List<String> detalhes,
        Instant momento) {

    public static ErroResposta de(int status, String mensagem, List<String> detalhes) {
        return new ErroResposta(status, mensagem, detalhes, Instant.now());
    }
}
