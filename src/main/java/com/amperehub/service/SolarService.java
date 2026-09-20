package com.amperehub.service;

import com.amperehub.dto.SolarDtos.SolarRequest;
import com.amperehub.dto.SolarDtos.SolarResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;

/**
 * Dimensiona um sistema fotovoltaico on-grid a partir do consumo médio
 * e da irradiação local, e estima investimento, economia e payback.
 */
@Service
public class SolarService {

    /** Área aproximada de um módulo fotovoltaico moderno, em m². */
    private static final double AREA_POR_MODULO_M2 = 2.3;

    /** Fator médio de emissão do Sistema Interligado Nacional, em kg CO2 por kWh. */
    private static final double FATOR_EMISSAO_KG_POR_KWH = 0.0385;

    private static final int DIAS_MES = 30;
    private static final double DEGRADACAO_ANUAL = 0.005;

    public SolarResultado calcular(SolarRequest req) {
        double compensavel = req.consumoMedioMensalKwh() - req.custoDisponibilidadeKwh();
        if (compensavel <= 0) {
            throw new RegraDeNegocioException(
                    "O consumo informado não supera o custo de disponibilidade. "
                            + "Nesse cenário a geração própria não traz retorno financeiro.");
        }

        double geracaoDiariaNecessaria = compensavel / DIAS_MES;
        double kwpNecessario = geracaoDiariaNecessaria / (req.horasSolPleno() * req.performanceRatio());

        int modulos = (int) Math.ceil(kwpNecessario * 1000.0 / req.potenciaModuloW());
        double kwpInstalado = modulos * req.potenciaModuloW() / 1000.0;

        double geracaoMensal = kwpInstalado * req.horasSolPleno() * req.performanceRatio() * DIAS_MES;
        double geracaoAnual = geracaoMensal * 12;
        double cobertura = geracaoMensal / req.consumoMedioMensalKwh() * 100.0;

        double investimento = kwpInstalado * 1000.0 * req.custoPorWattPico();
        double economiaMensal = Math.min(geracaoMensal, compensavel) * req.tarifaPorKwh();
        double economiaAnual = economiaMensal * 12;
        double payback = economiaMensal > 0 ? investimento / economiaMensal : 0.0;

        double economia25Anos = 0.0;
        for (int ano = 0; ano < 25; ano++) {
            economia25Anos += economiaAnual * Math.pow(1 - DEGRADACAO_ANUAL, ano);
        }

        return new SolarResultado(
                Arredondamento.duas(compensavel),
                Arredondamento.casas(kwpNecessario, 3),
                modulos,
                Arredondamento.casas(kwpInstalado, 3),
                Arredondamento.duas(geracaoMensal),
                Arredondamento.duas(geracaoAnual),
                Arredondamento.duas(cobertura),
                Arredondamento.duas(modulos * AREA_POR_MODULO_M2),
                Arredondamento.duas(investimento),
                Arredondamento.duas(economiaMensal),
                Arredondamento.duas(economiaAnual),
                Arredondamento.duas(payback),
                Arredondamento.duas(economia25Anos),
                Arredondamento.duas(geracaoAnual * FATOR_EMISSAO_KG_POR_KWH));
    }
}
