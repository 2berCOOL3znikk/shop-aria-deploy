package com.aria.service.integrations;
import org.springframework.stereotype.Component;
import java.util.*;
/** Validates owner-supplied ID format only; remote IDs are not verified without access. No HTTP or fiscalization. */
@Component
public class PosterPosAdapter implements ExternalPosAdapter {
 @Override public MappingPlan prepare(PosAdapter.Sale sale,Map<Long,String> ids){
  var mapped=new ArrayList<MappedLine>();for(var line:sale.lines()){
   String id=ids.get(line.productId());if(id==null||!id.matches("[0-9]{1,20}"))throw new IllegalArgumentException("Нет подтверждённого Poster product_id для ARIA #"+line.productId());
   mapped.add(new MappedLine(line.productId(),id,line.quantity(),line.expectedPrice()));
  }
  return new MappingPlan("POSTER",sale.requestKey(),"KZT",List.copyOf(mapped),false,false);
 }
}
