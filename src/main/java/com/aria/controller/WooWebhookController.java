package com.aria.controller;
import com.aria.service.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class WooWebhookController {
 private final StoreWebhookService webhooks;private final StoreSyncWorker worker;
 public WooWebhookController(StoreWebhookService s,StoreSyncWorker w){webhooks=s;worker=w;}
 @PostMapping("/api/webhooks/woocommerce/{token}/{resource}")
 public org.springframework.http.ResponseEntity<Void> receive(@PathVariable String token,@PathVariable String resource,jakarta.servlet.http.HttpServletRequest request)throws java.io.IOException{
  if(request.getContentLengthLong()>2_000_000)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
  byte[] body=request.getInputStream().readNBytes(2_000_001);if(body.length>2_000_000)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
  boolean accepted=webhooks.receive(token,resource,request.getHeader("X-WC-Webhook-Signature"),request.getContentType(),body);
  if(accepted)try{worker.wake();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Webhook task persisted; wake deferred");}
  return org.springframework.http.ResponseEntity.noContent().build();
 }
}
