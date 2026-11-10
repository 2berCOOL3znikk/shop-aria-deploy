package com.aria.service;

import org.springframework.stereotype.Component;
import org.springframework.core.env.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

/** This adapter is deliberately local-only: a signed nonce, never the secret, to one owned test address. */
@Component
public class LocalWorkspaceProbe {
 private static final String TARGET="http://127.0.0.1:8089";
 private final boolean local;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector(){public List<Proxy> select(URI uri){return List.of(Proxy.NO_PROXY);}public void connectFailed(URI uri,java.net.SocketAddress address,java.io.IOException error){}}).build();
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public LocalWorkspaceProbe(Environment env){local=env.acceptsProfiles(Profiles.of("local-onboarding"))&&!env.acceptsProfiles(Profiles.of("production"))&&"127.0.0.1".equals(env.getProperty("server.address"))&&"aria-registration".equals(env.getProperty("aria.organization.key"))&&"jdbc:postgresql://127.0.0.1:55434/aria_registration_local".equals(env.getProperty("spring.datasource.url"));}
 public String validate(String address){if(local&&TARGET.equals(address))return TARGET;throw new IllegalArgumentException("Доступна только отдельная локальная проверка кабинета. Внешний адрес ещё не подключён");}
 public void requireLocal(){validate(TARGET);}
 public record Result(Instant claimedAt){}
 public Result check(String address,String secret,String request,String binding,String ownerHash,Instant deadline){
  validate(address);String nonce=UUID.randomUUID().toString().replace("-","");
  try{
   // Payload is limited to request ID + random nonce. Only their HMAC is sent, not secret or contacts.
   var message=HttpRequest.newBuilder(URI.create(TARGET+"/api/company-activation/attest")).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("X-ARIA-Provision-Proof",WorkspaceProof.requestProof(secret,request,nonce)).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("requestId",request,"nonce",nonce)))).build();
   var response=client.send(message,info->new LimitedBody());if(response.statusCode()!=200)throw new IllegalStateException();Object parsed=json.readValue(response.body(),Object.class);if(!(parsed instanceof Map<?,?> body))throw new IllegalStateException();
   if(!request.equals(body.get("requestId"))||!binding.equals(body.get("binding"))||!ownerHash.equals(body.get("ownerHash"))||!nonce.equals(body.get("nonce"))||!deadline.equals(Instant.parse(Objects.toString(body.get("expiresAt"),""))))throw new IllegalStateException();
   Instant claimed=body.get("claimedAt")==null?null:Instant.parse(body.get("claimedAt").toString());if(claimed!=null&&(claimed.isAfter(deadline)||claimed.isAfter(Instant.now().plusSeconds(60))))throw new IllegalStateException();
   if(!WorkspaceProof.equal(Objects.toString(body.get("signature"),""),WorkspaceProof.sign(secret,request,binding,ownerHash,nonce,deadline,claimed)))throw new IllegalStateException();return new Result(claimed);
  }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new IllegalStateException("Работающий кабинет и учётная запись владельца не подтверждены. Проверьте настройки и повторите проверку");}
 }
 private static class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();private java.util.concurrent.Flow.Subscription subscription;private int size;
  public java.util.concurrent.CompletionStage<byte[]> getBody(){return delegate.getBody();}public void onSubscribe(java.util.concurrent.Flow.Subscription value){subscription=value;delegate.onSubscribe(value);}public void onNext(List<java.nio.ByteBuffer> chunks){for(var chunk:chunks)size+=chunk.remaining();if(size>8192){subscription.cancel();delegate.onError(new java.io.IOException("Readiness response too large"));}else delegate.onNext(chunks);}public void onError(Throwable error){delegate.onError(error);}public void onComplete(){delegate.onComplete();}
 }
}
