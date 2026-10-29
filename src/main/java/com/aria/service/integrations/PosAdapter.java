package com.aria.service.integrations;
import java.math.BigDecimal;
import java.util.List;
/** Domain boundary for an approved external POS adapter. No fiscal/provider integration is implied. */
public interface PosAdapter {
 record Line(Long productId,int quantity,BigDecimal expectedPrice){}
 record Sale(String requestKey,List<Line> lines){}
 Object record(Sale sale,String operator);
}
