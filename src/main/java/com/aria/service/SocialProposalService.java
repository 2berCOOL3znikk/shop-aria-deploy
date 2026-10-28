package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional
public class SocialProposalService {
    private final SocialProposalRepository proposals;private final PricingDecisionRepository decisions;private final InventoryService inventory;
    private final SmartPricingService smart;private final SocialService social;private final CharityEligibilityService charities;private final SocialOfferRepository offers;private final AuditService audit;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    public SocialProposalService(SocialProposalRepository proposals,PricingDecisionRepository decisions,InventoryService inventory,SmartPricingService smart,SocialService social,CharityEligibilityService charities,SocialOfferRepository offers,AuditService audit){this.proposals=proposals;this.decisions=decisions;this.inventory=inventory;this.smart=smart;this.social=social;this.charities=charities;this.offers=offers;this.audit=audit;}
    public Map<String,Object> propose(Long decisionId){
        var report=smart.currentForProposal(decisionId);var decision=decisions.findById(decisionId).orElseThrow();var p=inventory.lock(decision.productId);
        var old=proposals.findByDecisionId(decisionId);if(old.isPresent())return view(old.get());
        var scenarios=(List<?>)report.get("scenarios");double risk=scenarios.stream().mapToDouble(s->((Number)((Map<?,?>)s).get("estimatedExpiredUnits")).doubleValue()).min().orElse(0);
        if(risk<=0||inventory.available(p)==0)throw new IllegalStateException("Нет рассчитанного риска списания пригодного остатка");
        var item=new SocialProposal();item.decisionId=decisionId;item.productId=p.getId();item.quantity=Math.min(inventory.available(p),(int)Math.ceil(risk));item.validUntil=p.getExpiryDate()==null?LocalDate.now().plusDays(1):p.getExpiryDate();
        item.reason="Оценочный риск непроданного остатка. Требуются проверенная организация и подтверждение пригодности/хранения. Товар пока не зарезервирован.";proposals.saveAndFlush(item);audit.record("SOCIAL_PROPOSED","proposal:"+item.id,item.quantity+" ед. · без резерва");return view(item);
    }
    @Transactional(readOnly=true) public List<Map<String,Object>> list(){return proposals.findTop100ByOrderByCreatedAtDescIdDesc().stream().map(this::view).toList();}
    public Map<String,Object> approve(Long id,Long organizationId,int quantity,LocalDate until,String reason){
        var item=proposals.lockById(id).orElseThrow(()->new IllegalArgumentException("Предложение не найдено"));var p=inventory.lock(item.productId);em.refresh(item);inventory.synchronize(p);
        if(!"PROPOSED".equals(item.status))throw new IllegalStateException("Предложение уже обработано");
        if(charities.matching(p).stream().noneMatch(o->organizationId.equals(o.get("id"))))throw new IllegalStateException("Нет подходящей подтверждённой организации с указанными условиями приёма");
        if(quantity<1||quantity>item.quantity||quantity>inventory.available(p)||until.isBefore(LocalDate.now())||until.isAfter(item.validUntil)||p.getExpiryDate()!=null&&until.isAfter(p.getExpiryDate()))throw new IllegalStateException("Проверьте количество, пригодность и срок выдачи");
        var offer=social.publishOffer(p.getId(),quantity,until);social.claimOffer(offer.getId(),organizationId);
        item.offerId=offer.getId();item.organizationId=organizationId;item.quantity=quantity;item.validUntil=until;item.status="RESERVED";item.approvedAt=LocalDateTime.now();var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();item.approvedBy=auth==null?"system":auth.getName();
        audit.record("SOCIAL_PROPOSAL_APPROVED","proposal:"+id,"Пригодность, хранение и транспорт подтверждены · "+reason);return view(item);
    }
    public Map<String,Object> cancel(Long id,String reason){
        var item=proposals.lockById(id).orElseThrow(()->new IllegalArgumentException("Предложение не найдено"));if(item.offerId!=null)offers.lockById(item.offerId);inventory.lock(item.productId);em.refresh(item);
        if("PROPOSED".equals(item.status))item.status="CANCELLED";
        else if("RESERVED".equals(item.status)){social.finish(item.offerId,true,reason);item.status="CANCELLED";}
        else throw new IllegalStateException("Предложение уже закрыто");audit.record("SOCIAL_PROPOSAL_CANCELLED","proposal:"+id,reason);return view(item);
    }
    private Map<String,Object> view(SocialProposal p){var row=new LinkedHashMap<String,Object>();row.put("id",p.id);row.put("decisionId",p.decisionId);row.put("productId",p.productId);row.put("quantity",p.quantity);row.put("validUntil",p.validUntil);row.put("reason",p.reason);row.put("createdAt",p.createdAt);row.put("organizationId",p.organizationId);row.put("offerId",p.offerId);row.put("approvedAt",p.approvedAt);row.put("approvedBy",p.approvedBy);row.put("status",p.offerId==null?p.status:offers.findById(p.offerId).map(o->"TRANSFERRED".equals(o.getStatus())?"TRANSFERRED":"CANCELLED".equals(o.getStatus())?"CANCELLED":p.status).orElse(p.status));return row;}
}
