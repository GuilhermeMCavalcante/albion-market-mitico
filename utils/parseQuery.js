// Converte string "a,b,c" em array limpo sem itens vazios.
function parseCsvParam(value) {
  if (!value || typeof value !== 'string') return [];

  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

module.exports = {
  parseCsvParam,
};
