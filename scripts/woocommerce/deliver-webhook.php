<?php
// Calls the installed WooCommerce delivery implementation, not an ARIA HMAC stub.
if(PHP_SAPI!=='cli'||$argc!==3||!ctype_digit($argv[1])||!ctype_digit($argv[2]))exit(1);
$site=realpath(__DIR__.'/../../data/woocommerce/site/wordpress');
$_SERVER['HTTP_HOST']='127.0.0.1:8449';$_SERVER['HTTPS']='on';$_SERVER['SERVER_PORT']='8449';
require $site.'/wp-load.php';
$hook=wc_get_webhook((int)$argv[1]);
if(!$hook||!str_starts_with($hook->get_delivery_url(),'http://127.0.0.1:8085/api/webhooks/woocommerce/'))exit(2);
$hook->deliver((int)$argv[2]);
echo "Actual WooCommerce delivery attempted.\n";
