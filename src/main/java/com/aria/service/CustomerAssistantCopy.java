package com.aria.service;
import java.util.*;
/** System response copy only. Product names, attributes, prices and IDs pass unchanged. */
final class CustomerAssistantCopy {
 private CustomerAssistantCopy(){}
 private static final Map<String,String> EN=Map.ofEntries(
  Map.entry("Я помогаю с покупками в ARIA: найти товар, сравнить варианты и проверить доступные скидки. Что подберём в каталоге?","I help with shopping in ARIA: finding products, comparing options and checking available discounts. What would you like to find?"),
  Map.entry("Ваши заказы и их статусы доступны в профиле после входа. Я не передаю личные данные заказа внешнему AI.","Sign in to view your orders and their status in your profile. I do not share personal order details with external AI."),
  Map.entry("Подтверждённые условия доставки и возврата уточняйте в поддержке магазина. Я не буду придумывать сроки или обещать возврат.","Contact store support for confirmed delivery and return conditions. I cannot invent delivery dates or promise a refund."),
  Map.entry("Открыть профиль","Open profile"),Map.entry("Поддержка магазина","Store support"),Map.entry("Открыть каталог","Open catalogue"),
  Map.entry("Ответ по разделам магазина. Внешний AI не вызывался.","Store information only. External AI was not called."),
  Map.entry("Подбор по текущему каталогу и заданным ограничениям.","Selection uses the current catalogue and your stated limits."),
  Map.entry("Модель выбрала товары; цены и доступность проверены сервером по каталогу.","The model selected products; their prices and availability were checked against the catalogue."),
  Map.entry("Внешний AI недоступен. Показан подбор по текущему каталогу.","External AI is unavailable. Results use the current catalogue."),
  Map.entry("Внешний AI выключен или достигнут его лимит. Показан подбор по текущему каталогу.","External AI is off or its request limit was reached. Results use the current catalogue."),
  Map.entry("Подходящих доступных товаров не найдено. Попробуйте изменить бюджет или запрос.","No matching available products were found. Try adjusting your budget or search."),
  Map.entry("Сравните актуальные цены, категории, единицы и характеристики выбранных товаров ниже. Упаковки могут отличаться.","Compare the selected products' current prices, categories, units and verified details below. Pack sizes may differ."),
  Map.entry("Подходящие доступные товары показаны ниже. Цена и наличие повторно проверяются при оформлении.","Matching available products are shown below. Prices and availability are checked again at checkout."),
  Map.entry("Внешний AI получает только ваше сообщение и публичные карточки после вашего согласия. Поиск по каталогу работает и без внешнего AI.","With your consent, external AI receives only your message and public product cards. Catalogue search also works without external AI.")
 );
 static Map<String,Object> localize(Map<String,Object> response,String language){if(language==null||!language.toLowerCase(Locale.ROOT).startsWith("en"))return response;var out=new LinkedHashMap<>(response);for(String field:List.of("text","note"))if(out.get(field) instanceof String value)out.put(field,EN.getOrDefault(value,value));if(out.get("links") instanceof List<?> links)out.put("links",links.stream().map(raw->{var link=new LinkedHashMap<>((Map<String,Object>)raw);if(link.get("label") instanceof String label)link.put("label",EN.getOrDefault(label,label));return link;}).toList());return out;}
}
