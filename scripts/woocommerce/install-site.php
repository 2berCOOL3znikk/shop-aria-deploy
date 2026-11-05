<?php
// CLI bootstrap: real WordPress tables, actual WooCommerce plugin and API keys.
// The fixture catalogue and all prices/orders are explicitly educational.
if (PHP_SAPI !== 'cli') exit(1);
$base = realpath(__DIR__ . '/../../data/woocommerce');
if (!is_file($base.'/wordpress-installed')) define('WP_INSTALLING', true);
$config = json_decode(preg_replace('/^\xEF\xBB\xBF/', '', file_get_contents($base . '/config.json')), true, 512, JSON_THROW_ON_ERROR);
$_SERVER['HTTP_HOST'] = '127.0.0.1:8449';
$_SERVER['HTTPS'] = 'on';
$_SERVER['SERVER_PORT'] = '8449';
$site = $base . '/site/wordpress';
if (!is_file($site . '/wp-config.php')) {
    $settings = "<?php\ndefine('DB_NAME','aria_woo_e2e');\ndefine('DB_USER','root');\n";
    $settings .= "define('DB_PASSWORD'," . var_export($config['databasePassword'], true) . ");\n";
    $settings .= "define('DB_HOST','127.0.0.1:53389');\ndefine('DB_CHARSET','utf8mb4');\ndefine('DB_COLLATE','');\n";
    $settings .= "define('WP_HOME','https://127.0.0.1:8449');\ndefine('WP_SITEURL',WP_HOME);\n";
    $settings .= "define('WP_ENVIRONMENT_TYPE','local');\ndefine('DISABLE_WP_CRON',true);\ndefine('DISALLOW_FILE_EDIT',true);\ndefine('AUTOMATIC_UPDATER_DISABLED',true);\ndefine('WP_AUTO_UPDATE_CORE',false);\n";
    $settings .= "define('WP_DEBUG',false);\ndefine('AUTH_KEY'," . var_export(bin2hex(random_bytes(32)), true) . ");\n";
    $settings .= "\$table_prefix='aria_';\nif(!defined('ABSPATH')) define('ABSPATH',__DIR__.'/');\nrequire_once ABSPATH.'wp-settings.php';\n";
    file_put_contents($site . '/wp-config.php', $settings);
}
// Preserve credentials if a prior bootstrap stopped; repair only our own guard.
$existingConfig=file_get_contents($site.'/wp-config.php');
$existingConfig=str_replace("\ndefine('ABSPATH',__DIR__.'/');", "\nif(!defined('ABSPATH')) define('ABSPATH',__DIR__.'/');", $existingConfig);
file_put_contents($site.'/wp-config.php',$existingConfig);
require $site . '/wp-load.php';
require_once ABSPATH . 'wp-admin/includes/upgrade.php';
require_once ABSPATH . 'wp-admin/includes/plugin.php';
if (!is_blog_installed()) wp_install('ARIA — isolated educational WooCommerce', 'aria_owner', 'e2e@aria.example', false, '', $config['ownerPassword']);
if (!is_file($base.'/wordpress-installed')) {file_put_contents($base.'/wordpress-installed','Installed isolated WordPress');echo "WordPress installed; loading WooCommerce on next bootstrap.\n";exit(0);}
wp_set_current_user(1);
if (!is_plugin_active('woocommerce/woocommerce.php')) {
    $result = activate_plugin('woocommerce/woocommerce.php');
    if (is_wp_error($result)) throw new Exception($result->get_error_message());
}
if (!class_exists('WooCommerce')) require_once ABSPATH.'wp-content/plugins/woocommerce/woocommerce.php';
if (!did_action('woocommerce_init')) WC()->init();
if (!WC()->countries) WC()->countries=new WC_Countries();
WC_Install::install();
update_option('woocommerce_currency','KZT');
update_option('woocommerce_default_country','KZ');
update_option('woocommerce_store_address','Isolated educational fixtures, no real shop');
update_option('woocommerce_calc_taxes','no');
update_option('woocommerce_task_list_complete',true);
update_option('woocommerce_allow_tracking','no');
update_option('permalink_structure','/%postname%/');
// Test-only hooks permit exactly the isolated ARIA receiver. No general SSRF bypass.
$mu = $site . '/wp-content/mu-plugins';
if (!is_dir($mu)) mkdir($mu,0700,true);
file_put_contents($mu.'/aria-local-e2e.php', <<<'PHP'
<?php
// THIS FILE IS GENERATED ONLY INSIDE data/woocommerce, NEVER A PRODUCTION PLUGIN.
add_filter('http_request_host_is_external', function($allow,$host,$url){return str_starts_with($url,'http://127.0.0.1:8085/api/webhooks/woocommerce/') && $host==='127.0.0.1' ? true : $allow;},10,3);
add_filter('http_allowed_safe_ports', function($ports){$ports[]=8085;return $ports;});
// Execute the actual WooCommerce delivery synchronously; no WP-cron in this fixture.
add_filter('woocommerce_webhook_deliver_async','__return_false');
add_filter('pre_wp_mail','__return_true');
PHP
);
if (!is_file($base.'/fixtures.json')) {
    $simpleId=wc_get_product_id_by_sku('ARIA-WOO-TOWEL');$simple=$simpleId?wc_get_product($simpleId):new WC_Product_Simple();$simple->set_name('Полотенце хлопковое · учебный товар');$simple->set_sku('ARIA-WOO-TOWEL');$simple->set_regular_price('1500');$simple->set_manage_stock(true);$simple->set_stock_quantity(20);$simple->set_status('publish');$simple->save();
    $variableId=wc_get_product_id_by_sku('ARIA-WOO-TSHIRT');$variable=$variableId?wc_get_product($variableId):new WC_Product_Variable();$variable->set_name('Футболка · учебный товар');$variable->set_sku('ARIA-WOO-TSHIRT');$variable->set_status('publish');
    $attribute=new WC_Product_Attribute();$attribute->set_name('Размер');$attribute->set_options(['M','L']);$attribute->set_variation(true);$variable->set_attributes([$attribute]);$variable->save();
    $variations=[];
    foreach(['M','L'] as $size) {$variationId=wc_get_product_id_by_sku('ARIA-WOO-TSHIRT-'.$size);$v=$variationId?wc_get_product($variationId):new WC_Product_Variation();$v->set_parent_id($variable->get_id());$v->set_attributes(['размер'=>$size]);$v->set_sku('ARIA-WOO-TSHIRT-'.$size);$v->set_regular_price($size==='M'?'3200':'3500');$v->set_manage_stock(true);$v->set_stock_quantity($size==='M'?8:4);$v->set_status('publish');$v->save();$variations[]=$v->get_id();}
    WC_Product_Variable::sync($variable->get_id());
    $order=wc_create_order();$order->add_product($simple,2);$order->calculate_totals(false);$order->set_status('completed');$order->set_date_completed(time());$order->save();wc_reduce_stock_levels($order->get_id());
    $key='ck_'.bin2hex(random_bytes(20));$secret='cs_'.bin2hex(random_bytes(20));
    global $wpdb;
    $wpdb->insert($wpdb->prefix.'woocommerce_api_keys',['user_id'=>1,'description'=>'ARIA isolated E2E read/write','permissions'=>'read_write','consumer_key'=>wc_api_hash($key),'consumer_secret'=>$secret,'truncated_key'=>substr($key,-7)]);
    $readKey='ck_'.bin2hex(random_bytes(20));$readSecret='cs_'.bin2hex(random_bytes(20));
    $wpdb->insert($wpdb->prefix.'woocommerce_api_keys',['user_id'=>1,'description'=>'ARIA isolated E2E read only','permissions'=>'read','consumer_key'=>wc_api_hash($readKey),'consumer_secret'=>$readSecret,'truncated_key'=>substr($readKey,-7)]);
    file_put_contents($base.'/fixtures.json',json_encode(['simpleId'=>$simple->get_id(),'parentId'=>$variable->get_id(),'variationIds'=>$variations,'orderId'=>$order->get_id(),'key'=>$key,'secret'=>$secret,'readKey'=>$readKey,'readSecret'=>$readSecret],JSON_PRETTY_PRINT|JSON_UNESCAPED_UNICODE));
}
echo 'Real WordPress '.get_bloginfo('version').' / WooCommerce '.WC_VERSION." ready. Secrets are only in data/woocommerce.\n";
