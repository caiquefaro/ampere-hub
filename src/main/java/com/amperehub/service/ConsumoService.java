package com.amperehub.service;

import com.amperehub.dto.ConsumoDtos.ConsumoRequest;
import com.amperehub.dto.ConsumoDtos.ConsumoResultado;
import com.amperehub.dto.ConsumoDtos.EquipamentoRequest;
import com.amperehub.dto.ConsumoDtos.EquipamentoResultado;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Converte uma lista de cargas em consumo mensal e custo, aplicando
 * a bandeira tarifária vigente sobre a tarifa base informada.
 */
@Service
public class ConsumoService {

    public double consumoKwhMes(EquipamentoRequest e) {
        return e.potenciaW() * e.horasPorDia() * e.diasPorMes() * e.quantidade() / 1000.0;
    }

    public ConsumoResultado calcular(ConsumoRequest req) {
        double total = 0.0;
        List<double[]> brutos = new ArrayList<>();

        for (EquipamentoRequest e : req.equipamentos()) {
            double kwh = consumoKwhMes(e);
            total += kwh;
            brutos.add(new double[]{kwh});
        }

        double adicional = req.bandeira().getAdicionalPorKwh();
        double tarifaEfetiva = req.tarifaPorKwh() + adicional;

        List<EquipamentoResultado> detalhamento = new ArrayList<>();
        for (int i = 0; i < req.equipamentos().size(); i++) {
            EquipamentoRequest e = req.equipamentos().get(i);
            double kwh = brutos.get(i)[0];
            double participacao = total > 0 ? kwh / total * 100.0 : 0.0;
            detalhamento.add(new EquipamentoResultado(
                    e.nome(),
                    Arredondamento.duas(kwh),
                    Arredondamento.duas(kwh * tarifaEfetiva),
                    Arredondamento.duas(participacao)));
        }

        detalhamento.sort(Comparator.comparingDouble(EquipamentoResultado::consumoKwhMes).reversed());

        double custoEnergia = total * req.tarifaPorKwh();
        double custoBandeira = total * adicional;
        double custoTotal = custoEnergia + custoBandeira;

        EquipamentoResultado vilao = detalhamento.isEmpty() ? null : detalhamento.get(0);

        return new ConsumoResultado(
                Arredondamento.duas(total),
                Arredondamento.duas(custoEnergia),
                Arredondamento.duas(custoBandeira),
                Arredondamento.duas(custoTotal),
                Arredondamento.duas(custoTotal * 12),
                req.bandeira().getRotulo(),
                Arredondamento.casas(tarifaEfetiva, 5),
                vilao,
                detalhamento);
    }
}
