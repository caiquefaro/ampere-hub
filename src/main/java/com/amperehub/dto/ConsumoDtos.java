package com.amperehub.dto;

import com.amperehub.domain.Bandeira;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ConsumoDtos {

    private ConsumoDtos() {
    }

    public record EquipamentoRequest(
            @NotBlank @Size(max = 60) String nome,
            @Positive @Max(50000) double potenciaW,
            @PositiveOrZero @DecimalMax("24.0") double horasPorDia,
            @Min(1) @Max(31) int diasPorMes,
            @Min(1) @Max(1000) int quantidade) {
    }

    public record ConsumoRequest(
            @NotEmpty @Size(max = 60) List<@Valid EquipamentoRequest> equipamentos,
            @Positive @DecimalMax("5.0") double tarifaPorKwh,
            @NotNull Bandeira bandeira) {
    }

    public record EquipamentoResultado(
            String nome,
            double consumoKwhMes,
            double custoMes,
            double participacaoPercentual) {
    }

    public record ConsumoResultado(
            double consumoTotalKwhMes,
            double custoEnergia,
            double custoBandeira,
            double custoTotalMes,
            double custoTotalAno,
            String bandeira,
            double tarifaEfetivaPorKwh,
            EquipamentoResultado maiorVilao,
            List<EquipamentoResultado> detalhamento) {
    }
}
