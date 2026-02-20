package com.albion.market.http;

import com.albion.market.util.SimpleRateLimiter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpJsonClient {
    private static final Logger log = LoggerFactory.getLogger(HttpJsonClient.class);

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final Duration timeout;
    private final SimpleRateLimiter rateLimiter;

    public HttpJsonClient(ObjectMapper objectMapper, int maxRetries, Duration timeout, SimpleRateLimiter rateLimiter) {
        this.client = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
        this.rateLimiter = rateLimiter;
    }

    public <T> T getJson(String url, TypeReference<T> typeReference) {
        int attempt = 0;
        long backoffMillis = 500;
        while (true) {
            attempt++;
            rateLimiter.acquire();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return objectMapper.readValue(response.body(), typeReference);
                }
                if ((status == 429 || status >= 500) && attempt <= maxRetries) {
                    log.warn("HTTP {} for {} (attempt {}/{}). Retrying in {} ms", status, url, attempt, maxRetries, backoffMillis);
                    if (status == 429) {
                        rateLimiter.penalize(Duration.ofSeconds(2));
                    }
                    Thread.sleep(backoffMillis);
                    backoffMillis *= 2;
                    continue;
                }
                throw new IllegalStateException("Request failed with status " + status + " for URL " + url);
            } catch (IOException e) {
                if (attempt > maxRetries) {
                    throw new RuntimeException("Failed request for URL " + url, e);
                }
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying", ex);
                }
                backoffMillis *= 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }
    }
}
