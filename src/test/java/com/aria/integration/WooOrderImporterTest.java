package com.aria.integration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WooOrderImporterTest {
 private Map<String,Object> order(){return new HashMap<>(Map.of("id",11,"status","completed","currency","KZT","total","1000.00","date_modified_gmt","2026-10-30T08:00:00","line_items",List.of(Map.of("product_id",1,"variation_id",2,"quantity",2,"total","1000.00","name","PRIVATE CUSTOM NAME")),"refunds",List.of(Map.of("total","-100.00")),"billing",Map.of("email","PRIVATE@example.test")));}
 @Test void extractsOnlyOrderFactsAndVariationId(){var importer=new WooOrderImporter(mock(WooCommerceReader.class));var item=importer.parse(order());assertEquals("KZT",item.currency());assertEquals("2",item.lines().getFirst().productId());assertEquals("100.00",item.refundedTotal().toPlainString());assertNull(item.createdAtUtc());assertNull(item.completedAtUtc());assertFalse(item.toString().contains("PRIVATE"));}
 @Test void deletedProductDoesNotInventAProductId(){var row=order();row.put("line_items",List.of(Map.of("product_id",0,"variation_id",0,"quantity",1,"total","1000.00")));assertNull(new WooOrderImporter(mock(WooCommerceReader.class)).parse(row).lines().getFirst().productId());}
 @Test void rejectsInvalidAndMissingFacts(){var importer=new WooOrderImporter(mock(WooCommerceReader.class));for(var field:List.of("currency","date_modified_gmt","total")){var row=order();row.remove(field);assertThrows(IllegalStateException.class,()->importer.parse(row));}var row=order();row.put("refunds",List.of(Map.of("total","-2000.00")));assertThrows(IllegalStateException.class,()->importer.parse(row));}
 @Test void rejectsDuplicatesAndBoundsPagination(){var reader=mock(WooCommerceReader.class);when(reader.page(anyString(),anyString(),anyString(),anyString())).thenReturn(new WooCommerceReader.Page(List.of(order(),order()),false));assertThrows(IllegalStateException.class,()->new WooOrderImporter(reader).read("shop","k","s"));when(reader.page(anyString(),anyString(),anyString(),anyString())).thenReturn(new WooCommerceReader.Page(List.of(),true));assertThrows(IllegalStateException.class,()->new WooOrderImporter(reader).read("shop","k","s"));}
}
