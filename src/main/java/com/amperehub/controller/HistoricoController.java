package com.amperehub.controller;

import com.amperehub.service.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/historico")
@Tag(name = "Histórico", description = "Cálculos executados recentemente")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    public record ItemHistorico(Long id, String modulo, String titulo, Instant criadoEm) {
    }

    @GetMapping
    @Operation(summary = "Últimos 20 cálculos, opcionalmente filtrados por módulo")
    public List<ItemHistorico> listar(@RequestParam(required = false) String modulo) {
        return historicoService.ultimas(modulo).stream()
                .map(s -> new ItemHistorico(s.getId(), s.getModulo(), s.getTitulo(), s.getCriadoEm()))
                .toList();
    }
}
