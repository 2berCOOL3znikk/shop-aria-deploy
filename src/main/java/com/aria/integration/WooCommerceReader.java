package com.aria.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.time.Duration;
@Component
public class WooCommerceReader {
 private final Set<String> hosts;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
 public WooCommerceReader(@Value("${aria.integrations.allowed-hosts:}") String hosts){this.hosts=new HashSet<>(Arrays.asList(hosts.toLowerCase(Locale.ROOT).split(",")));}
 public URI validate(String address){
  try{var u=URI.create(address);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!hosts.contains(u.getHost().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException();
   for(var ip:InetAddress.getAllByName(u.getHost()))if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||ip.isSiteLocalAddress()||ip.isMulticastAddress()||(ip.getAddress().length==16&&(ip.getAddress()[0]&0xfe)==0xfc))throw new IllegalArgumentException();
   return URI.create(address.replaceAll("/+$","")+"/wp-json/wc/v3/");
  }catch(Exception e){throw new IllegalArgumentException("Адрес не разрешён. Для подключения нужен HTTPS и одобрение домена на сервере");}
 }
 public List<Map<String,Object>> get(String address,String resource,String key,String secret){
  return page(address,resource,key,secret).items();
 }
 public record Page(List<Map<String,Object>> items, boolean more){}
 public Page page(String address,String resource,String key,String secret){
  if(!resource.matches("(?:products(?:/[0-9]+/variations)?|orders)\\?per_page=100&page=[1-9][0-9]*"))throw new IllegalArgumentException("Неверный раздел магазина");
  var decoded=read(address,resource,key,secret);Object parsed=decoded.body();
  if(!(parsed instanceof List<?> list)||list.stream().anyMatch(x->!(x instanceof Map)))throw new IllegalStateException("Магазин вернул неподдерживаемый ответ");
  try{int current=Integer.parseInt(resource.substring(resource.lastIndexOf('=')+1));var total=decoded.headers().firstValue("X-WP-TotalPages");
   if(total.isPresent()&&(!total.get().matches("[0-9]{1,9}")||(!list.isEmpty()&&current>Integer.parseInt(total.get()))))throw new IllegalStateException("Магазин вернул некорректную пагинацию");
   return new Page((List<Map<String,Object>>)parsed,total.isPresent()?current<Integer.parseInt(total.get()):list.size()==100);
  }catch(NumberFormatException e){throw new IllegalStateException("Магазин вернул некорректную пагинацию");}
 }
 public String currency(String address,String key,String secret){
  try{Object body=read(address,"data/currencies/current",key,secret).body();if(!(body instanceof Map<?,?> map)||!(map.get("code") instanceof String code)||!code.matches("[A-Z]{3}"))throw new IllegalStateException("Магазин вернул неподдерживаемый код валюты");return code;}
  catch(IllegalStateException e){throw new IllegalStateException("Валюта каталога не подтверждена. "+e.getMessage());}
 }
 private record Decoded(Object body,HttpHeaders headers){}
 private Decoded read(String address,String resource,String key,String secret){
  var base=validate(address);
  try{var auth=Base64.getEncoder().encodeToString((key+":"+secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));var request=HttpRequest.newBuilder(base.resolve(resource)).timeout(Duration.ofSeconds(15)).header("Authorization","Basic "+auth).header("Accept","application/json").GET().build();
   var response=http.send(request,info->new LimitedBody());
   if(response.statusCode()!=200)throw new IllegalStateException(response.statusCode()==401||response.statusCode()==403?"Магазин отклонил доступ. Проверьте ключи и право чтения":"Магазин временно недоступен. Повторите проверку позже");
   return new Decoded(json.readValue(response.body(),Object.class),response.headers());
  }catch(IllegalStateException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new IllegalStateException("Не удалось прочитать магазин. Проверьте соединение и повторите попытку");}
 }
 private static class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  private final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();
  private java.util.concurrent.Flow.Subscription subscription;private long size;
  public java.util.concurrent.CompletionStage<byte[]> getBody(){return delegate.getBody();}
  public void onSubscribe(java.util.concurrent.Flow.Subscription s){subscription=s;delegate.onSubscribe(s);}
  public void onNext(List<java.nio.ByteBuffer> chunks){for(var b:chunks)size+=b.remaining();if(size>2_000_000){subscription.cancel();delegate.onError(new java.io.IOException("Response limit"));}else delegate.onNext(chunks);}
  public void onError(Throwable error){delegate.onError(error);}
  public void onComplete(){delegate.onComplete();}
 }
}
