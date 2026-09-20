package com.amperehub.service;

import com.amperehub.domain.MetodoInstalacao;
import com.amperehub.domain.Sistema;
import com.amperehub.dto.CondutorDtos.CondutorRequest;
import com.amperehub.dto.CondutorDtos.CondutorResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CondutorServiceTest {

    private final CondutorService service = new CondutorService();

    @Test
    @DisplayName("Corrente monofásica é P dividido por V vezes o fator de potência")
    void correnteMonofasica() {
        var req = new CondutorRequest(4400, 220, 1.0, Sistema.MONOFASICO,
                MetodoInstalacao.B1, 20, 4.0);
        assertThat(service.correnteProjeto(req)).isCloseTo(20.0, Offset.offset(0.01));
    }

    @Test
    @DisplayName("Corrente trifásica inclui a raiz de três no denominador")
    void correnteTrifasica() {
        var req = new CondutorRequest(10000, 380, 0.92, Sistema.TRIFASICO,
                MetodoInstalacao.B1, 30, 4.0);
        double esperado = 10000 / (Math.sqrt(3) * 380 * 0.92);
        assertThat(service.correnteProjeto(req)).isCloseTo(esperado, Offset.offset(0.01));
    }

    @Test
    @DisplayName("Circuito curto é dimensionado pela capacidade de condução")
    void circuitoCurtoDominadoPelaCorrente() {
        var req = new CondutorRequest(4400, 220, 1.0, Sistema.MONOFASICO,
                MetodoInstalacao.B1, 15, 4.0);

        CondutorResultado r = service.calcular(req);

        assertThat(r.correnteProjetoA()).isEqualTo(20.0);
        assertThat(r.secaoAdotadaMm2()).isEqualTo(2.5);
        assertThat(r.criterioDeterminante()).contains("Capacidade");
        assertThat(r.capacidadeConducaoA()).isGreaterThanOrEqualTo(r.correnteProjetoA());
    }

    @Test
    @DisplayName("Circuito longo é dimensionado pela queda de tensão")
    void circuitoLongoDominadoPelaQueda() {
        var req = new CondutorRequest(4400, 220, 1.0, Sistema.MONOFASICO,
                MetodoInstalacao.B1, 90, 4.0);

        CondutorResultado r = service.calcular(req);

        assertThat(r.secaoAdotadaMm2()).isGreaterThan(r.secaoPorCapacidadeMm2());
        assertThat(r.criterioDeterminante()).isEqualTo("Queda de tensão");
    }

    @Test
    @DisplayName("A queda de tensão final respeita o limite pedido")
    void quedaRespeitaOLimite() {
        var req = new CondutorRequest(8000, 220, 0.95, Sistema.MONOFASICO,
                MetodoInstalacao.C, 60, 3.0);

        CondutorResultado r = service.calcular(req);

        assertThat(r.quedaTensaoPercentual()).isLessThanOrEqualTo(3.0);
    }

    @Test
    @DisplayName("O disjuntor fica entre a corrente de projeto e a capacidade do cabo")
    void disjuntorEntreIbEIz() {
        var req = new CondutorRequest(10000, 380, 0.92, Sistema.TRIFASICO,
                MetodoInstalacao.B1, 25, 4.0);

        CondutorResultado r = service.calcular(req);

        assertThat(r.disjuntorA()).isGreaterThanOrEqualTo((int) Math.ceil(r.correnteProjetoA()));
        assertThat(r.disjuntorA()).isLessThanOrEqualTo((int) r.capacidadeConducaoA());
    }

    @Test
    @DisplayName("O método C conduz mais corrente que o B2 na mesma seção")
    void metodoCConduzMais() {
        var b2 = new CondutorRequest(6000, 220, 1.0, Sistema.MONOFASICO, MetodoInstalacao.B2, 10, 4.0);
        var c = new CondutorRequest(6000, 220, 1.0, Sistema.MONOFASICO, MetodoInstalacao.C, 10, 4.0);

        assertThat(service.calcular(c).capacidadeConducaoA())
                .isGreaterThanOrEqualTo(service.calcular(b2).capacidadeConducaoA());
    }

    @Test
    @DisplayName("Carga acima da tabela devolve erro de regra de negócio")
    void cargaAcimaDaTabela() {
        var req = new CondutorRequest(200000, 220, 1.0, Sistema.MONOFASICO,
                MetodoInstalacao.B1, 10, 4.0);

        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("95 mm²");
    }
}
