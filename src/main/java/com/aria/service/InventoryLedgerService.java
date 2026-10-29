package com.aria.service;
import com.aria.repository.InventoryMovementRepository;
import com.aria.entity.InventoryMovement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
@Service @Transactional(readOnly=true)
public class InventoryLedgerService {
 private final InventoryMovementRepository movements;
 public InventoryLedgerService(InventoryMovementRepository m){movements=m;}
 public Map<String,Object> report(Long productId,int page,int size){if(page<0||page>1000000||size<1||size>100)throw new IllegalArgumentException("Неверная страница");var rows=movements.findByTypeAndCreatedAtGreaterThanEqual("WRITE_OFF",LocalDate.now().minusDays(29).atStartOfDay());long units=0,unknown=0;BigDecimal cost=BigDecimal.ZERO;for(var r:rows){if(productId!=null&&!productId.equals(r.productId))continue;units-=r.quantityDelta;if(r.unitCost==null)unknown-=r.quantityDelta;else cost=cost.add(r.unitCost.multiply(BigDecimal.valueOf(-r.quantityDelta)));}
  org.springframework.data.jpa.domain.Specification<InventoryMovement> spec=(root,query,cb)->productId==null?cb.conjunction():cb.equal(root.get("productId"),productId);
  var result=movements.findAll(spec,org.springframework.data.domain.PageRequest.of(page,size,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt","id")));
  return Map.of("movements",com.aria.dto.AdminViews.PageResult.from(result),"writtenOffUnits30",units,"recordedWriteOffCost30",cost,"unknownCostUnits30",unknown,"note","Структурированные движения сохраняются с включения этого модуля. Начальный снимок не является новой поставкой или восстановленной историей. WRITE_OFF — оформленные потери; просроченные ещё не списанные партии — отдельный риск. Карантин не увеличивает пригодный остаток.");}
}
