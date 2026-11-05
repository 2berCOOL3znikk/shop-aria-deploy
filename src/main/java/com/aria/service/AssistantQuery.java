package com.aria.service;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
public record AssistantQuery(String text,BigDecimal budget,boolean discountsOnly,boolean withoutDiscounts,List<String> terms,String intent){
    public static String supportIntent(String input){
        String text=input.toLowerCase(Locale.ROOT).replace('ё','е');
        if(Pattern.compile("\\b(?:write|generate|create|solve|do|help(?:\\s+me)?).{0,90}(?:code|script|python|javascript|java\\b|homework|essay|equation)\\b").matcher(text).find())return "OUT_OF_SCOPE";
        if(Pattern.compile("\\b(?:my|our|where|status|history).{0,30}\\borders?\\b|\\borders?\\b.{0,25}\\b(?:status|history)\\b").matcher(text).find())return "ORDERS_HELP";
        if(Pattern.compile("\\b(?:delivery|shipping|returns?|refunds?)\\b").matcher(text).find())return "STORE_HELP";
        if(Pattern.compile("(?:напиши|написать|создай|сгенерируй|реши|сделай|помоги).{0,90}(?:код|скрипт|программ[уы]|python|javascript|java\\b|сочинени|реферат|домашн|уравнен)|(?:код|скрипт).{0,35}(?:python|javascript|java\\b)|(?:домашн.{0,15}задани)").matcher(text).find())return "OUT_OF_SCOPE";
        if(Pattern.compile("(?:мой|моего|мои|статус|где|история).{0,30}заказ|заказ.{0,25}(?:статус|история)").matcher(text).find())return "ORDERS_HELP";
        if(Pattern.compile("доставк|возврат|вернуть.{0,15}товар").matcher(text).find())return "STORE_HELP";
        return "CATALOGUE";
    }
    public static AssistantQuery parse(String input){
        String text=input.toLowerCase(Locale.ROOT).replace('ё','е');var match=Pattern.compile("(?:до|не\\s+(?:дороже|более)|up\\s+to|under|less\\s+than|no\\s+more\\s+than|not\\s+over)\\s*(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:[ \\u00a0]\\d{3})*(?:[.,]\\d{1,2})?)").matcher(text);BigDecimal budget=null;String termText=text;
        if(Pattern.compile("\\b(?:руб|рубл\\p{L}*|usd|eur|доллар\\p{L}*|евро|dollars?|euros?|rubles?|rub)\\b|[$€₽]").matcher(text).find())throw new IllegalArgumentException("Цены магазина указаны в тенге. Укажите бюджет в ₸: валютный курс не подключён");
        if(match.find()){String amount=match.group(1).replace(" ","").replace("\u00a0","");amount=amount.matches("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?")?amount.replace(",",""):amount.replace(',','.');budget=new BigDecimal(amount);termText=text.substring(0,match.start())+" "+text.substring(match.end());}
        boolean no=Pattern.compile("(?:без|не)\\s+(?:со\\s+)?скид|\\b(?:without|no|not)\\s+(?:on\\s+)?(?:a\\s+)?(?:discount\\w*|sale|promotion\\w*)\\b").matcher(text).find();boolean discount=!no&&(text.contains("скид")||text.contains("акци")||Pattern.compile("\\b(?:discount\\w*|promotions?|on\\s+sale)\\b").matcher(text).find());
        var stop=Set.of("найди","товар","товары","мне","подбери","покажи","посоветуй","недорогие","недорогой","дешевые","дешевый","сравни","эти","что","сейчас","есть","со","до","тенге","kzt","все","альтернативу","похожие","пожалуйста","не","дороже","скидкой","скидки","скидок","акции","available","options","product","products","please","show","find","me","compare","these","selected","items","item","all","under","less","than","up","to","without","no","not","on","sale","discount","discounts","discounted","promotion","promotions","tenge","some","more","over","similar","alternative","alternatives","suggest","recommend","what","which","current","currently","now","can","you","like","cheap","budget","affordable","with","that","the","and","for","are","there","any","need","would","look","looking","is");
        var terms=Arrays.stream(termText.split("[^\\p{L}\\p{N}-]+")).filter(t->t.length()>2&&!t.matches("\\d+")&&!stop.contains(t)).toList();
        String intent=text.contains("сравн")||text.contains("compare")?"COMPARE":text.contains("альтернатив")||text.contains("похож")||text.contains("alternative")||text.contains("similar")?"ALTERNATIVES":discount?"DISCOUNTS":budget!=null?"BUDGET":"SEARCH";
        return new AssistantQuery(text,budget,discount,no,terms,intent);
    }
    /** Explicit search aliases, not inferred product facts or a translation of merchant data. */
    public static List<String> aliases(String term){return switch(term){case "milk"->List.of("молок");case "chocolate"->List.of("шокол");case "bread"->List.of("хлеб");case "rice"->List.of("рис");case "pasta"->List.of("макарон","паста");case "beans"->List.of("фасоль","боб");case "water"->List.of("вода","воды");case "juice"->List.of("сок");case "tea"->List.of("чай");case "coffee"->List.of("кофе");case "towel"->List.of("полотен");case "shirt","t-shirt","tshirt"->List.of("футбол","рубаш");default->List.of();};}
}
