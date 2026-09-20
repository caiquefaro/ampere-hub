package com.amperehub.repository;

import com.amperehub.domain.Simulacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulacaoRepository extends JpaRepository<Simulacao, Long> {

    List<Simulacao> findTop20ByOrderByCriadoEmDesc();

    List<Simulacao> findTop20ByModuloOrderByCriadoEmDesc(String modulo);
}
