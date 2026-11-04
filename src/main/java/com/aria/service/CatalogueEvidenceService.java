package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import org.springframework.stereotype.Service;
import java.util.*;

/** Immutable references ship with the application; never fetches a product URL. */
@Service
public class CatalogueEvidenceService {
    public record Fact(String label,String value,String sourceUrl,String checkedAt) {}
    public record Photo(String path,String sourceUrl,String author,String license,String licenseUrl,String note) {}
    public record Entry(String referenceId,String sku,String name,String brand,String category,String size,
                        String description,String verification,List<Fact> facts,Photo photo,List<String> limitations) {}
    private record Catalogue(int schemaVersion,List<Entry> entries) {}
    private final List<Entry> entries;
    private final Map<String,Entry> references;

    public CatalogueEvidenceService(){
        try(var input=getClass().getResourceAsStream("/catalogue/demo-products.json")){
            if(input==null)throw new IllegalStateException("Catalogue reference not found");
            var source=tools.jackson.databind.json.JsonMapper.builder().build().readValue(input,Catalogue.class);
            if(source.schemaVersion()!=1)throw new IllegalStateException("Unsupported catalogue reference");
            entries=List.copyOf(source.entries());
            var map=new LinkedHashMap<String,Entry>();var codes=new HashSet<String>();
            for(var entry:entries){
                if(map.put(entry.referenceId(),entry)!=null||!codes.add(entry.sku()))throw new IllegalStateException("Duplicate catalogue identity");
                if(!entry.sku().matches("ARIA-DEMO-\\d{3}")||entry.facts().isEmpty())throw new IllegalStateException("Invalid catalogue identity");
                if(entry.photo()!=null&&(!entry.photo().path().startsWith("/images/demo/")||!entry.photo().license().equals("CC0 1.0")))throw new IllegalStateException("Photo permission must be verified");
            }
            references=Collections.unmodifiableMap(map);
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot read catalogue reference",exception);}
    }
    public List<Entry> entries(){return entries;}
    public Map<String,Object> view(Product product,ProductDetail detail){
        var result=new LinkedHashMap<String,Object>();
        boolean demo=detail!=null&&detail.getDemonstration();result.put("demonstration",demo);result.put("verification","UNVERIFIED");
        if(detail==null||detail.getCatalogueReference()==null)return result;
        var entry=references.get(detail.getCatalogueReference());
        if(entry==null||!Objects.equals(entry.name(),product.getName())||!Objects.equals(entry.brand(),detail.getBrand())||!Objects.equals(entry.category(),product.getCategory())||!Objects.equals(entry.size(),detail.getSize())){
            result.put("verification","OWNER_EDITED");return result;
        }
        result.put("reference",entry.referenceId());result.put("verification",entry.verification());result.put("facts",entry.facts());result.put("limitations",entry.limitations());
        result.put("photo",entry.photo()!=null&&Objects.equals(product.getImageUrl(),entry.photo().path())?entry.photo():null);
        result.put("catalogueOrder",entries.indexOf(entry));
        return result;
    }
}
