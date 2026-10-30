package com.aria.integration;
import org.springframework.stereotype.Component;
import java.util.*;
import java.math.BigDecimal;
@Component
public class WooCatalogImporter {
 private final WooCommerceReader reader;
 public WooCatalogImporter(WooCommerceReader reader){this.reader=reader;}
 public record Item(String externalId,String parentId,String name,String sku,BigDecimal price,Integer quantity){}
 public record Catalog(List<Item> items,int skipped,String currency){}
 public Catalog read(String address,String key,String secret){
  var items=new ArrayList<Item>();var seen=new HashSet<String>();int skipped=0;
  long deadline=System.nanoTime()+java.time.Duration.ofSeconds(60).toNanos();int[] requests={0};
  String currency=currency(address,key,secret,requests,deadline);
  for(var p:all(address,"products",key,secret,requests,deadline)){
   String type=Objects.toString(p.get("type"),"");
   if(type.equals("simple"))add(items,seen,item(p,null,null));
   else if(type.equals("variable"))for(var v:all(address,"products/"+id(p)+"/variations",key,secret,requests,deadline))add(items,seen,item(v,id(p),text(p,"name",500)));
   else skipped++;
  }
  if(!currency.equals(currency(address,key,secret,requests,deadline)))throw new IllegalStateException("Валюта магазина изменилась во время чтения. Предыдущий каталог сохранён");
  return new Catalog(List.copyOf(items),skipped,currency);
 }
 private String currency(String address,String key,String secret,int[] requests,long deadline){if(++requests[0]>30||System.nanoTime()>deadline)throw new IllegalStateException("Время проверки валюты превышено. Предыдущий каталог сохранён");String code=reader.currency(address,key,secret);if(code==null||!code.matches("[A-Z]{3}"))throw new IllegalStateException("Валюта каталога не подтверждена");return code;}
 private List<Map<String,Object>> all(String address,String resource,String key,String secret,int[] requests,long deadline){
  var rows=new ArrayList<Map<String,Object>>();
  for(int page=1;page<=11;page++){
   if(++requests[0]>30||System.nanoTime()>deadline)throw new IllegalStateException("Превышен лимит чтения пилота. Предыдущий каталог сохранён");
   var part=reader.page(address,resource+"?per_page=100&page="+page,key,secret);
   rows.addAll(part.items());if(rows.size()>1000)throw new IllegalStateException("Лимит пилота — 1000 карточек. Предыдущий каталог сохранён");
   if(!part.more())return rows;
  }
  throw new IllegalStateException("Слишком много страниц. Предыдущий каталог сохранён");
 }
 private void add(List<Item> items,Set<String> seen,Item item){if(!seen.add(item.externalId()))throw new IllegalStateException("Магазин вернул повторяющиеся карточки. Предыдущий каталог сохранён");if(items.size()>=1000)throw new IllegalStateException("Лимит пилота — 1000 карточек");items.add(item);}
 private Item item(Map<String,Object> p,String parentId,String parentName){
  String name=parentName==null?text(p,"name",500):parentName;
  if(parentId!=null){var labels=new ArrayList<String>();if(p.get("attributes") instanceof List<?> attributes)for(var a:attributes)if(a instanceof Map<?,?> m&&m.get("option") instanceof String value)labels.add(value);name+=" · "+(labels.isEmpty()?"Вариант "+id(p):String.join(" / ",labels));}
  if(name.isBlank()||name.length()>500)throw new IllegalStateException("Название товара не поддерживается. Предыдущий каталог сохранён");
  String sku=Objects.toString(p.get("sku"),"");if(sku.length()>100)throw new IllegalStateException("Артикул длиннее 100 символов");
  BigDecimal price=null;try{String value=Objects.toString(p.get("price"),"");if(!value.isBlank()){price=new BigDecimal(value);if(price.signum()<0||price.scale()>2||price.precision()>30)throw new NumberFormatException();}}catch(NumberFormatException e){throw new IllegalStateException("Магазин вернул неподдерживаемую цену");}
  Integer quantity=null;if(Boolean.TRUE.equals(p.get("manage_stock"))){Object raw=p.get("stock_quantity");if(raw!=null){try{quantity=new BigDecimal(raw.toString()).intValueExact();}catch(Exception e){throw new IllegalStateException("Магазин вернул неподдерживаемое количество");}}}
  return new Item(id(p),parentId,name,sku,price,quantity);
 }
 private String id(Map<String,Object> p){String id=Objects.toString(p.get("id"),"");if(!id.matches("[1-9][0-9]{0,18}"))throw new IllegalStateException("Магазин вернул неверный идентификатор");return id;}
 private String text(Map<String,Object> p,String key,int limit){String value=Objects.toString(p.get(key),"");if(value.isBlank()||value.length()>limit)throw new IllegalStateException("Магазин вернул неподдерживаемое название");return value;}
}
