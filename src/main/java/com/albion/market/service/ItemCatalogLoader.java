package com.albion.market.service;

import com.albion.market.http.HttpJsonClient;
import com.albion.market.model.ItemDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ItemCatalogLoader {
    private static final Logger log = LoggerFactory.getLogger(ItemCatalogLoader.class);

    private final HttpJsonClient httpJsonClient;

    public ItemCatalogLoader(HttpJsonClient httpJsonClient) {
        this.httpJsonClient = httpJsonClient;
    }

    public List<ItemDefinition> load(String catalogUrl) {
        List<JsonNode> nodes = httpJsonClient.getJson(catalogUrl, new TypeReference<>() {});
        List<ItemDefinition> items = new ArrayList<>();
        int ignored = 0;

        for (JsonNode node : nodes) {
            String uniqueName = text(node, "UniqueName");
            if (uniqueName == null || uniqueName.isBlank()) {
                ignored++;
                continue;
            }
            String localizedName = null;
            JsonNode localizedNode = node.get("LocalizedNames");
            if (localizedNode != null) {
                localizedName = text(localizedNode, "EN-US");
            }
            items.add(new ItemDefinition(uniqueName, localizedName));
        }

        log.info("Loaded {} valid items and ignored {} invalid rows from catalog", items.size(), ignored);
        return items;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
