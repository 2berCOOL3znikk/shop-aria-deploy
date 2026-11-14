package com.aria.service;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
@Service
public class RemoteCloudWorkspaceTransport implements CloudWorkspaceTransport {
 private final Set<String> allowed;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector(){public List<Proxy> select(URI u){return List.of(Proxy.NO_PROXY);}public void connectFailed(URI u,java.net.SocketAddress a,java.io.IOException e){}}).build();
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public RemoteCloudWorkspaceTransport(Environment env){allowed=Set.copyOf(Arrays.stream(env.getProperty("aria.saas.allowed-origins","").split(",")).map(String::trim).filter(s->!s.isBlank()).toList());for(String origin:allowed)validateOrigin(origin);}
 @Override public void validateOrigin(String origin){if(!allowed.contains(origin)||!origin.matches("https://aria-company-[ab]-2ber-stg\\.onrender\\.com"))throw new IllegalArgumentException("Адрес кабинета не разрешён платформой");}
 @Override public Map<String,Object> exchange(String origin,String secret,Map<String,Object> request){
  validateOrigin(origin);
  try{String raw=json.writeValueAsString(request);var call=HttpRequest.newBuilder(URI.create(origin+"/api/internal/workspace/bootstrap")).timeout(Duration.ofSeconds(25)).header("Content-Type","application/json").header("X-ARIA-Workspace-Proof",CloudWorkspaceProof.sign(secret,"request:"+raw)).header("X-ARIA-Workspace-Access",CloudWorkspaceProof.sign(secret,"access:"+java.time.LocalDate.now(java.time.ZoneOffset.UTC))).POST(HttpRequest.BodyPublishers.ofString(raw)).build();
   var response=client.send(call,info->new BoundedHttpBody(16384));byte[] bytes=response.body();if(response.statusCode()!=200)throw new IllegalStateException();
   Map<?,?> envelope=json.readValue(bytes,Map.class);String data=Objects.toString(envelope.get("data"),"");if(!CloudWorkspaceProof.equal(Objects.toString(envelope.get("proof"),""),CloudWorkspaceProof.sign(secret,"response:"+data)))throw new IllegalStateException();
   Map<String,Object> result=json.readValue(Base64.getUrlDecoder().decode(data),Map.class);if(!Objects.equals(result.get("nonce"),request.get("nonce"))||!Objects.equals(result.get("binding"),request.get("binding")))throw new IllegalStateException();return result;
  }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new IllegalStateException("Кабинет запускается или не подтвердил готовность");}
 }
}
