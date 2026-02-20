# albion-flip-api

Backend API em **Node.js + Express** para consultar preços do Albion Online Data Project e calcular oportunidades de arbitragem (flip) entre cidades e o Black Market.

## Funcionalidades

- Consulta de preços para **vários itens** em múltiplas cidades.
- Endpoint para cálculo de **flips** (arbitragem):
  - menor `sell_price_min` nas cidades informadas;
  - maior `buy_price_max` no **Black Market**;
  - lucro estimado com desconto de fees configuráveis.
- **Cache em memória** simples com TTL para reduzir chamadas repetidas e ajudar a respeitar rate limits.

## Estrutura de pastas

```text
albion-flip-api/
├── cache/
│   └── memoryCache.js
├── routes/
│   └── apiRoutes.js
├── services/
│   └── albionService.js
├── utils/
│   ├── feeCalculator.js
│   └── parseQuery.js
├── index.js
├── server.js
├── package.json
└── README.md
```

## Pré-requisitos

- Node.js 18+
- npm 9+

## Instalação

```bash
npm install
```

## Configuração

Crie um arquivo `.env` na raiz (opcional). Exemplo:

```env
PORT=3000
ALBION_API_BASE_URL=https://www.albion-online-data.com
ALBION_ITEM_QUALITY=2
CACHE_TTL_MS=30000

# Fee estimada = sell_price_cidade * FEE_RATE + FEE_FLAT
FEE_RATE=0.065
FEE_FLAT=0

BLACK_MARKET_LOCATION=Black Market
```

### Variáveis de ambiente

- `PORT`: porta local do servidor.
- `ALBION_API_BASE_URL`: base da API pública do Albion Data Project.
- `ALBION_ITEM_QUALITY`: qualidade usada na query (`qualities`).
- `CACHE_TTL_MS`: tempo de vida do cache em milissegundos.
- `FEE_RATE`: fee percentual para estimativa de lucro (ex.: `0.065` = 6,5%).
- `FEE_FLAT`: fee fixa somada à fee percentual.
- `BLACK_MARKET_LOCATION`: nome da localização de BM (default: `Black Market`).

## Como rodar

### Desenvolvimento

```bash
npm run dev
```

### Produção/local simples

```bash
npm start
```

Server default: `http://localhost:3000`

## Endpoints

### 1) GET `/api/prices`

Retorna preços de todos os itens passados para as cidades informadas.

#### Query params

- `items` (obrigatório): CSV com IDs dos itens.
- `locations` (obrigatório): CSV com cidades/localizações.

#### Exemplo

```bash
curl "http://localhost:3000/api/prices?items=T4_BAG,T4_CAPE&locations=Bridgewatch,Martlock"
```

---

### 2) GET `/api/flips`

Busca oportunidades de arbitragem por item.

#### Regra

`lucro = buy_price_BM - sell_price_cidade - fee_estimada`

onde:

`fee_estimada = sell_price_cidade * FEE_RATE + FEE_FLAT`

#### Query params

- `items` (obrigatório): CSV com IDs dos itens.
- `locations` (obrigatório): CSV com cidades para compra (o Black Market é acrescentado internamente para consulta, quando necessário).

#### Exemplo

```bash
curl "http://localhost:3000/api/flips?items=T4_BAG,T4_CAPE&locations=Bridgewatch,Martlock,Thetford"
```

#### Resposta (resumo)

- `flips`: array ordenado por `estimatedProfit` desc.
- `fee`: configuração de fee aplicada no cálculo.
- `cached`: indica se os dados vieram de cache.

## Observações

- O serviço utiliza dados públicos e depende de disponibilidade/rate limit da API externa.
- O cache é em memória local do processo (não compartilhado entre múltiplas instâncias).
