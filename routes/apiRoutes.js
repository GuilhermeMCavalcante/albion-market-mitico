const express = require('express');
const { parseCsvParam } = require('../utils/parseQuery');
const { fetchPrices, findFlips, BLACK_MARKET_LOCATION } = require('../services/albionService');

const router = express.Router();

router.get('/prices', async (req, res, next) => {
  try {
    const items = parseCsvParam(req.query.items);
    const locations = parseCsvParam(req.query.locations);

    if (!items.length || !locations.length) {
      return res.status(400).json({
        error: 'Parâmetros obrigatórios: items e locations (CSV).',
      });
    }

    const result = await fetchPrices(items, locations);

    return res.json({
      ...result,
      count: result.data.length,
    });
  } catch (error) {
    return next(error);
  }
});

router.get('/flips', async (req, res, next) => {
  try {
    const items = parseCsvParam(req.query.items);
    const locations = parseCsvParam(req.query.locations);

    if (!items.length || !locations.length) {
      return res.status(400).json({
        error: 'Parâmetros obrigatórios: items e locations (CSV).',
      });
    }

    // Evita usar Black Market como cidade de compra no lado de sell order.
    const cityLocations = locations.filter((loc) => loc !== BLACK_MARKET_LOCATION);

    if (!cityLocations.length) {
      return res.status(400).json({
        error: 'Informe ao menos uma cidade válida diferente de Black Market.',
      });
    }

    const result = await findFlips(items, cityLocations);
    return res.json(result);
  } catch (error) {
    return next(error);
  }
});

module.exports = router;
