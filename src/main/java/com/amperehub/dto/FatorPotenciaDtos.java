package com.amperehub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.amperehub.domain.Sistema;

public final class FatorPotenciaDtos {

    private FatorPotenciaDtos() {
    }

    public record FatorPotenciaRequest(
            @Positive @Max(100000) double potenciaAtivaKw,
            @DecimalMin("0.30") @DecimalMax("0.99") double fatorPotenciaAtual,
            @DecimalMin("0.92") @DecimalMax("1.00") double fatorPotenciaDesejado,
            @DecimalMin("110.0") @DecimalMax("35000.0") double tensaoV,
            @NotNull Sistema sistema,
            @PositiveOrZero double consumoMensalKwh,
            @PositiveOrZero @DecimalMax("5.0") double tarifaPorKwh) {
    }

    public record FatorPotenciaResultado(
            double potenciaAparenteAtualKva,
            double potenciaAparenteCorrigidaKva,
            double potenciaReativaAtualKvar,
            double potenciaReativaCorrigidaKvar,
            double capacitorNecessarioKvar,
            int bancoComercialKvar,
            double correnteAtualA,
            double correnteCorrigidaA,
            double reducaoCorrentePercentual,
            double liberacaoCapacidadeKva,
            double multaMensalEstimada,
            double multaAnualEstimada,
            boolean dentroDoLimiteLegal,
            String diagnostico) {
    }
}
