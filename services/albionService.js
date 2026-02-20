const axios = require('axios');
const MemoryCache = require('../cache/memoryCache');
const { calculateEstimatedFee } = require('../utils/feeCalculator');

const BASE_URL = process.env.ALBION_API_BASE_URL || 'https://www.albion-online-data.com';
const QUALITY = process.env.ALBION_ITEM_QUALITY || 2;
const CACHE_TTL_MS = Number(process.env.CACHE_TTL_MS || 30000);
const FEE_RATE = Number(process.env.FEE_RATE || 0.065);
const FEE_FLAT = Number(process.env.FEE_FLAT || 0);
const BLACK_MARKET_LOCATION = process.env.BLACK_MARKET_LOCATION || 'Black Market';

const cache = new MemoryCache(CACHE_TTL_MS);

// Cliente HTTP para centralizar timeout e baseURL.
const client = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
});

function buildCacheKey(prefix, payload) {
  return `${prefix}:${JSON.stringify(payload)}`;
}

async function fetchPrices(items, locations) {
  const sortedItems = [...items].sort();
  const sortedLocations = [...locations].sort();

  const cacheKey = buildCacheKey('prices', {
    items: sortedItems,
    locations: sortedLocations,
    quality: QUALITY,
  });

  const cachedValue = cache.get(cacheKey);
  if (cachedValue) {
    return { data: cachedValue, cached: true };
  }

  const itemPath = sortedItems.join(',');
  const locationQuery = sortedLocations.join(',');

  const { data } = await client.get(`/api/v2/stats/prices/${itemPath}.json`, {
    params: {
      locations: locationQuery,
      qualities: QUALITY,
    },
  });

  cache.set(cacheKey, data);
  return { data, cached: false };
}

function groupByItem(records) {
  return records.reduce((acc, row) => {
    if (!acc[row.item_id]) {
      acc[row.item_id] = [];
    }

    acc[row.item_id].push(row);
    return acc;
  }, {});
}

function extractBestPrices(itemRows, cityLocations) {
  let minSellCity = null;
  let bmMaxBuy = null;

  for (const row of itemRows) {
    const isCityLocation = cityLocations.includes(row.city);
    const isBlackMarket = row.city === BLACK_MARKET_LOCATION;

    // Menor preço de sell order em cidade (apenas valores > 0).
    if (isCityLocation && row.sell_price_min > 0) {
      if (!minSellCity || row.sell_price_min < minSellCity.price) {
        minSellCity = {
          city: row.city,
          price: row.sell_price_min,
          updatedAt: row.sell_price_min_date,
        };
      }
    }

    // Maior preço de buy order no Black Market (apenas valores > 0).
    if (isBlackMarket && row.buy_price_max > 0) {
      if (!bmMaxBuy || row.buy_price_max > bmMaxBuy.price) {
        bmMaxBuy = {
          city: row.city,
          price: row.buy_price_max,
          updatedAt: row.buy_price_max_date,
        };
      }
    }
  }

  return { minSellCity, bmMaxBuy };
}

async function findFlips(items, cityLocations) {
  // Para cálculo de flips, sempre adicionamos Black Market às locations buscadas.
  const queryLocations = cityLocations.includes(BLACK_MARKET_LOCATION)
    ? cityLocations
    : [...cityLocations, BLACK_MARKET_LOCATION];

  const { data, cached } = await fetchPrices(items, queryLocations);
  const byItem = groupByItem(data);

  const flips = [];

  for (const itemId of Object.keys(byItem)) {
    const rows = byItem[itemId];
    const { minSellCity, bmMaxBuy } = extractBestPrices(rows, cityLocations);

    if (!minSellCity || !bmMaxBuy) {
      continue;
    }

    const estimatedFee = calculateEstimatedFee(minSellCity.price, FEE_RATE, FEE_FLAT);
    const profit = bmMaxBuy.price - minSellCity.price - estimatedFee;

    // Retorna apenas flips com lucro positivo.
    if (profit > 0) {
      flips.push({
        itemId,
        buyFromCity: minSellCity.city,
        sellTo: bmMaxBuy.city,
        sellPriceCity: minSellCity.price,
        buyPriceBlackMarket: bmMaxBuy.price,
        estimatedFee,
        estimatedProfit: profit,
        sellPriceUpdatedAt: minSellCity.updatedAt,
        buyPriceUpdatedAt: bmMaxBuy.updatedAt,
      });
    }
  }

  // Ordena do maior para o menor lucro estimado.
  flips.sort((a, b) => b.estimatedProfit - a.estimatedProfit);

  return {
    itemsRequested: items,
    cityLocations,
    fee: {
      feeRate: FEE_RATE,
      feeFlat: FEE_FLAT,
    },
    flips,
    cached,
  };
}

module.exports = {
  fetchPrices,
  findFlips,
  BLACK_MARKET_LOCATION,
};
