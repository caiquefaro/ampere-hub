# Ampère Hub

Plataforma web que reúne cinco cálculos do dia a dia de quem trabalha com instalações elétricas: consumo de energia, comparação entre tarifa convencional e tarifa branca, dimensionamento fotovoltaico, dimensionamento de condutor e disjuntor pela NBR 5410, e correção de fator de potência.

O motor de cálculo é escrito em Java com Spring Boot e exposto como API REST. A interface é HTML e CSS puros, sem framework de frontend.

	
Demonstração online	https://ampere-hub.onrender.com/
Documentação da API	https://ampere-hub.onrender.com/docs
Código-fonte	https://github.com/caiquefaro/ampere-hub

---

## Por que este projeto existe

A maioria dos cálculos elétricos ainda é feita em planilhas soltas, cada uma com sua própria convenção. O Ampère Hub reúne essas contas em um só lugar e, mais importante, mostra **qual critério determinou cada resultado**. No dimensionamento de condutores, por exemplo, a resposta não é só "4 mm²": é "10 mm², porque a queda de tensão ao longo de 60 metros pesou mais que a capacidade de condução, que pediria só 4 mm²".

## Os cinco módulos

| Módulo | O que resolve | Saídas principais |
|---|---|---|
| Consumo | Lista de cargas com potência, horas de uso e quantidade | kWh/mês, custo por equipamento, adicional de bandeira, maior vilão da conta |
| Tarifa branca | Consumo dividido nos postos ponta, intermediário e fora de ponta | Conta nas duas modalidades, diferença mensal e anual, recomendação |
| Solar | Consumo médio, irradiação local e custo por watt-pico | kWp, número de módulos, área, investimento, payback, CO₂ evitado |
| Condutor | Potência, tensão, comprimento e método de instalação | Seção em mm², disjuntor, queda de tensão, critério determinante |
| Fator de potência | Potência ativa e fator de potência medido | kVAr do banco, redução de corrente, capacidade liberada, cobrança evitada |

## Decisões técnicas que valem comentar

**Dois critérios em paralelo no dimensionamento de condutores.** A NBR 5410 exige que a seção atenda tanto à capacidade de condução quanto ao limite de queda de tensão. O serviço calcula os dois independentemente e adota a maior seção, devolvendo qual dos dois mandou. As tabelas de capacidade de condução (métodos B1, B2 e C, cobre com isolação de PVC a 30 °C) estão em um mapa estático.

**Bandeiras tarifárias como enum com dado embutido.** Cada bandeira carrega seu adicional em R$/kWh, convertido dos valores que a ANEEL publica por 100 kWh. Adicionar uma bandeira nova é uma linha.

**Degradação dos módulos na projeção de 25 anos.** Somar a economia anual vinte e cinco vezes superestima o retorno. A projeção aplica 0,5% de perda de rendimento por ano.

**Erros de cálculo têm status próprio.** Consumo abaixo do custo de disponibilidade, carga acima da maior seção da tabela ou fator de potência alvo menor que o atual não são bugs, são cenários válidos que a física ou a norma rejeitam. Eles sobem como `RegraDeNegocioException` e viram HTTP 422 com mensagem explicando o que fazer, em vez de 500.

**Histórico genérico.** Em vez de uma tabela por módulo, uma só tabela `simulacao` guarda entrada e resultado como JSON. Qualquer módulo novo entra sem migração de banco.

## Stack

- Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation
- H2 em desenvolvimento, PostgreSQL em produção
- JUnit 5 e AssertJ
- springdoc-openapi para a documentação interativa
- HTML5, CSS3 (grid, variáveis CSS, responsivo) e JavaScript sem dependências
- Docker multi-stage e GitHub Actions

## Como rodar

Pré-requisitos: JDK 21 e Maven 3.9.

```bash
git clone https://github.com/CaiqueFaro/ampere-hub.git
cd ampere-hub
mvn spring-boot:run
```

- Interface: <http://localhost:8080>
- Documentação da API: <http://localhost:8080/docs>
- Console do banco H2: <http://localhost:8080/h2>

### Com Docker

```bash
docker compose up --build
```

Sobe a aplicação com PostgreSQL.

### Testes

```bash
mvn test
```

## A API

Base: `/api/v1`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/calculos/consumo` | Consumo e custo por equipamento |
| POST | `/calculos/tarifa-branca` | Comparativo entre modalidades tarifárias |
| POST | `/calculos/solar` | Dimensionamento fotovoltaico |
| POST | `/calculos/condutor` | Seção do condutor e disjuntor |
| POST | `/calculos/fator-potencia` | Banco de capacitores |
| GET | `/historico` | Últimos 20 cálculos, com filtro opcional por `modulo` |

Exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/calculos/condutor \
  -H "Content-Type: application/json" \
  -d '{
    "potenciaW": 6000,
    "tensaoV": 220,
    "fatorPotencia": 1.0,
    "sistema": "MONOFASICO",
    "metodoInstalacao": "B1",
    "comprimentoM": 60,
    "quedaTensaoMaximaPercentual": 4
  }'
```

Resposta:

```json
{
  "correnteProjetoA": 27.27,
  "secaoPorCapacidadeMm2": 4.0,
  "secaoPorQuedaMm2": 10.0,
  "secaoAdotadaMm2": 10.0,
  "capacidadeConducaoA": 57.0,
  "quedaTensaoV": 5.83,
  "quedaTensaoPercentual": 2.65,
  "disjuntorA": 32,
  "criterioDeterminante": "Queda de tensão",
  "observacao": "O circuito de 60 m foi dominado pela queda de tensão. Encurtar o trajeto costuma sair mais barato do que engrossar o cabo."
}
```

## Estrutura

```
src/main/java/com/amperehub/
├── domain/       Bandeira, Sistema, MetodoInstalacao, Simulacao
├── dto/          records de entrada e saída, com Bean Validation
├── service/      o motor de cálculo — onde está a engenharia
├── controller/   endpoints REST
├── repository/   acesso ao banco
└── exception/    erros de regra de negócio e handler global

src/main/resources/static/
├── index.html
├── css/style.css
└── js/app.js
```

## Referências

- ABNT NBR 5410 — Instalações elétricas de baixa tensão
- Resolução Normativa ANEEL nº 1.000/2021 — regras gerais de fornecimento
- Lei nº 14.300/2022 — marco legal da microgeração e minigeração distribuída
- Resolução Normativa ANEEL nº 1.011/2022 — bandeiras tarifárias

## Aviso

Os resultados são estimativas de estudo preliminar. Não substituem projeto assinado por profissional habilitado, nem dispensam a consulta às normas ABNT vigentes e às regras da distribuidora local. Tabelas de capacidade de condução não aplicam fatores de correção por temperatura ambiente e agrupamento de circuitos.

## Licença

MIT.
