<?php
/** Only the existing isolated installation and teaching SKU. No production connection or old-order edits. */
if (PHP_SAPI !== 'cli') { exit(1); }
$root = realpath(__DIR__ . '/../..');
$site = realpath($root . '/data/woocommerce/site/wordpress');
if (!$site || str_replace('\\', '/', $site) !== str_replace('\\', '/', $root . '/data/woocommerce/site/wordpress')) { throw new RuntimeException('Isolated installation required'); }
require $site . '/wp-load.php';
if (rtrim(get_option('home'), '/') !== 'https://127.0.0.1:8449') { throw new RuntimeException('Unexpected WordPress address'); }
$product = wc_get_product((int)($argv[1] ?? 0));
if (!$product || $product->get_sku() !== 'ARIA-QA-AUTONOMY-001' || $product->get_meta('aria_educational_fixture') !== 'autonomy-v1') { throw new RuntimeException('Existing labelled teaching SKU required'); }
$matches = wc_get_orders(array('limit' => 100, 'status' => 'completed', 'orderby' => 'date', 'order' => 'DESC'));
foreach ($matches as $existing) {
    if ($existing->get_meta('aria_educational_fixture') === 'autonomy-history-v1') {
        echo json_encode(array('orderId' => $existing->get_id(), 'created' => false)); exit(0);
    }
}
// REST v3 creation dates are read-only in the installed WooCommerce. Use its supported PHP object API
// for a NEW, explicitly synthetic historical fixture, without changing existing orders or the clock.
$now = new DateTimeImmutable('now', new DateTimeZone('UTC'));
$order = wc_create_order();
$order->add_product($product, 15, array('subtotal' => '22500.00', 'total' => '22500.00'));
$order->set_currency('KZT');
$order->set_customer_note('ARIA: искусственная учебная история. Не реальная покупка, оплата или коммерческий спрос.');
$order->update_meta_data('aria_educational_fixture', 'autonomy-history-v1');
$order->set_date_created($now->modify('-16 days')->getTimestamp());
$order->set_status('completed');
$order->set_date_completed($now->modify('-15 days')->getTimestamp());
$order->calculate_totals(false);
$order->save();
wc_reduce_stock_levels($order->get_id()); // WooCommerce guards against applying this stock reduction twice.
echo json_encode(array('orderId' => $order->get_id(), 'created' => true));
