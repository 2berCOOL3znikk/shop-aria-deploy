<?php
// Router for the isolated PHP built-in server. All WordPress/REST work is real.
$root = realpath(__DIR__ . '/../../data/woocommerce/site/wordpress');
$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$_SERVER['HTTPS'] = 'on';
$_SERVER['SERVER_PORT'] = '8449';
if (str_starts_with($uri, '/wp-json/')) {
    $_GET['rest_route'] = substr($uri, 8);
    require $root . '/index.php';
    return true;
}
$file = realpath($root . $uri);
if ($file && str_starts_with($file, $root . DIRECTORY_SEPARATOR) && is_file($file)) return false;
require $root . '/index.php';
