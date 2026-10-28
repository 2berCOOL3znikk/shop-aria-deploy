package com.aria.service.pricing;
import java.util.*;
/** Small strict CSV reader: RFC-style quoting, comma/semicolon, CRLF, BOM, bounded input. */
public final class CompetitorCsv {
    private CompetitorCsv(){}
    public static List<List<String>> read(String raw){
        if(raw==null||raw.isBlank()||raw.length()>200000)throw new IllegalArgumentException("CSV пуст или больше 200000 символов");
        String text=raw.startsWith("\uFEFF")?raw.substring(1):raw;
        String first=text.split("\\R",2)[0];char delimiter=first.contains(";")?';':',';
        var rows=new ArrayList<List<String>>();var row=new ArrayList<String>();var cell=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(quoted){if(c=='"'){if(i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}else cell.append(c);}
            else if(c=='"'){if(!cell.isEmpty()||closed)throw new IllegalArgumentException("Некорректные кавычки CSV");quoted=true;}
            else if(c==delimiter||c=='\n'||c=='\r'){
                row.add(cell.toString().trim());cell.setLength(0);closed=false;
                if(c!=delimiter){if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;if(row.stream().anyMatch(v->!v.isBlank()))rows.add(List.copyOf(row));row.clear();}
            }else {if(closed&&!Character.isWhitespace(c))throw new IllegalArgumentException("Текст после закрытой кавычки CSV");if(!closed)cell.append(c);}
            if(cell.length()>2000||row.size()>7||rows.size()>201)throw new IllegalArgumentException("Превышен размер строки или 200 записей CSV");
        }
        if(quoted)throw new IllegalArgumentException("Не закрыты кавычки CSV");
        if(!row.isEmpty()||!cell.isEmpty()||closed){row.add(cell.toString().trim());rows.add(List.copyOf(row));}
        if(rows.isEmpty()||!rows.getFirst().equals(List.of("sku","barcode","unit","price","currency","observedAt","url")))throw new IllegalArgumentException("Заголовок CSV: sku,barcode,unit,price,currency,observedAt,url");
        if(rows.size()>201)throw new IllegalArgumentException("Не более 200 записей за импорт");return rows.subList(1,rows.size());
    }
}
