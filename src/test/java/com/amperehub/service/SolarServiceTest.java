package com.amperehub.service;

import com.amperehub.dto.SolarDtos.SolarRequest;
import com.amperehub.dto.SolarDtos.SolarResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolarServiceTest {

    private final SolarService service = new SolarService();

    private SolarRequest residencia(double consumo) {
        return new SolarRequest(consumo, 50, 4.8, 550, 0.80, 0.95, 3.80);
    }

    @Test
    @DisplayName("Desconta o custo de disponibilidade do consumo compensável")
    void descontaCustoDeDisponibilidade() {
        SolarResultado r = service.calcular(residencia(500));
        assertThat(r.consumoCompensavelKwh()).isEqualTo(450.0);
    }

    @Test
    @DisplayName("Arredonda a quantidade de módulos para cima")
    void arredondaModulosParaCima() {
        SolarResultado r = service.calcular(residencia(500));

        double kwpNecessario = 450.0 / 30 / (4.8 * 0.80);
        int esperado = (int) Math.ceil(kwpNecessario * 1000 / 550);

        assertThat(r.quantidadeModulos()).isEqualTo(esperado);
        assertThat(r.potenciaInstaladaKwp()).isEqualTo(esperado * 550 / 1000.0);
    }

    @Test
    @DisplayName("A geração do sistema instalado cobre o consumo compensável")
    void geracaoCobreOConsumo() {
        SolarResultado r = service.calcular(residencia(500));
        assertThat(r.geracaoMensalKwh()).isGreaterThanOrEqualTo(r.consumoCompensavelKwh());
        assertThat(r.geracaoAnualKwh()).isCloseTo(r.geracaoMensalKwh() * 12,
                org.assertj.core.data.Offset.offset(0.05));
    }

    @Test
    @DisplayName("O payback bate com investimento dividido pela economia mensal")
    void paybackCoerente() {
        SolarResultado r = service.calcular(residencia(800));
        assertThat(r.paybackMeses()).isCloseTo(r.investimentoEstimado() / r.economiaMensal(),
                org.assertj.core.data.Offset.offset(0.05));
        assertThat(r.paybackMeses()).isBetween(1.0, 240.0);
    }

    @Test
    @DisplayName("Consumo abaixo do custo de disponibilidade é rejeitado")
    void rejeitaConsumoInsuficiente() {
        assertThatThrownBy(() -> service.calcular(residencia(40)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("custo de disponibilidade");
    }

    @Test
    @DisplayName("A economia em 25 anos considera a degradação anual dos módulos")
    void economiaDeLongoPrazoConsideraDegradacao() {
        SolarResultado r = service.calcular(residencia(600));
        assertThat(r.economia25Anos()).isLessThan(r.economiaAnual() * 25);
        assertThat(r.economia25Anos()).isGreaterThan(r.economiaAnual() * 20);
    }
}
