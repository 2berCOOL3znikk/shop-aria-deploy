package com.aria.service;
import com.aria.entity.*;
import com.aria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service @Transactional(readOnly=true)
public class CharityEligibilityService {
    private final OrganizationVerificationRepository verified;private final CharityAcceptanceRepository preferences;private final AppUserRepository users;private final AuditService audit;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    public CharityEligibilityService(OrganizationVerificationRepository verified,CharityAcceptanceRepository preferences,AppUserRepository users,AuditService audit){this.verified=verified;this.preferences=preferences;this.users=users;this.audit=audit;}
    public List<Map<String,Object>> organizations(){return verified.findAllByStatusOrderBySubmittedAtAsc("VERIFIED").stream().filter(v->users.findById(v.getUserId()).filter(u->u.getRole()==com.aria.enums.Role.ORG).isPresent()).map(v->{
        var row=new LinkedHashMap<String,Object>();row.put("id",v.getUserId());row.put("name",v.getOrganizationName());row.put("verification","VERIFIED");
        preferences.findById(v.getUserId()).ifPresent(p->{row.put("region",p.region);row.put("categories",json.readValue(p.categoriesJson,List.class));row.put("storageConditions",p.storageConditions);row.put("minimumRemainingDays",p.minimumRemainingDays);row.put("active",p.active);});return (Map<String,Object>)row;
    }).toList();}
    public List<Map<String,Object>> matching(Product p){return organizations().stream().filter(o->Boolean.TRUE.equals(o.get("active"))&&((List<?>)o.get("categories")).contains(PolicyService.categoryKey(p.getCategory()))&&(p.getExpiryDate()==null||!p.getExpiryDate().isBefore(LocalDate.now().plusDays(((Number)o.get("minimumRemainingDays")).intValue())))).toList();}
    @Transactional public void save(Long id,String region,List<String> categories,String storage,int days,boolean active){
        if(verified.findByUserId(id).filter(v->"VERIFIED".equals(v.getStatus())).isEmpty()||users.findById(id).filter(u->u.getRole()==com.aria.enums.Role.ORG).isEmpty())throw new IllegalStateException("Организация не подтверждена");
        var names=categories.stream().map(PolicyService::categoryKey).distinct().sorted().toList();if(names.isEmpty()||names.contains("*"))throw new IllegalArgumentException("Укажите точные принимаемые категории");
        var p=preferences.findById(id).orElseGet(CharityAcceptance::new);p.organizationId=id;p.region=region.trim();p.categoriesJson=json.writeValueAsString(names);p.storageConditions=storage.trim();p.minimumRemainingDays=days;p.active=active;preferences.save(p);audit.record("CHARITY_ACCEPTANCE_SAVED","organization:"+id,"Условия приёма и хранения подтверждены администратором");
    }
}
