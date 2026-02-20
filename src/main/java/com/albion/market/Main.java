package com.albion.market;

import com.albion.market.cli.CliOverrides;
import com.albion.market.config.AppConfig;
import com.albion.market.http.HttpJsonClient;
import com.albion.market.model.ItemDefinition;
import com.albion.market.model.PriceRecord;
import com.albion.market.persistence.CsvPriceWriter;
import com.albion.market.persistence.SqlitePriceRepository;
import com.albion.market.service.ItemCatalogLoader;
import com.albion.market.service.MarketCollectorService;
import com.albion.market.service.PriceApiService;
import com.albion.market.util.SimpleRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Instant start = Instant.now();

        AppConfig config = CliOverrides.apply(AppConfig.load(), args);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SimpleRateLimiter limiter = new SimpleRateLimiter(config.requestsPerSecond());
        HttpJsonClient http = new HttpJsonClient(objectMapper, config.maxRetries(), config.requestTimeout(), limiter);

        ItemCatalogLoader catalogLoader = new ItemCatalogLoader(http);
        PriceApiService priceApiService = new PriceApiService(http, config.pricesBaseUrl());
        MarketCollectorService marketCollector = new MarketCollectorService(priceApiService);

        log.info("Starting with concurrency={}, cities={}, qualities={}, enchantments={}",
                config.concurrency(), config.cities(), config.qualities(), config.enchantments());

        List<ItemDefinition> items = catalogLoader.load(config.itemCatalogUrl());
        List<PriceRecord> rows = marketCollector.collect(config, items);

        new CsvPriceWriter().write(config.outputCsvPath(), rows);
        new SqlitePriceRepository().save(config.sqliteDbPath(), rows);

        log.info("Done. Rows={}, csv={}, db={}, elapsed={}s",
                rows.size(), config.outputCsvPath(), config.sqliteDbPath(), Duration.between(start, Instant.now()).toSeconds());
    }
}
