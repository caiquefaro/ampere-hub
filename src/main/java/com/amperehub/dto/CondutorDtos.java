package com.amperehub.dto;

import com.amperehub.domain.MetodoInstalacao;
import com.amperehub.domain.Sistema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class CondutorDtos {

    private CondutorDtos() {
    }

    public record CondutorRequest(
            @Positive @Max(500000) double potenciaW,
            @DecimalMin("110.0") @DecimalMax("1000.0") double tensaoV,
            @DecimalMin("0.5") @DecimalMax("1.0") double fatorPotencia,
            @NotNull Sistema sistema,
            @NotNull MetodoInstalacao metodoInstalacao,
            @Positive @Max(2000) double comprimentoM,
            @DecimalMin("1.0") @DecimalMax("7.0") double quedaTensaoMaximaPercentual) {
    }

    public record CondutorResultado(
            double correnteProjetoA,
            double secaoPorCapacidadeMm2,
            double secaoPorQuedaMm2,
            double secaoAdotadaMm2,
            double capacidadeConducaoA,
            double quedaTensaoV,
            double quedaTensaoPercentual,
            int disjuntorA,
            String criterioDeterminante,
            String observacao) {
    }
}
