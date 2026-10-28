package com.aria;

import com.aria.entity.*;
import com.aria.enums.Role;
import com.aria.repository.*;
import com.aria.service.InventoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.seed-demo-catalog=false","aria.seed-demo-users=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:controltest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class ControlIntegrationTest {
    @Autowired Environment env;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired InventoryLotRepository lots;
    @Autowired InventoryService inventory;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired com.aria.service.AriaService analysis;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    private final HttpClient client=HttpClient.newHttpClient();
    private String admin,user,org;
    private final String base="/api/admin/control";
    record Reply(int status,Map<String,Object> body,String raw) {long id(){return ((Number)body.get("id")).longValue();}}
    @BeforeEach void setup()throws Exception{
        String suffix=UUID.randomUUID().toString();
        admin=account("a"+suffix,Role.ADMIN);user=account("u"+suffix,Role.USER);org=account("o"+suffix,Role.ORG);
    }
    private String account(String name,Role role)throws Exception {String email=name+"@test.local";users.save(new AppUser(email,passwords.encode("testPassword123"),name,role));return (String)call("POST","/api/auth/login",null,Map.of("email",email,"password","testPassword123")).body.get("token");}
    private Reply call(String method,String path,String token,Object data)throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path));
        if(token!=null)builder.header("Authorization","Bearer "+token);
        builder.header("Content-Type","application/json").method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        Map<String,Object> body=new HashMap<>();if(response.body().startsWith("{"))body=json.readValue(response.body(),Map.class);
        return new Reply(response.statusCode(),body,response.body());
    }
    private Map<String,Object> productBody(String sku){return Map.of("sku",sku,"name","Тестовый товар","category","Продукты","unitPrice",1000,"unit","шт","lowStockThreshold",2);}
    private long product()throws Exception {var result=call("POST",base+"/products",admin,productBody("TEST-"+UUID.randomUUID()));assertThat(result.status).as(result.raw).isEqualTo(200);return result.id();}
    private Map<String,Object> lotBody(long p,String number,int quantity,int days){return Map.of("productId",p,"lotNumber",number,"quantity",quantity,"unitCost",800,"receivedAt",LocalDate.now().toString(),"expiryDate",LocalDate.now().plusDays(days).toString());}
    private Reply lot(long p,String number,int qty,int days)throws Exception {var r=call("POST",base+"/lots",admin,lotBody(p,number,qty,days));assertThat(r.status).as(r.raw).isEqualTo(200);return r;}
    private long order(long p,int qty)throws Exception {assertThat(call("POST","/api/cart/add?productId="+p+"&qty="+qty,user,null).status).isEqualTo(200);var r=call("POST","/api/orders/checkout",user,Map.of("address","Тестовый адрес"));assertThat(r.status).as(r.raw).isEqualTo(200);return r.id();}
    @Test void registrationRequiresSingleUseCaptchaAndDoesNotAllowRoleEscalation()throws Exception {
        String email="new-"+UUID.randomUUID()+"@test.local";
        Map<String,Object> input=new HashMap<>(Map.of("email",email,"password","securePassword123","fullName","Покупатель","role","ADMIN"));
        assertThat(call("POST","/api/auth/register",null,input).status).isEqualTo(400);
        var challenge=call("GET","/api/auth/captcha",null,null);
        String question=(String)challenge.body.get("question");var numbers=java.util.regex.Pattern.compile("\\d+").matcher(question);numbers.find();int a=Integer.parseInt(numbers.group());numbers.find();int b=Integer.parseInt(numbers.group());
        input.put("captchaId",challenge.body.get("id"));input.put("captchaAnswer",Integer.toString(a+b));
        var registered=call("POST","/api/auth/register",null,input);assertThat(registered.status).as(registered.raw).isEqualTo(200);
        assertThat(registered.body.get("role")).isEqualTo("USER");
        input.put("email","another-"+email);assertThat(call("POST","/api/auth/register",null,input).status).isEqualTo(400);
    }
    @Test void passwordChangeRevokesAllEarlierTokensAndKeepsNewLoginUsable()throws Exception {
        String email="security-"+UUID.randomUUID()+"@test.local";
        users.save(new AppUser(email,passwords.encode("oldPassword123"),"Security",Role.USER));
        String token=(String)call("POST","/api/auth/login",null,Map.of("email",email,"password","oldPassword123")).body.get("token");
        String other=(String)call("POST","/api/auth/login",null,Map.of("email",email,"password","oldPassword123")).body.get("token");
        assertThat(call("POST","/api/profile/security/password",token,Map.of("currentPassword","wrong","newPassword","newPassword456")).status).isEqualTo(400);
        assertThat(call("GET","/api/orders",token,null).status).isEqualTo(200);
        assertThat(call("POST","/api/profile/security/password",token,Map.of("currentPassword","oldPassword123","newPassword","newPassword456")).status).isEqualTo(200);
        assertThat(call("GET","/api/orders",token,null).status).isEqualTo(401);assertThat(call("GET","/api/orders",other,null).status).isEqualTo(401);
        assertThat(call("POST","/api/auth/login",null,Map.of("email",email,"password","oldPassword123")).status).isEqualTo(401);
        assertThat(call("POST","/api/auth/login",null,Map.of("email",email,"password","newPassword456")).status).isEqualTo(200);
    }
    @Test void repeatedBadPasswordsAreLimitedWithoutExposingInternalErrors()throws Exception {
        String email="missing-"+UUID.randomUUID()+"@test.local";
        for(int i=0;i<6;i++)assertThat(call("POST","/api/auth/login",null,Map.of("email",email,"password","badPassword123")).status).isEqualTo(401);
        assertThat(call("POST","/api/auth/login",null,Map.of("email",email,"password","badPassword123")).status).isEqualTo(429);
    }
    @Test void shippedSalesAndOrderNamesReflectActualConfirmedOperations()throws Exception {
        long p=product();lot(p,"SALES",8,60);long id=order(p,2);
        var before=orders.shippedUnitsSince(java.time.LocalDateTime.now().minusDays(30));
        assertThat(before.stream().filter(row->((Number)row[0]).longValue()==p)).isEmpty();
        assertThat(call("PUT",base+"/orders/"+id+"/status",admin,Map.of("status","SHIPPED","reason","Передан в доставку")).status).isEqualTo(200);
        var shipped=orders.findById(id).orElseThrow();assertThat(shipped.getShippedAt()).isNotNull();
        assertThat(orders.shippedUnitsSince(java.time.LocalDateTime.now().minusDays(30)).stream().filter(row->((Number)row[0]).longValue()==p).map(row->((Number)row[1]).intValue()).toList()).containsExactly(2);
        var changed=new HashMap<>(productBody("SNAP-"+p));changed.put("name","Другое название");assertThat(call("PUT",base+"/products/"+p,admin,changed).status).isEqualTo(200);
        assertThat(call("GET","/api/orders",user,null).raw).contains("Тестовый товар").doesNotContain("Другое название");
        shipped.setShippedAt(java.time.LocalDateTime.now().minusDays(31));orders.save(shipped);
        assertThat(orders.shippedUnitsSince(java.time.LocalDateTime.now().minusDays(30)).stream().filter(row->((Number)row[0]).longValue()==p)).isEmpty();
    }
    @Test void adminBoundaryValidationAndStaticStyles()throws Exception {
        assertThat(call("GET",base+"/products",null,null).status).isEqualTo(401);
        assertThat(call("POST",base+"/products",user,productBody("DENIED")).status).isEqualTo(403);
        assertThat(call("POST",base+"/products",admin,Map.of("sku","bad")).status).isEqualTo(400);
        assertThat(call("GET","/control.css",null,null).status).isEqualTo(200);
    }
    @Test void threeRelatedResourcesSupportCreateReadUpdateAndSafeDelete()throws Exception {
        var supplier=call("POST",base+"/suppliers",admin,Map.of("name","Поставщик","active",true));assertThat(supplier.status).isEqualTo(200);
        long p=product();var input=new HashMap<>(lotBody(p,"FIRST",4,10));input.put("supplierId",supplier.id());var l=call("POST",base+"/lots",admin,input);assertThat(l.status).as(l.raw).isEqualTo(200);
        assertThat(call("GET",base+"/products/"+p,admin,null).status).isEqualTo(200);
        assertThat(call("PUT",base+"/products/"+p,admin,productBody("UPDATED-"+p)).status).isEqualTo(200);
        assertThat(call("PUT",base+"/suppliers/"+supplier.id(),admin,Map.of("name","Обновлён","active",true)).status).isEqualTo(200);
        input.put("quantity",5);assertThat(call("PUT",base+"/lots/"+l.id(),admin,input).status).isEqualTo(200);
        assertThat(call("DELETE",base+"/suppliers/"+supplier.id(),admin,null).status).isEqualTo(409);
        assertThat(call("DELETE",base+"/products/"+p,admin,null).status).isEqualTo(409);
        assertThat(call("DELETE",base+"/lots/"+l.id(),admin,Map.of("reason","Повреждение")).status).isEqualTo(200);
        assertThat(call("DELETE",base+"/products/"+p,admin,null).status).isEqualTo(200);
        assertThat(call("GET","/api/products/"+p,null,null).status).isEqualTo(404);
    }
    @Test void fefoSaleAndCancellationRestoreOriginalLotsExactlyOnce()throws Exception {
        long p=product();long later=lot(p,"LATER",4,20).id(),first=lot(p,"FIRST",3,3).id();long order=order(p,4);
        assertThat(lots.findById(first).orElseThrow().getQuantity()).isZero();assertThat(lots.findById(later).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(call("PUT",base+"/lots/"+first,admin,lotBody(p,"FIRST",3,3)).status).isEqualTo(409);
        assertThat(call("PUT",base+"/orders/"+order+"/status",admin,Map.of("status","CANCELLED","reason","Отмена до отправки")).status).isEqualTo(200);
        assertThat(lots.findById(first).orElseThrow().getQuantity()).isEqualTo(3);assertThat(lots.findById(later).orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(call("PUT",base+"/orders/"+order+"/status",admin,Map.of("status","CANCELLED","reason","Повтор")).status).isEqualTo(409);
    }
    @Test void expiredLotCannotBeReceivedOrConsumedEvenWithFreshLot()throws Exception {
        long p=product();assertThat(call("POST",base+"/lots",admin,lotBody(p,"EXPIRED",5,-1)).status).isEqualTo(400);
        long id=lot(p,"OLD",5,2).id();InventoryLot expired=lots.findById(id).orElseThrow();expired.setExpiryDate(LocalDate.now().minusDays(1));lots.save(expired);
        lot(p,"FRESH",2,10);assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(2);
        assertThat(call("POST","/api/cart/add?productId="+p+"&qty=3",user,null).status).isEqualTo(400);
        order(p,2);assertThat(lots.findById(id).orElseThrow().getQuantity()).isEqualTo(5);
    }
    @Test void discountIsBoundedAndCheckoutRejectsStalePrice()throws Exception {
        long p=product();lot(p,"BASE",5,10);
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",21,"reason","Проверка")).status).isEqualTo(409);
        assertThat(call("POST","/api/cart/add?productId="+p,user,null).status).isEqualTo(200);
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",20,"reason","Срок")).status).isEqualTo(200);
        assertThat(call("POST","/api/orders/checkout",user,Map.of("address","Тестовый адрес")).status).isEqualTo(409);
        assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(5);
    }
    @Test void duplicateConcurrentCheckoutOnlyCreatesOneOrder()throws Exception {
        long p=product();lot(p,"STOCK",5,10);assertThat(call("POST","/api/cart/add?productId="+p+"&qty=2",user,null).status).isEqualTo(200);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var jobs=executor.invokeAll(List.<Callable<Integer>>of(()->call("POST","/api/orders/checkout",user,Map.of("address","Адрес")).status,()->call("POST","/api/orders/checkout",user,Map.of("address","Адрес")).status));
            assertThat(jobs.stream().map(j->{try{return j.get();}catch(Exception e){throw new RuntimeException(e);}}).toList()).containsExactlyInAnyOrder(200,409);
        }
        assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(3);
    }
    @Test void socialReservationRequiresVerificationAndTransferNeedsSeparateConfirmation()throws Exception {
        long p=product();lot(p,"SOCIAL",5,7);var offer=call("POST","/api/social/offers",admin,Map.of("productId",p,"quantity",2,"validUntil",LocalDate.now().plusDays(3).toString()));assertThat(offer.status).as(offer.raw).isEqualTo(200);
        assertThat(call("POST","/api/social/offers/"+offer.id()+"/claim",org,null).status).isEqualTo(400);
        var application=call("POST","/api/organizations/verification",org,Map.of("organizationName","Тестовый фонд","registrationNumber","000000000001"));assertThat(application.status).isEqualTo(200);
        assertThat(call("POST","/api/admin/organizations/verification-requests/"+application.id()+"/approve",admin,null).status).isEqualTo(200);
        assertThat(call("POST","/api/social/offers/"+offer.id()+"/claim",org,null).status).isEqualTo(200);
        assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(5);
        assertThat(call("POST",base+"/social/"+offer.id()+"/confirm",admin,Map.of("reason","Выдано по акту")).status).isEqualTo(200);
        assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(3);
        assertThat(call("POST",base+"/social/"+offer.id()+"/confirm",admin,Map.of("reason","Повтор")).status).isEqualTo(409);
    }
    @Test void imageUploadPersistsAndCardRemainsEditable()throws Exception {
        long p=product();var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        String boundary="AriaTestBoundary";var body=new java.io.ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));body.write(bytes.toByteArray());body.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+"/api/admin/products/"+p+"/photo")).header("Authorization","Bearer "+admin).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        var response=client.send(request,HttpResponse.BodyHandlers.ofString());assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(call("GET","/api/products/"+p+"/photo",null,null).status).isEqualTo(200);
        var update=new HashMap<>(productBody("PHOTO-"+p));update.put("imageUrl","/api/products/"+p+"/photo");assertThat(call("PUT",base+"/products/"+p,admin,update).status).isEqualTo(200);
    }
    @Test void favoritesDoNotExposePurchaseCostsOrAuditData()throws Exception {
        long p=product();lot(p,"FAVORITE",2,10);
        assertThat(call("POST","/api/favorites/"+p,user,null).status).isEqualTo(200);
        var response=call("GET","/api/favorites",user,null);assertThat(response.status).isEqualTo(200);
        assertThat(response.raw).contains("displayPrice").doesNotContain("unitCost","marketAvgPrice","ariaReason");
    }
    @Test void categoryPolicyCapsMarginAndCanBeResetWithoutChangingPrice()throws Exception {
        String category="policy-"+UUID.randomUUID();var body=new HashMap<>(productBody("POLICY-"+UUID.randomUUID()));body.put("category",category);
        var created=call("POST",base+"/products",admin,body);assertThat(created.status).as(created.raw).isEqualTo(200);long p=created.id();lot(p,"RULES",5,5);
        var policy=new HashMap<String,Object>(Map.of("category",category,"warningDays",20,"criticalDays",2,"clearanceDays",0,"warningPercent",12,"criticalPercent",25,"clearancePercent",45,"slowStockDays",90,"slowStockPercent",10,"minimumMarginPercent",10));
        assertThat(call("PUT",base+"/policies",user,policy).status).isEqualTo(403);
        assertThat(call("PUT",base+"/policies",admin,policy).status).isEqualTo(200);
        var preview=call("GET",base+"/pricing/"+p+"?percent=12",admin,null);assertThat(preview.status).isEqualTo(200);
        assertThat(((Map<?,?>)preview.body.get("current")).get("allowed")).isEqualTo(false);
        assertThat(((Map<?,?>)preview.body.get("recommendation")).get("suggestedPercent")).isEqualTo(11);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",11,"reason","Маржа не ниже 10%")).status).isEqualTo(200);
        policy.put("minimumMarginPercent",20);assertThat(call("PUT",base+"/policies",admin,policy).status).isEqualTo(409);
        assertThat(call("DELETE",base+"/policies?category="+category,admin,null).status).isEqualTo(200);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isEqualTo(11);
    }
    @Test void invalidPolicyThresholdsAreRejected()throws Exception {
        var policy=new HashMap<String,Object>(Map.of("category","invalid-"+UUID.randomUUID(),"warningDays",1,"criticalDays",7,"clearanceDays",0,"warningPercent",15,"criticalPercent",30,"clearancePercent",50,"slowStockDays",180,"slowStockPercent",10,"minimumMarginPercent",0));
        assertThat(call("PUT",base+"/policies",admin,policy).status).isEqualTo(400);
        policy.put("warningDays",30);policy.put("warningPercent",70);assertThat(call("PUT",base+"/policies",admin,policy).status).isEqualTo(400);
    }
    @Test void soldOutDiscountCanBeClearedWithoutMakingExpiredStockSellable()throws Exception {
        long p=product();lot(p,"EMPTY",1,5);assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",20,"reason","Срок")).status).isEqualTo(200);
        order(p,1);assertThat(inventory.available(products.findById(p).orElseThrow())).isZero();
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",0,"reason","Скидка закончена")).status).isEqualTo(200);
        assertThat(call("GET","/api/products/"+p,null,null).status).isEqualTo(404);
    }
}
