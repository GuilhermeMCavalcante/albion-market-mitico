const express = require('express');
const dotenv = require('dotenv');
const apiRoutes = require('./routes/apiRoutes');

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Habilita JSON em requests futuras (mesmo com endpoints GET por enquanto).
app.use(express.json());

// Prefixo principal da API.
app.use('/api', apiRoutes);

// Endpoint simples de health check.
app.get('/health', (_, res) => {
  res.json({ status: 'ok', service: 'albion-flip-api' });
});

// Middleware de erro centralizado.
app.use((err, req, res, next) => {
  console.error('Erro não tratado:', err);
  res.status(500).json({
    error: 'Erro interno no servidor',
    details: err.message,
  });
});

app.listen(PORT, () => {
  console.log(`albion-flip-api rodando na porta ${PORT}`);
});
