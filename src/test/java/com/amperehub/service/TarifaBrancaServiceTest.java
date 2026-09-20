package com.amperehub.service;

import com.amperehub.dto.TarifaDtos.TarifaBrancaRequest;
import com.amperehub.dto.TarifaDtos.TarifaBrancaResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarifaBrancaServiceTest {

    private final TarifaBrancaService service = new TarifaBrancaService();

    @Test
    @DisplayName("Perfil concentrado fora de ponta favorece a tarifa branca")
    void perfilForaDePontaFavoreceBranca() {
        var req = new TarifaBrancaRequest(20, 30, 350, 0.85, 1.60, 1.05, 0.62);

        TarifaBrancaResultado r = service.calcular(req);

        assertThat(r.modalidadeRecomendada()).isEqualTo("BRANCA");
        assertThat(r.custoBranca()).isLessThan(r.custoConvencional());
        assertThat(r.percentualForaPonta()).isGreaterThan(80.0);
    }

    @Test
    @DisplayName("Consumo pesado em ponta favorece a tarifa convencional")
    void perfilEmPontaFavoreceConvencional() {
        var req = new TarifaBrancaRequest(250, 50, 100, 0.85, 1.60, 1.05, 0.62);

        TarifaBrancaResultado r = service.calcular(req);

        assertThat(r.modalidadeRecomendada()).isEqualTo("CONVENCIONAL");
        assertThat(r.custoBranca()).isGreaterThan(r.custoConvencional());
    }

    @Test
    @DisplayName("O total confere com a soma dos três postos")
    void somaOsPostos() {
        var req = new TarifaBrancaRequest(20, 30, 350, 0.85, 1.60, 1.05, 0.62);
        assertThat(service.calcular(req).consumoTotalKwh()).isEqualTo(400.0);
    }

    @Test
    @DisplayName("A diferença anual é doze vezes a mensal")
    void projetaDiferencaAnual() {
        var req = new TarifaBrancaRequest(20, 30, 350, 0.85, 1.60, 1.05, 0.62);
        TarifaBrancaResultado r = service.calcular(req);
        assertThat(r.diferencaAnual()).isEqualTo(r.diferencaMensal() * 12);
    }

    @Test
    @DisplayName("Consumo zerado em todos os postos é rejeitado")
    void rejeitaConsumoZerado() {
        var req = new TarifaBrancaRequest(0, 0, 0, 0.85, 1.60, 1.05, 0.62);
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
