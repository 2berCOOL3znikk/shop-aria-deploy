package com.aria.service;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;

/** Synthetic products and project-owned illustrations, never manufacturer evidence. */
@Component
public class SupermarketDemoCatalogue {
 public record Item(String code,String name,String category,BigDecimal price,BigDecimal cost,BigDecimal minimumPrice,String unit,String size,String image,String profile,int quantity,Integer expiryDays,String storage,int weeklySales){}
 private record Source(int schemaVersion,boolean demonstration,String notice,List<Item> items){}
 private final List<Item> items;private final Map<String,Item> references;
 public SupermarketDemoCatalogue(){try(var stream=getClass().getResourceAsStream("/catalogue/supermarket-demo.json")){
  var data=tools.jackson.databind.json.JsonMapper.builder().build().readValue(stream,Source.class);if(data.schemaVersion()!=1||!data.demonstration()||data.items().size()!=64)throw new IllegalStateException("Invalid supermarket teaching set");
  var byCode=new LinkedHashMap<String,Item>();for(var item:data.items()){if(!item.code().matches("ARIA-LAB-\\d{3}")||byCode.put(item.code(),item)!=null||!item.image().equals("/images/demo/supermarket/"+item.code().toLowerCase(Locale.ROOT)+".svg")||item.cost().signum()<0||item.minimumPrice().compareTo(item.cost())<0||item.price().compareTo(item.minimumPrice())<0||item.quantity()<1||item.weeklySales()<0)throw new IllegalStateException("Invalid teaching product");}
  items=List.copyOf(data.items());references=Map.copyOf(byCode);
 }catch(java.io.IOException e){throw new IllegalStateException("Cannot read supermarket teaching set",e);}}
 public List<Item> items(){return items;}
 public Item reference(String value){return value!=null&&value.startsWith("supermarket-v1:")?references.get(value.substring("supermarket-v1:".length())):null;}
 public Map<String,Object> illustration(Item item){return Map.of("path",item.image(),"author","ARIA project","sourceUrl","https://github.com/2berCOOL3znikk/shop-aria-deploy/blob/codex/production-foundation/src/main/resources/static"+item.image(),"note","Оригинальная учебная иллюстрация проекта. Не фотография коммерческого товара.");}
}
