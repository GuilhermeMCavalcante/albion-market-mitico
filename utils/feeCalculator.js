// Calcula fee estimada com parte percentual + parte fixa.
// Exemplo: fee = sellPrice * rate + flat
function calculateEstimatedFee(sellPrice, feeRate, feeFlat) {
  const ratePart = sellPrice * feeRate;
  return ratePart + feeFlat;
}

module.exports = {
  calculateEstimatedFee,
};
