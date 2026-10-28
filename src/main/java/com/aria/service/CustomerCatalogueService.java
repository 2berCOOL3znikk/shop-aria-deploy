package com.aria.service;
import com.aria.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/** One explicit allowlist; never serializes Product, purchase costs or internal reasons. */
@Service
public class CustomerCatalogueService {
    private final ProductRepository products;private final PublicProductView view;
    public CustomerCatalogueService(ProductRepository products,PublicProductView view){this.products=products;this.view=view;}
    @Transactional(readOnly=true) public List<Map<String,Object>> available(){
        return products.findAll().stream().filter(p->!p.getArchived()&&p.getUnitPrice()!=null&&p.getUnitPrice().signum()>0).map(view::summary)
            .filter(p->((Number)p.get("quantityOnHand")).intValue()>0).map(p->{var safe=new LinkedHashMap<String,Object>();for(String key:List.of("id","sku","name","category","unit","displayPrice","discountPercent","brand","size"))if(p.get(key)!=null)safe.put(key,p.get(key));return (Map<String,Object>)safe;}).toList();
    }
}
