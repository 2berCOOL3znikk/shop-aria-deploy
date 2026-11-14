package com.aria.security;
import com.aria.service.*;
import com.aria.entity.WorkspacePoolSlot;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.env.Environment;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Routing is not authentication: every forwarded bearer is verified again by the isolated destination. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE+100)
public class WorkspaceGatewayFilter extends OncePerRequestFilter {
 private final CloudRegistrationService registry;
 private final String nativeBinding;
 @org.springframework.beans.factory.annotation.Autowired private com.aria.repository.AppUserRepository nativeUsers;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector(){public List<Proxy> select(URI u){return List.of(Proxy.NO_PROXY);}public void connectFailed(URI u,java.net.SocketAddress a,IOException e){}}).build();
 private final Semaphore concurrent=new Semaphore(4,true);
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 private static final int MAX=6*1024*1024;
 public WorkspaceGatewayFilter(CloudRegistrationService registry,Environment env){this.registry=registry;nativeBinding=env.getProperty("aria.organization.key","demo");}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String path=request.getServletPath();if(!registry.enabled()||!path.startsWith("/api/")){chain.doFilter(request,response);return;}
  if(path.contains("..")||path.contains("//")||path.contains("\\")||request.getRequestURI().contains("%")||request.getQueryString()!=null&&request.getQueryString().length()>4096){error(response,400,"Проверьте адрес запроса");return;}
  // Platform administration, public admission and bootstrap are never forwarded using a tenant hint.
  if(path.startsWith("/api/internal/")){error(response,404,"Ресурс недоступен");return;}
  if(request.getMethod().equals("POST")&&(path.startsWith("/api/saas/")||path.equals("/api/admin/workspace-pool/slots"))){byte[] admission=request.getInputStream().readNBytes(8193);if(admission.length>8192){error(response,413,"Запрос слишком большой");return;}request=new BufferedRequest(request,admission);}
  if(path.startsWith("/api/admin/workspace-pool")||path.startsWith("/api/admin/company-signup")||path.startsWith("/api/saas/")||path.startsWith("/api/company-signup/")||path.startsWith("/api/company-activation/")){chain.doFilter(request,response);return;}
  try{
   String authorization=request.getHeader("Authorization"),hint=request.getHeader("X-ARIA-Workspace");Optional<WorkspacePoolSlot> destination=Optional.empty();
   if(authorization!=null&&authorization.startsWith("Bearer ")){
    String binding=claimBinding(authorization.substring(7));if(hint!=null&&!hint.equals(binding)){error(response,401,"Контекст компании изменился. Войдите заново");return;}
    if(nativeBinding.equals(binding)){chain.doFilter(request,response);return;}
    destination=registry.slotByBinding(binding);if(destination.isEmpty()){error(response,401,"Сеанс компании недоступен. Войдите заново");return;}
   }else if(path.equals("/api/auth/login")&&request.getMethod().equals("POST")){
    byte[] body=request.getInputStream().readNBytes(4097);if(body.length>4096){error(response,413,"Запрос слишком большой");return;}request=new BufferedRequest(request,body);
    if("platform".equals(request.getHeader("X-ARIA-Login-Context"))||nativeBinding.equals(hint)){chain.doFilter(request,response);return;}
    if(hint!=null){destination=registry.slotByBinding(hint);if(destination.isEmpty()){error(response,401,"Кабинет недоступен");return;}}
    else {Map<?,?> input;try{input=json.readValue(body,Map.class);}catch(Exception malformed){error(response,400,"Проверьте данные входа");return;}Object email=input.get("email");if(email instanceof String value){if(!"company".equals(request.getHeader("X-ARIA-Login-Context"))&&nativeUsers!=null&&nativeUsers.existsByEmailIgnoreCase(value.trim())){chain.doFilter(request,response);return;}destination=registry.loginSlot(value);}}
   }else if(Set.of("/api/employee-invitations/inspect","/api/employee-invitations/accept").contains(path)&&request.getMethod().equals("POST")&&hint!=null&&!hint.equals(nativeBinding)){
    destination=registry.slotByBinding(hint);if(destination.isEmpty()){error(response,404,"Приглашение недоступно");return;}
   }
   if(destination.isEmpty()){chain.doFilter(request,response);return;}
   proxy(request,response,destination.get());
  }catch(ResponseStatusException e){error(response,e.getStatusCode().value(),e.getReason()==null?"Кабинет недоступен":e.getReason());}
  catch(IllegalArgumentException e){error(response,401,"Проверьте данные входа");}
  catch(RuntimeException e){error(response,503,"Кабинет временно недоступен. Повторите запрос позже");}
 }
 private String claimBinding(String token){try{if(token.length()>8192)throw new Exception();String[] p=token.split("\\.");if(p.length!=3)throw new Exception();byte[] raw=Base64.getUrlDecoder().decode(p[1]);if(raw.length>4096)throw new Exception();Map<?,?> claims=json.readValue(raw,Map.class);String key=Objects.toString(claims.get("organization"),"");if(!key.matches("[a-z0-9][a-z0-9-]{0,79}")||!("aria:"+key).equals(claims.get("iss")))throw new Exception();return key;}catch(Exception e){throw new IllegalArgumentException();}}
 private void proxy(HttpServletRequest request,HttpServletResponse response,WorkspacePoolSlot slot)throws IOException{
  registry.validateOrigin(slot.origin);try{if(!concurrent.tryAcquire(10,java.util.concurrent.TimeUnit.SECONDS)){error(response,429,"Кабинет занят. Повторите через несколько секунд");return;}}catch(InterruptedException e){Thread.currentThread().interrupt();error(response,503,"Повторите запрос позже");return;}
  try{
   byte[] body=request.getInputStream().readNBytes(MAX+1);if(body.length>MAX){error(response,413,"Файл слишком большой");return;}
   String target=slot.origin+request.getServletPath()+(request.getQueryString()==null?"":"?"+request.getQueryString());var builder=HttpRequest.newBuilder(URI.create(target)).timeout(Duration.ofSeconds(50));
   for(String header:List.of("Authorization","Content-Type","Accept","Accept-Language","If-Match","Idempotency-Key")){String value=request.getHeader(header);if(value!=null)builder.header(header,value);}
   builder.method(request.getMethod(),body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));
   var result=client.send(builder.build(),info->new BoundedHttpBody(MAX));response.setStatus(result.statusCode());response.setHeader("Cache-Control","no-store");
   for(String header:List.of("Content-Type","Content-Disposition","Retry-After"))result.headers().firstValue(header).ifPresent(v->response.setHeader(header,v));
   // Neither provider redirects nor cookies may move the user to another host or select a workspace.
   if(result.statusCode()>=300&&result.statusCode()<400){error(response,502,"Кабинет не подтвердил ответ");return;}response.getOutputStream().write(result.body());
  }catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();error(response,503,"Кабинет запускается или временно недоступен. Повторите запрос");}finally{concurrent.release();}
 }
 private void error(HttpServletResponse response,int status,String message)throws IOException{if(response.isCommitted())return;response.setStatus(status);response.setHeader("Cache-Control","no-store");response.setContentType("application/json;charset=UTF-8");response.getWriter().write(json.writeValueAsString(Map.of("message",message)));}
 private static final class BufferedRequest extends HttpServletRequestWrapper {
  private final byte[] body;BufferedRequest(HttpServletRequest r,byte[] body){super(r);this.body=body;}
  @Override public ServletInputStream getInputStream(){return new ServletInputStream(){final ByteArrayInputStream input=new ByteArrayInputStream(body);public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};}
  @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
 }
}
