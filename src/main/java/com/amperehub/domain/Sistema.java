package com.amperehub.domain;

public enum Sistema {

    MONOFASICO("Monofásico", 2),
    BIFASICO("Bifásico", 2),
    TRIFASICO("Trifásico", 3);

    private final String rotulo;
    private final int condutoresCarregados;

    Sistema(String rotulo, int condutoresCarregados) {
        this.rotulo = rotulo;
        this.condutoresCarregados = condutoresCarregados;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getCondutoresCarregados() {
        return condutoresCarregados;
    }

    public boolean isTrifasico() {
        return this == TRIFASICO;
    }
}
