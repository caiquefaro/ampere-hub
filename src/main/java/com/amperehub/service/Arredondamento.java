package com.amperehub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento half-up usado em todos os resultados expostos pela API. */
public final class Arredondamento {

    private Arredondamento() {
    }

    public static double casas(double valor, int casas) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor).setScale(casas, RoundingMode.HALF_UP).doubleValue();
    }

    public static double duas(double valor) {
        return casas(valor, 2);
    }
}
