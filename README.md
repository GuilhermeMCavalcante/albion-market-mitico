# Albion Market Mitico (Java 17 + Maven)

Projeto para baixar o catálogo de itens do Albion Online e consultar preços na API pública do Albion Online Data.

## Fonte dos dados
- Catálogo de itens (`items.json`): `https://raw.githubusercontent.com/ao-data/ao-bin-dumps/master/formatted/items.json`
- Preços (lote): `https://www.albion-online-data.com/api/v2/stats/prices/{item1,item2,...}.json?locations=...&qualities=...`

> Observação: o endpoint de preços em lote reduz o número de requisições e ajuda com rate limit.

## Requisitos
- Java 17+
- Maven 3.9+

## Como rodar
```bash
mvn clean test
mvn -q exec:java
# alternativa
mvn -q spring-boot:run
```

Saídas padrão:
- CSV: `./output/prices.csv`
- SQLite: `./output/prices.db`

## Exemplos de comando (CLI)
```bash
mvn -q exec:java
# alternativa
mvn -q spring-boot:run -Dexec.args="--cities=Bridgewatch,Martlock --qualities=1,2 --enchantments=0,1,2,3 --concurrency=8 --ratePerMinute=180 --ratePer5Minutes=300 --out=./output/precos.csv --db=./output/precos.db --sinceMinutes=120"
```

Argumentos suportados:
- `--cities=Bridgewatch,Martlock,...`
- `--qualities=1,2,3,4,5`
- `--enchantments=0,1,2,3`
- `--concurrency=6`
- `--ratePerMinute=180`
- `--ratePer5Minutes=300`
- `--out=./output/prices.csv`
- `--db=./output/prices.db`
- `--sinceMinutes=120` (opcional, filtro local por timestamp mais recente de buy/sell)

## Configuração (`application.properties`)
Arquivo exemplo em `src/main/resources/application.properties`.
Também é possível sobrescrever por variáveis de ambiente com os mesmos nomes em UPPERCASE.

## Estrutura do projeto
- `com.albion.market.Main`: orquestração
- `config/AppConfig`: leitura de configuração
- `http/HttpJsonClient`: HTTP + retry/backoff + timeout
- `service/ItemCatalogLoader`: download/parse do catálogo
- `service/PriceApiService`: integração com endpoint de preços
- `service/MarketCollectorService`: chunking + concorrência + progresso
- `persistence/CsvPriceWriter`: exportação CSV
- `persistence/SqlitePriceRepository`: criação/upsert em SQLite
- `util/SimpleRateLimiter`: limite simples de taxa
- `util/ItemIdParser`: extração tier/enchant do `item_id`

## Rate limit e caching
- O cliente aplica:
  - limite por minuto (`requests_per_minute`, padrão 180)
  - limite por 5 minutos (`requests_per_5_minutes`, padrão 300)
  - retry com backoff exponencial para `429` e `5xx`
- Para cargas muito grandes, ajuste `chunk_size`, `concurrency`, `requests_per_minute` e `requests_per_5_minutes`.
- Caching não foi habilitado por padrão; recomendável incluir cache local para catálogo e respostas em execuções frequentes.

## Banco SQLite
- Script SQL: `scripts/create_prices_table.sql`
- Tabela: `prices`
- Chave composta para upsert: `(item_id, city, quality, retrieved_at)`

## Testes
Inclui JUnit 5 para:
- parser de `item_id` (tier/enchant)
- chunking de requests
- mapeamento JSON da API
