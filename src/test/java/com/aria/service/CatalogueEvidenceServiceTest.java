package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.ProductDetail;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CatalogueEvidenceServiceTest {
    private final CatalogueEvidenceService evidence=new CatalogueEvidenceService();
    @Test void catalogueHasUniqueCommercialReferencesAndOnlyTheDocumentedImage(){
        assertThat(evidence.entries()).hasSize(16);
        assertThat(evidence.entries().stream().map(CatalogueEvidenceService.Entry::sku).distinct()).hasSize(16);
        assertThat(evidence.entries()).allMatch(e->!e.name().matches(".*\\d{13}.*")&&!e.facts().isEmpty());
        assertThat(evidence.entries().stream().filter(e->e.photo()!=null)).hasSize(1).allMatch(e->e.photo().license().equals("CC0 1.0"));
        var can=evidence.entries().getLast();assertThat(can.size()).isNull();assertThat(can.verification()).isEqualTo("PACKAGE_PHOTO");
        assertThat(can.facts()).noneMatch(f->f.label().contains("Масса")||f.label().contains("Состав"));
    }
    @Test void changedIdentityDoesNotInheritVerificationOrAnUnrelatedPhoto(){
        var entry=evidence.entries().getLast();var product=new Product();product.setName(entry.name());product.setCategory(entry.category());product.setImageUrl(entry.photo().path());
        var detail=new ProductDetail();detail.setBrand(entry.brand());detail.setSize(entry.size());detail.setCatalogueReference(entry.referenceId());detail.setDemonstration(true);
        assertThat(evidence.view(product,detail)).containsEntry("verification","PACKAGE_PHOTO").containsEntry("demonstration",true).containsKey("photo");
        product.setImageUrl("/api/products/12/photo");assertThat(evidence.view(product,detail).get("photo")).isNull();
        product.setName("Другой товар");assertThat(evidence.view(product,detail)).containsEntry("verification","OWNER_EDITED").containsEntry("demonstration",true).doesNotContainKeys("facts","photo");
        assertThat(evidence.view(product,null)).containsEntry("verification","UNVERIFIED").containsEntry("demonstration",false);
    }
}
