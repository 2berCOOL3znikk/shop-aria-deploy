package com.aria.controller;
import com.aria.enums.Role;
import com.aria.repository.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Objects;

/** A private product image is available only through an eligible offer or the recipient's own claim. */
@RestController
public class SocialOfferPhotoController {
 private final SocialOfferRepository offers;private final ProductPhotoRepository photos;private final AppUserRepository users;
 public SocialOfferPhotoController(SocialOfferRepository offers,ProductPhotoRepository photos,AppUserRepository users){this.offers=offers;this.photos=photos;this.users=users;}
 @GetMapping("/api/social/offers/{id}/photo") @Transactional(readOnly=true)
 public ResponseEntity<byte[]> photo(@PathVariable Long id,Authentication authentication){
  if(authentication==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  var user=users.findByEmail(authentication.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(user.getRole()!=Role.ADMIN&&user.getRole()!=Role.ORG)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  var offer=offers.findById(id).orElseThrow(this::unavailable);
  boolean open="OPEN".equals(offer.getStatus())&&offer.getQuantityAvailable()>0&&offer.getValidUntil()!=null&&!offer.getValidUntil().isBefore(LocalDate.now());
  if(user.getRole()!=Role.ADMIN&&!open&&!Objects.equals(offer.getOrgId(),user.getId()))throw unavailable();
  String assigned="/api/products/"+offer.getProduct().getId()+"/photo";
  if(!assigned.equals(offer.getProduct().getImageUrl()))throw unavailable();
  var photo=photos.findByProductId(offer.getProduct().getId()).orElseThrow(this::unavailable);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(photo.getContentType())).body(photo.getData());
 }
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Фото недоступно");}
}
