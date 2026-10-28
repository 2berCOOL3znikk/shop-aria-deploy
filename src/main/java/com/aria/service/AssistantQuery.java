package com.aria.service;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
public record AssistantQuery(String text,BigDecimal budget,boolean discountsOnly,boolean withoutDiscounts,List<String> terms,String intent){
    public static AssistantQuery parse(String input){
        String text=input.toLowerCase(Locale.ROOT).replace('ё','е');var match=Pattern.compile("(?:до|не\\s+(?:дороже|более))\\s*(\\d+(?:[ \\u00a0]\\d{3})*(?:[.,]\\d{1,2})?)").matcher(text);BigDecimal budget=null;String termText=text;
        if(Pattern.compile("\\b(?:руб|рубл\\p{L}*|usd|eur|доллар\\p{L}*|евро)\\b|[$€₽]").matcher(text).find())throw new IllegalArgumentException("Цены магазина указаны в тенге. Укажите бюджет в ₸: валютный курс не подключён");
        if(match.find()){budget=new BigDecimal(match.group(1).replace(" ","").replace("\u00a0","").replace(',','.'));termText=text.substring(0,match.start())+" "+text.substring(match.end());}
        boolean no=Pattern.compile("(?:без|не)\\s+(?:со\\s+)?скид").matcher(text).find();boolean discount=!no&&(text.contains("скид")||text.contains("акци"));
        var stop=Set.of("найди","товар","товары","мне","подбери","покажи","посоветуй","недорогие","недорогой","дешевые","дешевый","сравни","эти","что","сейчас","есть","со","до","тенге","kzt","все","альтернативу","похожие","пожалуйста","не","дороже","скидкой","скидки","скидок","акции");
        var terms=Arrays.stream(termText.split("[^\\p{L}\\p{N}-]+")).filter(t->t.length()>2&&!t.matches("\\d+")&&!stop.contains(t)).toList();
        String intent=text.contains("сравн")?"COMPARE":text.contains("альтернатив")||text.contains("похож")?"ALTERNATIVES":discount?"DISCOUNTS":budget!=null?"BUDGET":"SEARCH";
        return new AssistantQuery(text,budget,discount,no,terms,intent);
    }
}
