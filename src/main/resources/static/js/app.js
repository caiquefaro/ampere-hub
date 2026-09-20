/* Ampère Hub — camada de apresentação: coleta os formulários,
   chama a API Java e desenha os resultados. */

const API = '/api/v1';

const reais = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const numero = (casas = 2) => new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: casas, maximumFractionDigits: casas
});

const brl = v => reais.format(v ?? 0);
const num = (v, casas = 2) => numero(casas).format(v ?? 0);

/* ---------- utilidades de rede ---------- */

async function chamar(rota, corpo) {
  const resposta = await fetch(`${API}/calculos/${rota}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(corpo)
  });
  const dados = await resposta.json();
  if (!resposta.ok) {
    throw Object.assign(new Error(dados.mensagem || 'O cálculo não pôde ser concluído.'),
      { detalhes: dados.detalhes || [] });
  }
  return dados;
}

function lerFormulario(form) {
  const dados = {};
  new FormData(form).forEach((valor, chave) => {
    const numerico = Number(valor);
    dados[chave] = valor !== '' && !Number.isNaN(numerico) ? numerico : valor;
  });
  return dados;
}

function mostrarErro(alvo, erro) {
  const itens = (erro.detalhes || []).map(d => `<li>${d}</li>`).join('');
  alvo.innerHTML = `<div class="erro"><p>${erro.message}</p>${itens ? `<ul>${itens}</ul>` : ''}</div>`;
}

function conectar(idForm, idSaida, rota, montarCorpo, desenhar) {
  const form = document.getElementById(idForm);
  const saida = document.getElementById(idSaida);
  form.addEventListener('submit', async evento => {
    evento.preventDefault();
    saida.innerHTML = '<p class="carregando">Calculando…</p>';
    try {
      const resultado = await chamar(rota, montarCorpo(form));
      saida.innerHTML = desenhar(resultado);
    } catch (erro) {
      mostrarErro(saida, erro);
    }
  });
}

const metrica = (rotulo, valor, unidade = '', acento = false) => `
  <div class="metrica${acento ? ' acento' : ''}">
    <dt>${rotulo}</dt>
    <dd>${valor}${unidade ? `<span class="unidade">${unidade}</span>` : ''}</dd>
  </div>`;

const destaque = (rotulo, valor, unidade, nota) => `
  <div class="destaque">
    <p class="rotulo">${rotulo}</p>
    <p class="numero">${valor}${unidade ? `<span class="unidade">${unidade}</span>` : ''}</p>
    ${nota ? `<p class="nota">${nota}</p>` : ''}
  </div>`;

/* ---------- módulo: consumo ---------- */

const SUGESTOES = [
  { nome: 'Chuveiro elétrico', potenciaW: 5500, horasPorDia: 0.6, diasPorMes: 30, quantidade: 1 },
  { nome: 'Geladeira', potenciaW: 150, horasPorDia: 24, diasPorMes: 30, quantidade: 1 },
  { nome: 'Ar-condicionado 12.000 BTU', potenciaW: 1100, horasPorDia: 6, diasPorMes: 30, quantidade: 1 }
];

const lista = document.getElementById('lista-equipamentos');
let sugestaoAtual = 0;

function adicionarEquipamento(dados) {
  const base = dados || SUGESTOES[sugestaoAtual % SUGESTOES.length];
  sugestaoAtual++;

  const linha = document.createElement('div');
  linha.className = 'equipamento';
  linha.innerHTML = `
    <button type="button" class="remover" aria-label="Remover equipamento">&times;</button>
    <label class="nome-campo">Equipamento
      <input type="text" data-campo="nome" value="${base.nome}" maxlength="60" required>
    </label>
    <label>Potência (W)
      <input type="number" data-campo="potenciaW" value="${base.potenciaW}" step="10" min="1" required>
    </label>
    <label>Horas por dia
      <input type="number" data-campo="horasPorDia" value="${base.horasPorDia}" step="0.1" min="0" max="24" required>
    </label>
    <label>Dias por mês
      <input type="number" data-campo="diasPorMes" value="${base.diasPorMes}" step="1" min="1" max="31" required>
    </label>
    <label>Quantidade
      <input type="number" data-campo="quantidade" value="${base.quantidade}" step="1" min="1" required>
    </label>`;

  linha.querySelector('.remover').addEventListener('click', () => {
    if (lista.children.length > 1) linha.remove();
  });

  lista.appendChild(linha);
}

document.getElementById('add-equipamento').addEventListener('click', () => adicionarEquipamento());
SUGESTOES.forEach(s => adicionarEquipamento(s));

function corpoConsumo(form) {
  const equipamentos = [...lista.querySelectorAll('.equipamento')].map(linha => {
    const item = {};
    linha.querySelectorAll('[data-campo]').forEach(campo => {
      item[campo.dataset.campo] = campo.type === 'number' ? Number(campo.value) : campo.value;
    });
    return item;
  });
  const geral = lerFormulario(form);
  return { equipamentos, tarifaPorKwh: geral.tarifaPorKwh, bandeira: geral.bandeira };
}

function desenharConsumo(r) {
  const classeBandeira = r.bandeira === 'Verde' ? 'ok'
    : r.bandeira === 'Amarela' ? 'atencao' : 'alerta';

  const linhas = r.detalhamento.map(e => `
    <tr>
      <td>${e.nome}
        <span class="barra-fundo"><i style="width:${e.participacaoPercentual}%"></i></span>
      </td>
      <td class="num">${num(e.consumoKwhMes, 1)}</td>
      <td class="num">${brl(e.custoMes)}</td>
      <td class="num">${num(e.participacaoPercentual, 1)}%</td>
    </tr>`).join('');

  return `
    ${destaque('Custo mensal estimado', brl(r.custoTotalMes), '',
      `${num(r.consumoTotalKwhMes, 1)} kWh no mês · ${brl(r.custoTotalAno)} por ano`)}
    <dl class="grade-metricas">
      ${metrica('Energia', brl(r.custoEnergia))}
      ${metrica('Adicional de bandeira', brl(r.custoBandeira), '', r.custoBandeira > 0)}
      ${metrica('Tarifa efetiva', num(r.tarifaEfetivaPorKwh, 5), 'R$/kWh')}
      ${metrica('Bandeira', `<span class="selo ${classeBandeira}">${r.bandeira}</span>`)}
    </dl>
    <table class="tabela">
      <thead><tr><th>Equipamento</th><th class="num">kWh/mês</th><th class="num">Custo</th><th class="num">Fatia</th></tr></thead>
      <tbody>${linhas}</tbody>
    </table>
    ${r.maiorVilao ? `<div class="comentario">
      <strong>${r.maiorVilao.nome}</strong> responde por ${num(r.maiorVilao.participacaoPercentual, 1)}% do consumo,
      ou ${brl(r.maiorVilao.custoMes)} por mês. É onde qualquer troca de equipamento rende mais.
    </div>` : ''}`;
}

/* ---------- módulo: tarifa branca ---------- */

function desenharTarifa(r) {
  const branca = r.modalidadeRecomendada === 'BRANCA';
  return `
    ${destaque(branca ? 'Economia com a tarifa branca' : 'Economia mantendo a convencional',
      brl(r.diferencaMensal), 'por mês',
      `${brl(r.diferencaAnual)} no ano · diferença de ${num(r.diferencaPercentual, 1)}%`)}
    <dl class="grade-metricas">
      ${metrica('Modalidade recomendada', branca ? 'Tarifa branca' : 'Convencional', '', true)}
      ${metrica('Conta na convencional', brl(r.custoConvencional))}
      ${metrica('Conta na branca', brl(r.custoBranca))}
      ${metrica('Consumo total', num(r.consumoTotalKwh, 0), 'kWh')}
      ${metrica('Fora de ponta', num(r.percentualForaPonta, 1), '%')}
    </dl>
    <div class="comentario">${r.leitura}</div>`;
}

/* ---------- módulo: solar ---------- */

function desenharSolar(r) {
  const anos = Math.floor(r.paybackMeses / 12);
  const meses = Math.round(r.paybackMeses % 12);
  return `
    ${destaque('Sistema dimensionado', num(r.potenciaInstaladaKwp, 2), 'kWp',
      `${r.quantidadeModulos} módulos · ${num(r.areaEstimadaM2, 1)} m² de telhado`)}
    <dl class="grade-metricas">
      ${metrica('Geração mensal', num(r.geracaoMensalKwh, 0), 'kWh')}
      ${metrica('Cobertura do consumo', num(r.coberturaPercentual, 0), '%')}
      ${metrica('Investimento', brl(r.investimentoEstimado))}
      ${metrica('Economia mensal', brl(r.economiaMensal), '', true)}
      ${metrica('Retorno', `${anos}a ${meses}m`, '', true)}
      ${metrica('Economia em 25 anos', brl(r.economia25Anos))}
      ${metrica('Geração anual', num(r.geracaoAnualKwh, 0), 'kWh')}
      ${metrica('CO₂ evitado por ano', num(r.co2EvitadoAnualKg, 0), 'kg')}
    </dl>
    <div class="comentario">
      O consumo compensável é de ${num(r.consumoCompensavelKwh, 0)} kWh: o custo de disponibilidade
      continua sendo cobrado mesmo com o sistema gerando. A economia em 25 anos já considera
      0,5% de degradação dos módulos por ano.
    </div>`;
}

/* ---------- módulo: condutor ---------- */

function desenharCondutor(r) {
  return `
    ${destaque('Seção adotada', num(r.secaoAdotadaMm2, 1), 'mm²',
      `Disjuntor de ${r.disjuntorA} A · critério determinante: ${r.criterioDeterminante.toLowerCase()}`)}
    <dl class="grade-metricas">
      ${metrica('Corrente de projeto', num(r.correnteProjetoA, 1), 'A')}
      ${metrica('Capacidade do cabo', num(r.capacidadeConducaoA, 1), 'A')}
      ${metrica('Por capacidade', num(r.secaoPorCapacidadeMm2, 1), 'mm²')}
      ${metrica('Por queda de tensão', num(r.secaoPorQuedaMm2, 1), 'mm²')}
      ${metrica('Queda de tensão', num(r.quedaTensaoV, 2), 'V')}
      ${metrica('Queda percentual', num(r.quedaTensaoPercentual, 2), '%', true)}
    </dl>
    <table class="tabela">
      <thead><tr><th>Verificação</th><th class="num">Valor</th><th>Situação</th></tr></thead>
      <tbody>
        <tr><td>Ib ≤ In</td><td class="num">${num(r.correnteProjetoA, 1)} ≤ ${r.disjuntorA} A</td>
            <td><span class="selo ok">Atende</span></td></tr>
        <tr><td>In ≤ Iz</td><td class="num">${r.disjuntorA} ≤ ${num(r.capacidadeConducaoA, 1)} A</td>
            <td><span class="selo ok">Atende</span></td></tr>
        <tr><td>Queda de tensão</td><td class="num">${num(r.quedaTensaoPercentual, 2)}%</td>
            <td><span class="selo ok">Dentro do limite</span></td></tr>
      </tbody>
    </table>
    <div class="comentario">${r.observacao}</div>`;
}

/* ---------- módulo: fator de potência ---------- */

function desenharReativo(r) {
  const selo = r.dentroDoLimiteLegal
    ? '<span class="selo ok">Dentro da norma</span>'
    : '<span class="selo alerta">Abaixo de 0,92</span>';

  return `
    ${destaque('Banco de capacitores', r.bancoComercialKvar, 'kVAr',
      `Cálculo exato: ${num(r.capacitorNecessarioKvar, 2)} kVAr, arredondado para o degrau comercial de 5 kVAr`)}
    <dl class="grade-metricas">
      ${metrica('Situação atual', selo)}
      ${metrica('Corrente antes', num(r.correnteAtualA, 1), 'A')}
      ${metrica('Corrente depois', num(r.correnteCorrigidaA, 1), 'A', true)}
      ${metrica('Redução de corrente', num(r.reducaoCorrentePercentual, 1), '%', true)}
      ${metrica('Potência aparente antes', num(r.potenciaAparenteAtualKva, 1), 'kVA')}
      ${metrica('Potência aparente depois', num(r.potenciaAparenteCorrigidaKva, 1), 'kVA')}
      ${metrica('Capacidade liberada', num(r.liberacaoCapacidadeKva, 1), 'kVA')}
      ${metrica('Cobrança evitada por ano', brl(r.multaAnualEstimada), '', r.multaAnualEstimada > 0)}
    </dl>
    <div class="comentario">${r.diagnostico}</div>`;
}

/* ---------- ligações ---------- */

conectar('form-consumo', 'saida-consumo', 'consumo', corpoConsumo, desenharConsumo);
conectar('form-tarifa', 'saida-tarifa', 'tarifa-branca', lerFormulario, desenharTarifa);
conectar('form-solar', 'saida-solar', 'solar', lerFormulario, desenharSolar);
conectar('form-condutor', 'saida-condutor', 'condutor', lerFormulario, desenharCondutor);
conectar('form-reativo', 'saida-reativo', 'fator-potencia', lerFormulario, desenharReativo);

/* ---------- diagrama unifilar como navegação ---------- */

document.querySelectorAll('.no').forEach(no => {
  no.setAttribute('tabindex', '0');
  no.setAttribute('role', 'link');
  const ir = () => document.getElementById(no.dataset.alvo)?.scrollIntoView({ block: 'start' });
  no.addEventListener('click', ir);
  no.addEventListener('keydown', e => {
    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); ir(); }
  });
});

/* ---------- histórico ---------- */

const MODULOS = {
  'consumo': 'Consumo',
  'tarifa-branca': 'Tarifa branca',
  'solar': 'Solar',
  'condutor': 'Condutor',
  'fator-potencia': 'Fator de potência'
};

document.getElementById('carregar-historico').addEventListener('click', async () => {
  const saida = document.getElementById('saida-historico');
  saida.innerHTML = '<p class="carregando">Buscando registros…</p>';
  try {
    const resposta = await fetch(`${API}/historico`);
    const itens = await resposta.json();
    if (!itens.length) {
      saida.innerHTML = '<p class="vazio">Nenhum cálculo registrado ainda. Rode um dos módulos acima.</p>';
      return;
    }
    const linhas = itens.map(i => `
      <tr>
        <td>${MODULOS[i.modulo] || i.modulo}</td>
        <td class="num">${i.titulo ?? '—'}</td>
        <td class="num">${new Date(i.criadoEm).toLocaleString('pt-BR')}</td>
      </tr>`).join('');
    saida.innerHTML = `<table class="tabela">
      <thead><tr><th>Módulo</th><th class="num">Resultado</th><th class="num">Quando</th></tr></thead>
      <tbody>${linhas}</tbody></table>`;
  } catch {
    saida.innerHTML = '<div class="erro"><p>Não foi possível ler o histórico. Verifique se a aplicação está no ar.</p></div>';
  }
});
