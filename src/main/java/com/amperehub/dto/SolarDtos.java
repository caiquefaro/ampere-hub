package com.amperehub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public final class SolarDtos {

    private SolarDtos() {
    }

    public record SolarRequest(
            @Positive @Max(500000) double consumoMedioMensalKwh,
            @Min(30) @Max(100) int custoDisponibilidadeKwh,
            @DecimalMin("2.0") @DecimalMax("8.0") double horasSolPleno,
            @Min(100) @Max(1000) int potenciaModuloW,
            @DecimalMin("0.5") @DecimalMax("0.95") double performanceRatio,
            @Positive @DecimalMax("5.0") double tarifaPorKwh,
            @DecimalMin("1.0") @DecimalMax("15.0") double custoPorWattPico) {
    }

    public record SolarResultado(
            double consumoCompensavelKwh,
            double potenciaNecessariaKwp,
            int quantidadeModulos,
            double potenciaInstaladaKwp,
            double geracaoMensalKwh,
            double geracaoAnualKwh,
            double coberturaPercentual,
            double areaEstimadaM2,
            double investimentoEstimado,
            double economiaMensal,
            double economiaAnual,
            double paybackMeses,
            double economia25Anos,
            double co2EvitadoAnualKg) {
    }
}
