package com.amperehub.domain;

/**
 * Métodos de referência da NBR 5410 mais usados em instalações prediais.
 */
public enum MetodoInstalacao {

    B1("B1 — condutores em eletroduto embutido em alvenaria"),
    B2("B2 — cabo multipolar em eletroduto embutido em alvenaria"),
    C("C — cabo sobre parede ou em eletrocalha perfurada");

    private final String rotulo;

    MetodoInstalacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
