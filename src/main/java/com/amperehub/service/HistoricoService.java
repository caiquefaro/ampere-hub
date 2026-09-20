package com.amperehub.service;

import com.amperehub.domain.Simulacao;
import com.amperehub.repository.SimulacaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/** Guarda cada cálculo executado para alimentar o painel de histórico. */
@Service
public class HistoricoService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    private final SimulacaoRepository repository;
    private final ObjectMapper mapper;

    public HistoricoService(SimulacaoRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void registrar(String modulo, String titulo, Object entrada, Object resultado) {
        try {
            repository.save(new Simulacao(
                    modulo,
                    titulo,
                    mapper.writeValueAsString(entrada),
                    mapper.writeValueAsString(resultado)));
        } catch (JsonProcessingException e) {
            log.warn("Cálculo do módulo {} não foi registrado no histórico", modulo, e);
        }
    }

    public List<Simulacao> ultimas(String modulo) {
        return modulo == null || modulo.isBlank()
                ? repository.findTop20ByOrderByCriadoEmDesc()
                : repository.findTop20ByModuloOrderByCriadoEmDesc(modulo);
    }
}
