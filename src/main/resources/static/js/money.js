window.formatKZT = function(value) {
  if (value == null || value === '' || !Number.isFinite(Number(value))) return '—';
  const amount = Number(value);
  const grouped = new Intl.NumberFormat(ariaCopy.locale(), { maximumFractionDigits: 2 }).format(amount);
  return grouped.replace(/[\s\u00a0\u202f]/g, "\u00a0") + "\u00a0₸";
};
window.ariaMoney = {
  currency: "KZT",
  format(value) { return window.formatKZT(value); }
};
