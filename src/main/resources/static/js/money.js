// Цены демо-склада и магазина хранятся в тенге; курс внешнего сервиса не нужен для локального показа.
window.ariaMoney = {
    currency: 'KZT',
    format(value) {
        const amount = Number(value || 0);
        return new Intl.NumberFormat('ru-KZ', {
            style: 'currency', currency: this.currency, maximumFractionDigits: 0
        }).format(amount);
    }
};
