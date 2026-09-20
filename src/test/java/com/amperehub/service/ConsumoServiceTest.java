package com.amperehub.service;

import com.amperehub.domain.Bandeira;
import com.amperehub.dto.ConsumoDtos.ConsumoRequest;
import com.amperehub.dto.ConsumoDtos.ConsumoResultado;
import com.amperehub.dto.ConsumoDtos.EquipamentoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumoServiceTest {

    private final ConsumoService service = new ConsumoService();

    @Test
    @DisplayName("Chuveiro de 5500 W por 0,5 h/dia em 30 dias consome 82,5 kWh")
    void calculaConsumoDeUmEquipamento() {
        var chuveiro = new EquipamentoRequest("Chuveiro", 5500, 0.5, 30, 1);
        assertThat(service.consumoKwhMes(chuveiro)).isEqualTo(82.5);
    }

    @Test
    @DisplayName("Quantidade multiplica o consumo da carga")
    void multiplicaPelaQuantidade() {
        var lampadas = new EquipamentoRequest("Lâmpada LED", 10, 5, 30, 8);
        assertThat(service.consumoKwhMes(lampadas)).isEqualTo(12.0);
    }

    @Test
    @DisplayName("Bandeira verde não acrescenta custo à tarifa base")
    void bandeiraVerdeNaoCobraAdicional() {
        var req = new ConsumoRequest(
                List.of(new EquipamentoRequest("Geladeira", 150, 24, 30, 1)),
                0.80, Bandeira.VERDE);

        ConsumoResultado r = service.calcular(req);

        assertThat(r.consumoTotalKwhMes()).isEqualTo(108.0);
        assertThat(r.custoBandeira()).isZero();
        assertThat(r.custoTotalMes()).isEqualTo(86.40);
    }

    @Test
    @DisplayName("Bandeira vermelha 2 aplica R$ 0,07877 por kWh sobre a tarifa")
    void bandeiraVermelhaAplicaAdicional() {
        var req = new ConsumoRequest(
                List.of(new EquipamentoRequest("Ar-condicionado", 1000, 8, 30, 1)),
                0.90, Bandeira.VERMELHA_2);

        ConsumoResultado r = service.calcular(req);

        assertThat(r.consumoTotalKwhMes()).isEqualTo(240.0);
        assertThat(r.custoEnergia()).isEqualTo(216.0);
        assertThat(r.custoBandeira()).isEqualTo(18.90);
        assertThat(r.custoTotalMes()).isEqualTo(234.90);
        assertThat(r.custoTotalAno()).isEqualTo(2818.86);
    }

    @Test
    @DisplayName("O detalhamento vem ordenado do maior para o menor consumo")
    void ordenaDetalhamentoPorConsumo() {
        var req = new ConsumoRequest(List.of(
                new EquipamentoRequest("Lâmpada", 10, 5, 30, 1),
                new EquipamentoRequest("Chuveiro", 5500, 0.5, 30, 1),
                new EquipamentoRequest("Geladeira", 150, 24, 30, 1)),
                1.0, Bandeira.VERDE);

        ConsumoResultado r = service.calcular(req);

        assertThat(r.detalhamento()).extracting("nome")
                .containsExactly("Geladeira", "Chuveiro", "Lâmpada");
        assertThat(r.maiorVilao().nome()).isEqualTo("Geladeira");
    }

    @Test
    @DisplayName("As participações percentuais somam 100")
    void participacoesSomamCem() {
        var req = new ConsumoRequest(List.of(
                new EquipamentoRequest("A", 100, 10, 30, 1),
                new EquipamentoRequest("B", 300, 10, 30, 1)),
                1.0, Bandeira.VERDE);

        ConsumoResultado r = service.calcular(req);

        double soma = r.detalhamento().stream()
                .mapToDouble(e -> e.participacaoPercentual()).sum();
        assertThat(soma).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.01));
    }
}
