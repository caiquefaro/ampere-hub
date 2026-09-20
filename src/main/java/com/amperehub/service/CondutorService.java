package com.amperehub.service;

import com.amperehub.domain.MetodoInstalacao;
import com.amperehub.domain.Sistema;
import com.amperehub.dto.CondutorDtos.CondutorRequest;
import com.amperehub.dto.CondutorDtos.CondutorResultado;
import com.amperehub.exception.RegraDeNegocioException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dimensiona condutor e disjuntor por dois critérios da NBR 5410:
 * capacidade de condução de corrente e limite de queda de tensão.
 * Prevalece sempre a maior seção entre os dois.
 */
@Service
public class CondutorService {

    /** Resistividade do cobre a 70 °C, em ohm.mm²/m. */
    private static final double RESISTIVIDADE_COBRE = 0.0178;

    private static final double[] SECOES = {1.5, 2.5, 4, 6, 10, 16, 25, 35, 50, 70, 95};

    private static final int[] DISJUNTORES = {6, 10, 16, 20, 25, 32, 40, 50, 63, 70, 80, 100, 125, 160, 200};

    /**
     * Capacidade de condução em ampères, para condutores de cobre com
     * isolação de PVC a 30 °C. Chave: metodo + numero de condutores carregados.
     */
    private static final Map<String, double[]> CAPACIDADES = new LinkedHashMap<>();

    static {
        CAPACIDADES.put("B1-2", new double[]{17.5, 24, 32, 41, 57, 76, 101, 125, 151, 192, 232});
        CAPACIDADES.put("B1-3", new double[]{15.5, 21, 28, 36, 50, 68, 89, 110, 134, 171, 207});
        CAPACIDADES.put("B2-2", new double[]{16.5, 23, 30, 38, 52, 69, 90, 111, 133, 168, 201});
        CAPACIDADES.put("B2-3", new double[]{15.0, 20, 27, 34, 46, 62, 80, 99, 118, 149, 179});
        CAPACIDADES.put("C-2", new double[]{19.5, 27, 36, 46, 63, 85, 112, 138, 168, 213, 258});
        CAPACIDADES.put("C-3", new double[]{17.5, 24, 32, 41, 57, 76, 96, 119, 144, 184, 223});
    }

    public double correnteProjeto(CondutorRequest req) {
        double denominador = req.tensaoV() * req.fatorPotencia();
        if (req.sistema().isTrifasico()) {
            denominador *= Math.sqrt(3);
        }
        return req.potenciaW() / denominador;
    }

    public CondutorResultado calcular(CondutorRequest req) {
        double ib = correnteProjeto(req);
        double[] capacidades = tabela(req.metodoInstalacao(), req.sistema());

        int idxCapacidade = -1;
        for (int i = 0; i < SECOES.length; i++) {
            if (capacidades[i] >= ib) {
                idxCapacidade = i;
                break;
            }
        }
        if (idxCapacidade < 0) {
            throw new RegraDeNegocioException(
                    "Corrente de projeto de %.1f A acima da maior seção da tabela (95 mm²). "
                            .formatted(ib) + "Considere dividir o circuito ou usar condutores em paralelo.");
        }

        double limiteQueda = req.quedaTensaoMaximaPercentual() / 100.0 * req.tensaoV();
        int idxQueda = -1;
        for (int i = 0; i < SECOES.length; i++) {
            if (quedaTensaoV(req, ib, SECOES[i]) <= limiteQueda) {
                idxQueda = i;
                break;
            }
        }
        if (idxQueda < 0) {
            throw new RegraDeNegocioException(
                    "Nenhuma seção até 95 mm² atende à queda de tensão em %.0f m. "
                            .formatted(req.comprimentoM())
                            + "Reduza o comprimento do circuito ou eleve a tensão de alimentação.");
        }

        int idxFinal = Math.max(idxCapacidade, idxQueda);
        double secao = SECOES[idxFinal];
        double iz = capacidades[idxFinal];
        double queda = quedaTensaoV(req, ib, secao);
        double quedaPercent = queda / req.tensaoV() * 100.0;

        int disjuntor = escolherDisjuntor(ib, iz);

        String criterio = idxQueda > idxCapacidade
                ? "Queda de tensão"
                : (idxQueda == idxCapacidade ? "Capacidade de condução e queda de tensão" : "Capacidade de condução");

        return new CondutorResultado(
                Arredondamento.duas(ib),
                SECOES[idxCapacidade],
                SECOES[idxQueda],
                secao,
                iz,
                Arredondamento.duas(queda),
                Arredondamento.duas(quedaPercent),
                disjuntor,
                criterio,
                observacao(criterio, req));
    }

    private double quedaTensaoV(CondutorRequest req, double ib, double secao) {
        double fator = req.sistema().isTrifasico() ? Math.sqrt(3) : 2.0;
        return fator * RESISTIVIDADE_COBRE * req.comprimentoM() * ib / secao;
    }

    private int escolherDisjuntor(double ib, double iz) {
        for (int in : DISJUNTORES) {
            if (in >= ib && in <= iz) {
                return in;
            }
        }
        throw new RegraDeNegocioException(
                "Não há disjuntor comercial que satisfaça Ib <= In <= Iz para esta combinação.");
    }

    private double[] tabela(MetodoInstalacao metodo, Sistema sistema) {
        String chave = metodo.name() + "-" + sistema.getCondutoresCarregados();
        double[] valores = CAPACIDADES.get(chave);
        if (valores == null) {
            throw new RegraDeNegocioException("Combinação de método e sistema não suportada: " + chave);
        }
        return valores;
    }

    private String observacao(String criterio, CondutorRequest req) {
        if (criterio.startsWith("Queda")) {
            return "O circuito de %.0f m foi dominado pela queda de tensão. Encurtar o trajeto costuma sair mais barato do que engrossar o cabo."
                    .formatted(req.comprimentoM());
        }
        return "Seção definida pela corrente. Confira fatores de correção de temperatura e agrupamento antes de fechar o projeto.";
    }
}
