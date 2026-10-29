package com.aria.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Provider boundary. Implementations must not mutate local stock while reading external snapshots. */
public interface StoreConnector {
 enum Platform { WOOCOMMERCE, SHOPIFY, KASPI }
 enum Permission { READ_CATALOG, READ_ORDERS, WRITE_PRICE, WRITE_STOCK }
 record Access(Platform platform,List<Permission> verifiedPermissions,Instant checkedAt) {
  public Access { verifiedPermissions=List.copyOf(verifiedPermissions); }
 }
 // No cost, expiry or batches: these are not established by a catalogue API.
 record ProductSnapshot(String externalId,String sku,String name,String currency,BigDecimal price,
                        Integer availableQuantity,String externalVersion,Instant observedAt) {}
 record OrderLine(String externalProductId,int quantity,BigDecimal lineTotal) {}
 record OrderSnapshot(String externalId,String status,String currency,List<OrderLine> lines,
                      Instant modifiedAt,Instant observedAt) {
  public OrderSnapshot { lines=List.copyOf(lines); }
 }
 record Page<T>(List<T> items,String nextCursor) {
  public Page { items=List.copyOf(items); }
 }
 Access verifyAccess();
 Page<ProductSnapshot> products(String cursor);
 Page<OrderSnapshot> orders(String cursor);
 // Deliberately no write method until permissions, mapping, conflict handling and genuine E2E are verified.
}
