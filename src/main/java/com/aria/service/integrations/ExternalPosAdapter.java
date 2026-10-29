package com.aria.service.integrations;
import java.util.*;
import java.math.BigDecimal;
/** Mapping contract, deliberately separate from a transport or a fiscal receipt. */
public interface ExternalPosAdapter {
 record MappedLine(Long ariaProductId,String externalProductId,int quantity,BigDecimal unitPrice){}
 record MappingPlan(String provider,String requestKey,String currency,List<MappedLine> lines,boolean sent,boolean fiscal){}
 MappingPlan prepare(PosAdapter.Sale sale,Map<Long,String> productIds);
}
