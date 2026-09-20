package com.amperehub.service;

import com.amperehub.dto.TarifaDtos.TarifaBrancaRequest;
import com.amperehub.dto.TarifaDtos.TarifaBrancaResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;

/**
 * Compara a tarifa convencional com a tarifa branca, que cobra preços
 * diferentes nos postos ponta, intermediário e fora de ponta.
 */
@Service
public class TarifaBrancaService {

    public TarifaBrancaResultado calcular(TarifaBrancaRequest req) {
        double total = req.consumoPontaKwh() + req.consumoIntermediarioKwh() + req.consumoForaPontaKwh();
        if (total <= 0) {
            throw new RegraDeNegocioException("Informe o consumo de pelo menos um posto tarifário.");
        }

        double convencional = total * req.tarifaConvencional();
        double branca = req.consumoPontaKwh() * req.tarifaPonta()
                + req.consumoIntermediarioKwh() * req.tarifaIntermediaria()
                + req.consumoForaPontaKwh() * req.tarifaForaPonta();

        double diferenca = convencional - branca;
        double percentual = convencional > 0 ? diferenca / convencional * 100.0 : 0.0;
        double percentualForaPonta = req.consumoForaPontaKwh() / total * 100.0;

        String recomendada = diferenca > 0 ? "BRANCA" : "CONVENCIONAL";
        String leitura = montarLeitura(diferenca, percentualForaPonta);

        return new TarifaBrancaResultado(
                Arredondamento.duas(total),
                Arredondamento.duas(convencional),
                Arredondamento.duas(branca),
                Arredondamento.duas(Math.abs(diferenca)),
                Arredondamento.duas(Math.abs(percentual)),
                Arredondamento.duas(Math.abs(diferenca) * 12),
                recomendada,
                Arredondamento.duas(percentualForaPonta),
                leitura);
    }

    private String montarLeitura(double diferenca, double percentualForaPonta) {
        if (diferenca > 0) {
            return "A tarifa branca sai mais barata neste perfil: %.0f%% do consumo está fora de ponta."
                    .formatted(percentualForaPonta);
        }
        if (Math.abs(diferenca) < 0.01) {
            return "As duas modalidades custam praticamente o mesmo. Vale manter a convencional pela previsibilidade.";
        }
        return "A convencional sai mais barata. O consumo em ponta ainda pesa demais para a tarifa branca compensar.";
    }
}
