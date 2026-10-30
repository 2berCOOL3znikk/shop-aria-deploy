package com.aria.integration;
import org.junit.jupiter.api.Test;
import java.net.*;import java.net.http.*;import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WooCommerceReaderTest {
 @Test void readsOfficialProductShapeUsingGetAndNeverPlacesCredentialsInUrl()throws Exception {
  var http=mock(HttpClient.class);var reader=spy(new WooCommerceReader(""));org.springframework.test.util.ReflectionTestUtils.setField(reader,"http",http);
  doReturn(URI.create("https://shop.test/wp-json/wc/v3/")).when(reader).validate("https://shop.test");
  HttpResponse<byte[]> response=mock(HttpResponse.class);when(response.headers()).thenReturn(HttpHeaders.of(java.util.Map.of(),(a,b)->true));when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("[{\"id\":12,\"type\":\"simple\",\"name\":\"Tea\",\"price\":\"1250.00\",\"manage_stock\":true,\"stock_quantity\":4}]".getBytes(StandardCharsets.UTF_8));
  when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any())).thenReturn(response);
  var result=reader.get("https://shop.test","products?per_page=100&page=1","test-key","test-secret");assertEquals(1,result.size());assertEquals("Tea",result.getFirst().get("name"));
  var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(capture.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());assertEquals("GET",capture.getValue().method());assertFalse(capture.getValue().uri().toString().contains("test-key"));assertTrue(capture.getValue().headers().firstValue("Authorization").orElseThrow().startsWith("Basic "));
  when(response.statusCode()).thenReturn(401);assertThrows(IllegalStateException.class,()->reader.get("https://shop.test","products?per_page=100&page=1","x","y"));
  when(response.statusCode()).thenReturn(302);assertThrows(IllegalStateException.class,()->reader.get("https://shop.test","products?per_page=100&page=1","x","y"));
  when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));assertThrows(IllegalStateException.class,()->reader.get("https://shop.test","products?per_page=100&page=1","x","y"));
  when(response.body()).thenReturn("[]".getBytes(StandardCharsets.UTF_8));when(response.headers()).thenReturn(HttpHeaders.of(java.util.Map.of("X-WP-TotalPages",java.util.List.of("bad-header")),(a,b)->true));assertThrows(IllegalStateException.class,()->reader.get("https://shop.test","products?per_page=100&page=1","x","y"));
 }
 @Test void readsOnlyOfficialCurrentCurrencyCode()throws Exception{
  var http=mock(HttpClient.class);var reader=spy(new WooCommerceReader(""));org.springframework.test.util.ReflectionTestUtils.setField(reader,"http",http);doReturn(URI.create("https://shop.test/wp-json/wc/v3/")).when(reader).validate("https://shop.test");
  HttpResponse<byte[]> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);when(response.headers()).thenReturn(HttpHeaders.of(java.util.Map.of(),(a,b)->true));when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any())).thenReturn(response);
  when(response.body()).thenReturn("{\"code\":\"KZT\",\"symbol\":\"<script>bad</script>\"}".getBytes(StandardCharsets.UTF_8));assertEquals("KZT",reader.currency("https://shop.test","k","s"));var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(capture.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());assertEquals("/wp-json/wc/v3/data/currencies/current",capture.getValue().uri().getPath());assertEquals("GET",capture.getValue().method());
  for(String bad:new String[]{"[]","{}","{\"code\":\"kzt\"}","{\"code\":\"KZT<script>\"}"}){when(response.body()).thenReturn(bad.getBytes(StandardCharsets.UTF_8));assertThrows(IllegalStateException.class,()->reader.currency("https://shop.test","k","s"));}when(response.statusCode()).thenReturn(403);assertThrows(IllegalStateException.class,()->reader.currency("https://shop.test","k","s"));
 }
}
