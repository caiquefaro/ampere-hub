package com.amperehub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class TarifaDtos {

    private TarifaDtos() {
    }

    public record TarifaBrancaRequest(
            @PositiveOrZero double consumoPontaKwh,
            @PositiveOrZero double consumoIntermediarioKwh,
            @PositiveOrZero double consumoForaPontaKwh,
            @Positive @DecimalMax("5.0") double tarifaConvencional,
            @Positive @DecimalMax("10.0") double tarifaPonta,
            @Positive @DecimalMax("10.0") double tarifaIntermediaria,
            @Positive @DecimalMax("5.0") double tarifaForaPonta) {
    }

    public record TarifaBrancaResultado(
            double consumoTotalKwh,
            double custoConvencional,
            double custoBranca,
            double diferencaMensal,
            double diferencaPercentual,
            double diferencaAnual,
            String modalidadeRecomendada,
            double percentualForaPonta,
            String leitura) {
    }
}
