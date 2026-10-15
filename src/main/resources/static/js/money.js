window.formatKZT = function(value) {
  const amount = Number(value || 0);
  const grouped = new Intl.NumberFormat("ru-KZ", { maximumFractionDigits: 0 }).format(amount);
  return grouped.replace(/[\s\u00a0\u202f]/g, "\u00a0") + "\u00a0₸";
};
window.ariaMoney = {
  currency: "KZT",
  format(value) { return window.formatKZT(value); }
};