package com.aria.integration;
import org.springframework.stereotype.Component;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Component
public class WooOrderImporter {
 private final WooCommerceReader reader;
 public WooOrderImporter(WooCommerceReader reader){this.reader=reader;}
 public record Line(String productId,int quantity,BigDecimal total){}
 public record Item(String externalId,String status,String currency,BigDecimal total,BigDecimal refundedTotal,LocalDateTime createdAtUtc,LocalDateTime modifiedAtUtc,LocalDateTime completedAtUtc,List<Line> lines){}
 public List<Item> read(String address,String key,String secret){
  var result=new ArrayList<Item>();var seen=new HashSet<String>();int lineCount=0;long deadline=System.nanoTime()+java.time.Duration.ofSeconds(60).toNanos();
  for(int page=1;page<=11;page++){
   if(System.nanoTime()>deadline)throw new IllegalStateException("Время чтения заказов превышено. Предыдущие данные сохранены");
   var response=reader.page(address,"orders?per_page=100&page="+page,key,secret);
   for(var row:response.items()){if(result.size()>=1000)throw new IllegalStateException("Лимит пилота — 1000 заказов за обновление");var item=parse(row);lineCount+=item.lines().size();if(lineCount>10000)throw new IllegalStateException("Лимит пилота — 10000 позиций за обновление");if(!seen.add(item.externalId()))throw new IllegalStateException("Магазин вернул повторяющиеся заказы");result.add(item);}
   if(!response.more())return List.copyOf(result);
  }
  throw new IllegalStateException("Слишком много страниц заказов. Предыдущие данные сохранены");
 }
 Item parse(Map<String,Object> row){
  String status=Objects.toString(row.get("status"),""),currency=Objects.toString(row.get("currency"),"");
  if(!status.matches("[a-z0-9_-]{1,40}")||!currency.matches("[A-Z]{3}"))throw new IllegalStateException("Неизвестный статус или валюта заказа");
  var modified=date(row.get("date_modified_gmt"));if(modified==null)throw new IllegalStateException("Для обновления заказа нужна дата изменения в UTC");
  var lines=new ArrayList<Line>();
  if(!(row.get("line_items") instanceof List<?> source)||source.size()>1000)throw new IllegalStateException("Неподдерживаемый состав заказа");
  for(var entry:source){if(!(entry instanceof Map<?,?> line))throw new IllegalStateException("Неподдерживаемый состав заказа");
   try{int quantity=new BigDecimal(Objects.toString(line.get("quantity"),"")).intValueExact();if(quantity<=0)throw new IllegalArgumentException();Object variation=line.get("variation_id");Object productValue=variation!=null&&!variation.toString().equals("0")?variation:line.get("product_id");String product=productValue==null||productValue.toString().equals("0")?null:id(productValue);lines.add(new Line(product,quantity,money(line.get("total"),false)));}catch(IllegalArgumentException|ArithmeticException e){throw new IllegalStateException("Неподдерживаемая позиция заказа");}
  }
  BigDecimal refunds=null;if(row.get("refunds") instanceof List<?> values){refunds=BigDecimal.ZERO;for(var value:values){if(!(value instanceof Map<?,?> refund))throw new IllegalStateException("Неподдерживаемые данные возврата");refunds=refunds.add(money(refund.get("total"),true).abs());}}
  var total=money(row.get("total"),false);if(refunds!=null&&refunds.compareTo(total)>0)throw new IllegalStateException("Сумма возвратов превышает сумму заказа");
  return new Item(id(row.get("id")),status,currency,total,refunds,date(row.get("date_created_gmt")),modified,date(row.get("date_completed_gmt")),List.copyOf(lines));
 }
 private String id(Object raw){String value=Objects.toString(raw,"");if(!value.matches("[1-9][0-9]{0,18}"))throw new IllegalStateException("Некорректный идентификатор заказа или товара");return value;}
 private BigDecimal money(Object raw,boolean signed){try{var value=new BigDecimal(Objects.toString(raw,""));if(value.precision()>30||value.scale()>6||(!signed&&value.signum()<0))throw new NumberFormatException();return value;}catch(Exception e){throw new IllegalStateException("Некорректная сумма в заказе");}}
 private LocalDateTime date(Object raw){if(raw==null||raw.toString().isBlank())return null;try{return LocalDateTime.parse(raw.toString());}catch(Exception e){throw new IllegalStateException("Некорректная дата в заказе");}}
}
