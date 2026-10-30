package com.aria.integration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class WooCatalogImporterTest {
 private Map<String,Object> simple(long id){return Map.of("id",id,"type","simple","name","Tea","price","123.45","manage_stock",true,"stock_quantity",3);}
 @Test void followsPagesAndLoadsVariationsWithoutInventingInheritedStock(){
  var reader=mock(WooCommerceReader.class);
  when(reader.page("shop","products?per_page=100&page=1","key","secret")).thenReturn(new WooCommerceReader.Page(List.of(simple(1),Map.of("id",2,"type","variable","name","Coffee")),true));
  when(reader.page("shop","products?per_page=100&page=2","key","secret")).thenReturn(new WooCommerceReader.Page(List.of(Map.of("id",4,"type","grouped")),false));
  when(reader.page("shop","products/2/variations?per_page=100&page=1","key","secret")).thenReturn(new WooCommerceReader.Page(List.of(Map.of("id",3,"price","500.00","manage_stock",false,"attributes",List.of(Map.of("option","Large")))),false));
  var catalog=new WooCatalogImporter(reader).read("shop","key","secret");assertEquals(2,catalog.items().size());assertEquals(1,catalog.skipped());var variant=catalog.items().get(1);assertEquals("2",variant.parentId());assertEquals("Coffee · Large",variant.name());assertNull(variant.quantity());
 }
 @Test void rejectsDuplicateIdsAndInvalidPrices(){var r=mock(WooCommerceReader.class);when(r.page(anyString(),anyString(),anyString(),anyString())).thenReturn(new WooCommerceReader.Page(List.of(simple(1),simple(1)),false));assertThrows(IllegalStateException.class,()->new WooCatalogImporter(r).read("shop","k","s"));var invalid=new HashMap<>(simple(2));invalid.put("price","invalid");when(r.page(anyString(),anyString(),anyString(),anyString())).thenReturn(new WooCommerceReader.Page(List.of(invalid),false));assertThrows(IllegalStateException.class,()->new WooCatalogImporter(r).read("shop","k","s"));}
 @Test void boundsNeverEndingPagination(){var r=mock(WooCommerceReader.class);when(r.page(anyString(),anyString(),anyString(),anyString())).thenReturn(new WooCommerceReader.Page(List.of(),true));assertThrows(IllegalStateException.class,()->new WooCatalogImporter(r).read("shop","k","s"));verify(r,times(11)).page(anyString(),anyString(),anyString(),anyString());}
}
