package com.albion.market.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public record AppConfig(
        String itemCatalogUrl,
        String pricesBaseUrl,
        List<String> cities,
        List<Integer> qualities,
        Set<Integer> enchantments,
        int concurrency,
        int chunkSize,
        int maxRetries,
        Duration requestTimeout,
        int requestsPerMinute,
        int requestsPerFiveMinutes,
        Path outputCsvPath,
        Path sqliteDbPath,
        Integer sinceMinutes
) {
    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
        }

        return new AppConfig(
                read(props, "item_catalog_url", "https://raw.githubusercontent.com/ao-data/ao-bin-dumps/master/formatted/items.json"),
                read(props, "prices_base_url", "https://www.albion-online-data.com/api/v2/stats/prices"),
                csvStrings(read(props, "cities", "Bridgewatch,Martlock,Thetford,Lymhurst,FortSterling,Caerleon")),
                csvInts(read(props, "qualities", "1,2,3,4,5")),
                csvInts(read(props, "include_enchantments", "0,1,2,3")).stream().collect(Collectors.toSet()),
                Integer.parseInt(read(props, "concurrency", "6")),
                Integer.parseInt(read(props, "chunk_size", "100")),
                Integer.parseInt(read(props, "max_retries", "5")),
                Duration.ofSeconds(Long.parseLong(read(props, "request_timeout_seconds", "30"))),
                Integer.parseInt(read(props, "requests_per_minute", "180")),
                Integer.parseInt(read(props, "requests_per_5_minutes", "300")),
                Path.of(read(props, "output_csv_path", "./output/prices.csv")),
                Path.of(read(props, "sqlite_db_path", "./output/prices.db")),
                readOptionalInt(props, "since_minutes")
        );
    }

    private static String read(Properties props, String key, String def) {
        return System.getenv().getOrDefault(key.toUpperCase(), props.getProperty(key, def));
    }

    private static Integer readOptionalInt(Properties props, String key) {
        String value = System.getenv().getOrDefault(key.toUpperCase(), props.getProperty(key));
        return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
    }

    private static List<String> csvStrings(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private static List<Integer> csvInts(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(Integer::parseInt).toList();
    }
}
