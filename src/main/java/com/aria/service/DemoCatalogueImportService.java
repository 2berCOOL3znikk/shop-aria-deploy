package com.aria.service;

import com.aria.config.OrganizationBoundary;
import com.aria.dto.ControlRequests.*;
import com.aria.entity.*;
import com.aria.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class DemoCatalogueImportService {
    private final CatalogueEvidenceService evidence;
    private final ControlService control;
    private final InventoryService inventory;
    private final ProductRepository products;
    private final ProductDetailRepository details;
    private final OrganizationBoundary boundary;
    private final AuditService audit;
    private final EntityManager em;
    @Value("${aria.demo-catalog-import-enabled:false}") private boolean enabled;
    private static final List<String> EDUCATIONAL_PRICES=List.of("790","1450","1150","1190","1290","1290","890","990","1050","1790","1950","2290","6990","6490","28900","1590");

    public DemoCatalogueImportService(CatalogueEvidenceService e,ControlService c,InventoryService i,ProductRepository p,ProductDetailRepository d,OrganizationBoundary b,AuditService a,EntityManager em){evidence=e;control=c;inventory=i;products=p;details=d;boundary=b;audit=a;this.em=em;}
    private boolean eligible(Organization organization){return enabled&&"demo".equals(boundary.key())&&organization!=null&&organization.demonstration;}
    @Transactional(readOnly=true)
    public Map<String,Object> status(){
        boolean allowed=eligible(em.find(Organization.class,1));
        return Map.of("available",allowed,"references",evidence.entries().size(),"imported",em.createQuery("select count(d) from ProductDetail d where d.demonstration=true and d.catalogueReference is not null",Long.class).getSingleResult(),"note",allowed?"Добавятся учебные карточки и партии. Цены, закупка, остатки и даты не являются данными реального магазина. Повтор не восстанавливает проданный запас.":"Импорт учебного каталога выключен. Доступен только в отдельно разрешённой демонстрационной компании.");
    }
    @Transactional
    public Map<String,Object> populate(boolean confirmed){
        if(!confirmed)throw new IllegalArgumentException("Подтвердите создание учебных карточек и партий");
        var organization=em.find(Organization.class,1,LockModeType.PESSIMISTIC_WRITE);
        if(!eligible(organization))throw new ResponseStatusException(HttpStatus.CONFLICT,"Учебный импорт не разрешён для этой компании");
        var created=new ArrayList<Long>();var preserved=new ArrayList<Long>();int index=0;
        for(var entry:evidence.entries()){
            var existing=products.findBySkuIgnoreCase(entry.sku());
            if(existing.isPresent()){
                var detail=details.findByProductId(existing.get().getId()).orElse(null);
                if(detail==null||!detail.getDemonstration()||!entry.referenceId().equals(detail.getCatalogueReference()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Один из учебных артикулов уже занят. Существующие товары не изменены.");
                preserved.add(existing.get().getId());index++;continue;
            }
            var price=new BigDecimal(EDUCATIONAL_PRICES.get(index));var cost=price.multiply(new BigDecimal("0.60")).setScale(2,RoundingMode.HALF_UP);
            var product=control.saveProduct(null,new ProductInput(entry.sku(),entry.name(),entry.category(),price,entry.brand(),entry.description(),entry.photo()==null?"/images/product-placeholder.svg":entry.photo().path(),null,"шт",3,cost));
            var detail=details.findByProductId(product.getId()).orElseThrow();detail.setCatalogueReference(entry.referenceId());detail.setDemonstration(true);detail.setSize(entry.size());details.saveAndFlush(detail);
            // These dates/costs are explicitly educational, never manufacturer facts.
            int days=entry.category().equals("Молочные продукты")?14:90;
            LocalDate expiry=entry.category().equals("Бытовые товары")?null:LocalDate.now().plusDays(days);
            inventory.save(null,new LotInput(product.getId(),null,"DEMO-CATALOG-V1",LocalDate.now(),null,expiry,12,cost));
            created.add(product.getId());index++;
        }
        audit.record("DEMO_CATALOG_IMPORTED","demo-catalogue","Учебные карточки: создано "+created.size()+", сохранено без изменения "+preserved.size()+". История продаж не создавалась.");
        return Map.of("created",created,"preserved",preserved,"demonstration",true,"note","Учебные цены и партии созданы отдельно от проверенных характеристик. Существующие карточки, цены и остатки не перезаписывались.");
    }
}
