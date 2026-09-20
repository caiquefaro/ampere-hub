package com.amperehub.controller;

import com.amperehub.dto.CondutorDtos.CondutorRequest;
import com.amperehub.dto.CondutorDtos.CondutorResultado;
import com.amperehub.dto.ConsumoDtos.ConsumoRequest;
import com.amperehub.dto.ConsumoDtos.ConsumoResultado;
import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaRequest;
import com.amperehub.dto.FatorPotenciaDtos.FatorPotenciaResultado;
import com.amperehub.dto.SolarDtos.SolarRequest;
import com.amperehub.dto.SolarDtos.SolarResultado;
import com.amperehub.dto.TarifaDtos.TarifaBrancaRequest;
import com.amperehub.dto.TarifaDtos.TarifaBrancaResultado;
import com.amperehub.service.CondutorService;
import com.amperehub.service.ConsumoService;
import com.amperehub.service.FatorPotenciaService;
import com.amperehub.service.HistoricoService;
import com.amperehub.service.SolarService;
import com.amperehub.service.TarifaBrancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calculos")
@Tag(name = "Cálculos", description = "Motor de cálculo do Ampère Hub")
public class CalculoController {

    private final ConsumoService consumoService;
    private final TarifaBrancaService tarifaBrancaService;
    private final SolarService solarService;
    private final CondutorService condutorService;
    private final FatorPotenciaService fatorPotenciaService;
    private final HistoricoService historicoService;

    public CalculoController(ConsumoService consumoService,
                             TarifaBrancaService tarifaBrancaService,
                             SolarService solarService,
                             CondutorService condutorService,
                             FatorPotenciaService fatorPotenciaService,
                             HistoricoService historicoService) {
        this.consumoService = consumoService;
        this.tarifaBrancaService = tarifaBrancaService;
        this.solarService = solarService;
        this.condutorService = condutorService;
        this.fatorPotenciaService = fatorPotenciaService;
        this.historicoService = historicoService;
    }

    @PostMapping("/consumo")
    @Operation(summary = "Consumo mensal e custo por equipamento")
    public ConsumoResultado consumo(@Valid @RequestBody ConsumoRequest req) {
        ConsumoResultado resultado = consumoService.calcular(req);
        historicoService.registrar("consumo", "%.0f kWh/mês".formatted(resultado.consumoTotalKwhMes()), req, resultado);
        return resultado;
    }

    @PostMapping("/tarifa-branca")
    @Operation(summary = "Comparativo entre tarifa convencional e tarifa branca")
    public TarifaBrancaResultado tarifaBranca(@Valid @RequestBody TarifaBrancaRequest req) {
        TarifaBrancaResultado resultado = tarifaBrancaService.calcular(req);
        historicoService.registrar("tarifa-branca", resultado.modalidadeRecomendada(), req, resultado);
        return resultado;
    }

    @PostMapping("/solar")
    @Operation(summary = "Dimensionamento fotovoltaico e retorno do investimento")
    public SolarResultado solar(@Valid @RequestBody SolarRequest req) {
        SolarResultado resultado = solarService.calcular(req);
        historicoService.registrar("solar", "%.2f kWp".formatted(resultado.potenciaInstaladaKwp()), req, resultado);
        return resultado;
    }

    @PostMapping("/condutor")
    @Operation(summary = "Seção do condutor e disjuntor pela NBR 5410")
    public CondutorResultado condutor(@Valid @RequestBody CondutorRequest req) {
        CondutorResultado resultado = condutorService.calcular(req);
        historicoService.registrar("condutor", "%.1f mm² / %d A"
                .formatted(resultado.secaoAdotadaMm2(), resultado.disjuntorA()), req, resultado);
        return resultado;
    }

    @PostMapping("/fator-potencia")
    @Operation(summary = "Correção de fator de potência e banco de capacitores")
    public FatorPotenciaResultado fatorPotencia(@Valid @RequestBody FatorPotenciaRequest req) {
        FatorPotenciaResultado resultado = fatorPotenciaService.calcular(req);
        historicoService.registrar("fator-potencia", "%d kVAr".formatted(resultado.bancoComercialKvar()), req, resultado);
        return resultado;
    }
}
