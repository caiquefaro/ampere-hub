package com.amperehub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro histórico de um cálculo executado. A entrada e o resultado são
 * guardados como JSON bruto para que todos os módulos usem a mesma tabela.
 */
@Entity
@Table(name = "simulacao")
public class Simulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(length = 120)
    private String titulo;

    @Lob
    @Column(nullable = false)
    private String entradaJson;

    @Lob
    @Column(nullable = false)
    private String resultadoJson;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    protected Simulacao() {
    }

    public Simulacao(String modulo, String titulo, String entradaJson, String resultadoJson) {
        this.modulo = modulo;
        this.titulo = titulo;
        this.entradaJson = entradaJson;
        this.resultadoJson = resultadoJson;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getModulo() {
        return modulo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getEntradaJson() {
        return entradaJson;
    }

    public String getResultadoJson() {
        return resultadoJson;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
