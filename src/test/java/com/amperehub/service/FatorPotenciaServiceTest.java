package com.amperehub.service;

import com.amperehub.domain.Sistema;
import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaRequest;
import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FatorPotenciaServiceTest {

    private final FatorPotenciaService service = new FatorPotenciaService();

    @Test
    @DisplayName("100 kW de 0,75 para 0,92 exigem cerca de 45,6 kVAr")
    void calculaPotenciaReativaNecessaria() {
        var req = new FatorPotenciaRequest(100, 0.75, 0.92, 380,
                Sistema.TRIFASICO, 30000, 0.60);

        FatorPotenciaResultado r = service.calcular(req);

        assertThat(r.capacitorNecessarioKvar()).isCloseTo(45.6, Offset.offset(0.5));
        assertThat(r.bancoComercialKvar()).isEqualTo(50);
    }

    @Test
    @DisplayName("A corrente cai na mesma proporção do ganho de fator de potência")
    void reduzCorrenteDoAlimentador() {
        var req = new FatorPotenciaRequest(100, 0.75, 0.92, 380,
                Sistema.TRIFASICO, 30000, 0.60);

        FatorPotenciaResultado r = service.calcular(req);

        assertThat(r.correnteCorrigidaA()).isLessThan(r.correnteAtualA());
        assertThat(r.reducaoCorrentePercentual()).isCloseTo((1 - 0.75 / 0.92) * 100, Offset.offset(0.1));
    }

    @Test
    @DisplayName("A potência aparente diminui e libera capacidade do transformador")
    void liberaCapacidadeInstalada() {
        var req = new FatorPotenciaRequest(200, 0.80, 0.95, 380,
                Sistema.TRIFASICO, 60000, 0.55);

        FatorPotenciaResultado r = service.calcular(req);

        assertThat(r.potenciaAparenteAtualKva()).isEqualTo(250.0);
        assertThat(r.potenciaAparenteCorrigidaKva()).isCloseTo(210.53, Offset.offset(0.01));
        assertThat(r.liberacaoCapacidadeKva()).isCloseTo(39.47, Offset.offset(0.01));
    }

    @Test
    @DisplayName("Fator de potência abaixo de 0,92 gera cobrança estimada por reativo")
    void estimaMultaQuandoAbaixoDoLimite() {
        var req = new FatorPotenciaRequest(100, 0.80, 0.92, 380,
                Sistema.TRIFASICO, 40000, 0.50);

        FatorPotenciaResultado r = service.calcular(req);

        assertThat(r.dentroDoLimiteLegal()).isFalse();
        assertThat(r.multaMensalEstimada()).isCloseTo(40000 * 0.50 * (0.92 / 0.80 - 1), Offset.offset(0.01));
        assertThat(r.multaAnualEstimada()).isCloseTo(r.multaMensalEstimada() * 12, Offset.offset(0.05));
    }

    @Test
    @DisplayName("Fator de potência já dentro da norma não gera cobrança")
    void semMultaQuandoDentroDoLimite() {
        var req = new FatorPotenciaRequest(100, 0.93, 0.98, 380,
                Sistema.TRIFASICO, 40000, 0.50);

        FatorPotenciaResultado r = service.calcular(req);

        assertThat(r.dentroDoLimiteLegal()).isTrue();
        assertThat(r.multaMensalEstimada()).isZero();
    }

    @Test
    @DisplayName("Fator desejado menor ou igual ao atual é rejeitado")
    void rejeitaAlvoInvalido() {
        var req = new FatorPotenciaRequest(100, 0.95, 0.92, 380,
                Sistema.TRIFASICO, 40000, 0.50);

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
