package com.amperehub.service;

import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaRequest;
import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;

/**
 * Calcula a potência reativa necessária para corrigir o fator de potência
 * e estima a cobrança por excedente de reativo evitada.
 */
@Service
public class FatorPotenciaService {

    /** Fator de potência mínimo exigido pela regulação brasileira. */
    private static final double FP_REFERENCIA = 0.92;

    /** Degrau usual de banco de capacitores automático, em kVAr. */
    private static final int DEGRAU_KVAR = 5;

    public FatorPotenciaResultado calcular(FatorPotenciaRequest req) {
        if (req.fatorPotenciaDesejado() <= req.fatorPotenciaAtual()) {
            throw new RegraDeNegocioException(
                    "O fator de potência desejado precisa ser maior que o atual.");
        }

        double p = req.potenciaAtivaKw();
        double fp1 = req.fatorPotenciaAtual();
        double fp2 = req.fatorPotenciaDesejado();

        double tg1 = Math.tan(Math.acos(fp1));
        double tg2 = Math.tan(Math.acos(fp2));

        double qc = p * (tg1 - tg2);
        int banco = (int) (Math.ceil(qc / DEGRAU_KVAR) * DEGRAU_KVAR);

        double sAtual = p / fp1;
        double sCorrigida = p / fp2;

        double fatorSistema = req.sistema().isTrifasico() ? Math.sqrt(3) : 1.0;
        double iAtual = sAtual * 1000.0 / (fatorSistema * req.tensaoV());
        double iCorrigida = sCorrigida * 1000.0 / (fatorSistema * req.tensaoV());
        double reducao = (iAtual - iCorrigida) / iAtual * 100.0;

        boolean dentroDoLimite = fp1 >= FP_REFERENCIA;
        double multaMensal = 0.0;
        if (!dentroDoLimite) {
            multaMensal = req.consumoMensalKwh() * req.tarifaPorKwh() * (FP_REFERENCIA / fp1 - 1.0);
        }

        return new FatorPotenciaResultado(
                Arredondamento.duas(sAtual),
                Arredondamento.duas(sCorrigida),
                Arredondamento.duas(p * tg1),
                Arredondamento.duas(p * tg2),
                Arredondamento.duas(qc),
                banco,
                Arredondamento.duas(iAtual),
                Arredondamento.duas(iCorrigida),
                Arredondamento.duas(reducao),
                Arredondamento.duas(sAtual - sCorrigida),
                Arredondamento.duas(multaMensal),
                Arredondamento.duas(multaMensal * 12),
                dentroDoLimite,
                diagnostico(dentroDoLimite, fp1, reducao));
    }

    private String diagnostico(boolean dentroDoLimite, double fp1, double reducao) {
        if (dentroDoLimite) {
            return "Fator de potência de %.2f já atende ao mínimo de 0,92. A correção libera capacidade do transformador e reduz perdas, mas não evita cobrança."
                    .formatted(fp1);
        }
        return "Fator de potência de %.2f está abaixo do mínimo de 0,92 e gera cobrança por excedente de reativo. A correção ainda reduz a corrente do alimentador em %.0f%%."
                .formatted(fp1, reducao);
    }
}
