package com.aria.controller;

import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import com.aria.repository.SocialOfferRepository;
import com.aria.service.SocialActService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Existing ADMIN and organization-issuer guards apply to the entire /api/admin prefix. */
@RestController
@RequestMapping("/api/admin/control/social")
public class SocialDocumentController {
    private final SocialOfferRepository offers;
    private final AppUserRepository users;
    private final SocialActService acts;
    public SocialDocumentController(SocialOfferRepository offers,AppUserRepository users,SocialActService acts){this.offers=offers;this.users=users;this.acts=acts;}
    @Transactional(readOnly=true)
    @GetMapping("/{id}/act")
    public ResponseEntity<String> act(@PathVariable Long id){
        var offer=offers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Передача не найдена"));
        if(offer.getOrgId()==null||!java.util.Set.of("CLAIMED","TRANSFERRED").contains(offer.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Акт доступен для подтверждённого резерва или выдачи");
        var organization=users.findById(offer.getOrgId()).filter(u->u.getRole()==Role.ORG).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Получатель не найден. Проверьте реквизиты документа"));
        return acts.document(offer,organization.getFullName());
    }
}
