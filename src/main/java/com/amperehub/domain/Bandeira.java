package com.amperehub.domain;

/**
 * Bandeiras tarifárias da ANEEL. O adicional é aplicado por kWh consumido.
 * Valores de referência publicados por 100 kWh, convertidos aqui para R$/kWh.
 */
public enum Bandeira {

    VERDE("Verde", 0.0),
    AMARELA("Amarela", 0.01885),
    VERMELHA_1("Vermelha patamar 1", 0.04463),
    VERMELHA_2("Vermelha patamar 2", 0.07877);

    private final String rotulo;
    private final double adicionalPorKwh;

    Bandeira(String rotulo, double adicionalPorKwh) {
        this.rotulo = rotulo;
        this.adicionalPorKwh = adicionalPorKwh;
    }

    public String getRotulo() {
        return rotulo;
    }

    public double getAdicionalPorKwh() {
        return adicionalPorKwh;
    }
}
