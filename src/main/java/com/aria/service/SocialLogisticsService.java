package com.aria.service;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional
public class SocialLogisticsService {
 private final SocialProposalRepository proposals;private final SocialOfferRepository offers;private final AppUserRepository users;private final AuditService audit;
 public SocialLogisticsService(SocialProposalRepository p,SocialOfferRepository o,AppUserRepository u,AuditService a){proposals=p;offers=o;users=u;audit=a;}
 @Transactional(readOnly=true) public List<Map<String,Object>> staff(){return users.findAll().stream().filter(u->u.getRole()==com.aria.enums.Role.ADMIN).map(u->Map.<String,Object>of("id",u.getId(),"name",u.getFullName()==null?"Сотрудник #"+u.getId():u.getFullName())).toList();}
 public Map<String,Object> plan(Long id,long version,Long assignee,LocalDateTime at,String state,String reason){
  var p=proposals.lockById(id).orElseThrow(()->new IllegalArgumentException("Заявка не найдена"));if(p.version!=version)throw new IllegalStateException("Логистика изменилась; обновите страницу");
  if(p.offerId==null||offers.findById(p.offerId).filter(o->"CLAIMED".equals(o.getStatus())).isEmpty())throw new IllegalStateException("Логистика доступна после подтверждённого резерва и до фактической выдачи");
  users.findById(assignee).filter(u->u.getRole()==com.aria.enums.Role.ADMIN).orElseThrow(()->new IllegalArgumentException("Назначьте действующего сотрудника ADMIN"));
  var path=List.of("UNPLANNED","SCHEDULED","READY","IN_TRANSIT");int before=path.indexOf(p.logisticsStatus),after=path.indexOf(state);if(after<1||after<before||after>before+1)throw new IllegalStateException("Переходите последовательно: назначено → готово → в пути. Выдача подтверждается отдельно");
  if(at.toLocalDate().isAfter(p.validUntil)||(!Objects.equals(at,p.plannedAt)&&at.isBefore(LocalDateTime.now())))throw new IllegalArgumentException("Время передачи должно быть согласовано в пределах срока выдачи, не в прошлом");
  p.assigneeId=assignee;p.plannedAt=at;p.logisticsStatus=state;audit.record("SOCIAL_LOGISTICS_UPDATED","proposal:"+id,state+" · сотрудник "+assignee+" · "+at+" · "+reason);return Map.of("status",state,"plannedAt",at,"assigneeId",assignee);
 }
}
