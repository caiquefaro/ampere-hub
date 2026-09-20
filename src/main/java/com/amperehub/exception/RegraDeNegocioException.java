package com.amperehub.exception;

/** Erro previsível de cálculo, devolvido ao cliente como HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
