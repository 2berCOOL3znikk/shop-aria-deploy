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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"aria.intelligence.external-enabled=false","aria.seed-demo-catalog=false","aria.seed-demo-users=false","spring.datasource.url=${ARIA_TEST_DATABASE_URL:jdbc:h2:mem:controltest;DB_CLOSE_DELAY=-1}","spring.datasource.username=${ARIA_TEST_DATABASE_USER:sa}","spring.datasource.password=${ARIA_TEST_DATABASE_PASSWORD:}","spring.datasource.driver-class-name=${ARIA_TEST_DATABASE_DRIVER:org.h2.Driver}","spring.flyway.enabled=${ARIA_TEST_FLYWAY:false}","spring.flyway.locations=classpath:db/migration/postgresql","spring.flyway.baseline-on-migrate=true","spring.flyway.baseline-version=0","spring.jpa.hibernate.ddl-auto=${ARIA_TEST_SCHEMA_MODE:update}","logging.level.root=ERROR"})
class ControlIntegrationTest {
    @Autowired Environment env;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired InventoryLotRepository lots;
    @Autowired InventoryService inventory;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired com.aria.service.AriaService analysis;
    @Autowired AuditEventRepository auditEvents;
    @Autowired SmartPricingSettingsRepository smartSettings;
    @Autowired PriceChangeRepository priceChanges;
    @Autowired com.aria.service.SmartPricingService smartPricing;
    @Autowired OrganizationVerificationRepository organizationVerifications;
    private final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    private final HttpClient client=HttpClient.newHttpClient();
    private String admin,user,org,orgEmail;
    private final String base="/api/admin/control";
    record Reply(int status,Map<String,Object> body,String raw) {long id(){return ((Number)body.get("id")).longValue();}}
    @BeforeEach void setup()throws Exception{
        smartSettings.deleteAll(); // isolated test DB only: every scenario starts in RECOMMEND
        String suffix=UUID.randomUUID().toString();
        admin=account("a"+suffix,Role.ADMIN);user=account("u"+suffix,Role.USER);org=account("o"+suffix,Role.ORG);
    }
    private String account(String name,Role role)throws Exception {String email=name+"@test.local";if(role==Role.ORG)orgEmail=email;users.save(new AppUser(email,passwords.encode("testPassword123"),name,role));return (String)call("POST","/api/auth/login",null,Map.of("email",email,"password","testPassword123")).body.get("token");}
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
    @Test void adminStockReadExcludesExpiredLotsWithoutChangingStoredProduct()throws Exception {
        long p=product();long lotId=lot(p,"STALE",5,2).id();
        var row=lots.findById(lotId).orElseThrow();row.setExpiryDate(LocalDate.now().minusDays(1));lots.save(row);
        assertThat(products.findById(p).orElseThrow().getQuantityOnHand()).isEqualTo(5);
        long before=auditEvents.count();var reply=call("GET",base+"/products",admin,null);
        var list=json.readValue(reply.raw,List.class);
        var product=(Map<?,?>)list.stream().filter(item->((Number)((Map<?,?>)item).get("id")).longValue()==p).findFirst().orElseThrow();
        assertThat(((Number)product.get("quantityOnHand")).intValue()).isZero();
        assertThat(products.findById(p).orElseThrow().getQuantityOnHand()).isEqualTo(5);
        assertThat(auditEvents.count()).isEqualTo(before);
        assertThat(((Number)call("GET",base+"/overview",admin,null).body.get("expiredUnits")).longValue()).isGreaterThanOrEqualTo(5);
    }
    @Test void orderPagesSearchSnapshotsAndKeepStableStatusFilteredPages()throws Exception {
        long p=product();lot(p,"PAGED",12,40);String marker="HISTORY-"+UUID.randomUUID();
        var product=products.findById(p).orElseThrow();product.setName(marker);products.save(product);
        long first=order(p,1),second=order(p,2),third=order(p,3);
        assertThat(call("PUT",base+"/orders/"+second+"/status",admin,Map.of("status","SHIPPED","reason","Доставка")).status).isEqualTo(200);
        product=products.findById(p).orElseThrow();product.setName("Переименован");products.save(product);
        var page0=call("GET",base+"/orders/page?q="+marker+"&state=PENDING&size=1&sort=oldest",admin,null);
        assertThat(page0.status).as(page0.raw).isEqualTo(200);assertThat(((Number)page0.body.get("totalElements")).intValue()).isEqualTo(2);
        var content=(List<?>)page0.body.get("content");var firstRow=(Map<?,?>)content.getFirst();assertThat(((Number)firstRow.get("id")).longValue()).isEqualTo(first);
        var item=(Map<?,?>)((List<?>)firstRow.get("items")).getFirst();assertThat(item.containsKey("productName")).isTrue();assertThat(item.containsKey("product")).isFalse();
        var page1=call("GET",base+"/orders/page?q="+marker+"&state=PENDING&size=1&page=1&sort=oldest",admin,null);
        assertThat(((Number)((Map<?,?>)((List<?>)page1.body.get("content")).getFirst()).get("id")).longValue()).isEqualTo(third);
    }
    @Test void adminPagedListsRejectInvalidQueriesAndEscapeSearchWildcards()throws Exception {
        assertThat(call("GET",base+"/orders/page",null,null).status).isEqualTo(401);
        assertThat(call("GET",base+"/audit/page",user,null).status).isEqualTo(403);
        for(String path:List.of("/orders/page?size=101","/orders/page?page=-1","/orders/page?sort=bad","/orders/page?state=bad","/audit/page?size=0"))
            assertThat(call("GET",base+path,admin,null).status).isEqualTo(400);
        String prefix="AUDIT-"+UUID.randomUUID();
        for(String suffix:List.of("%_","xx")){AuditEvent event=new AuditEvent();event.setActor("test-admin");event.setAction("READ_TEST");event.setResource("audit:test");event.setDetails(prefix+suffix);auditEvents.save(event);}
        var result=call("GET",base+"/audit/page?q="+prefix+"%25_&size=1",admin,null);
        assertThat(result.status).as(result.raw).isEqualTo(200);assertThat(((Number)result.body.get("totalElements")).intValue()).isEqualTo(1);
        assertThat(result.raw).contains(prefix+"%_");
        AuditEvent translated=new AuditEvent();translated.setActor("test-admin");translated.setAction("PRODUCT_PHOTO_UPDATED");translated.setResource("photo:test");translated.setDetails(prefix);auditEvents.save(translated);
        var russian=call("GET",base+"/audit/page?q="+java.net.URLEncoder.encode("Обновлено фото",java.nio.charset.StandardCharsets.UTF_8),admin,null);
        assertThat(russian.status).isEqualTo(200);assertThat(russian.raw).contains("PRODUCT_PHOTO_UPDATED");
    }
    @Test void discountConfirmationCannotUseAnOutdatedPrice()throws Exception {
        long p=product();lot(p,"QUOTE",5,20);
        var changed=new HashMap<>(productBody("QUOTE-"+p));changed.put("unitPrice",1100);
        assertThat(call("PUT",base+"/products/"+p,admin,changed).status).isEqualTo(200);
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",10,"reason","Старый расчёт","expectedPrice",900)).status).isEqualTo(409);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",10,"reason","Новый расчёт","expectedPrice",990)).status).isEqualTo(200);
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
        assertThat(((Map<?,?>)preview.body.get("recommendation")).get("suggestedPercent")).isEqualTo(0);
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
    @Test void smartPricingRequiresAdminAndAnalysisDoesNotChangePrices()throws Exception {
        long p=product();lot(p,"SMART",10,3);String path=base+"/smart-pricing";
        assertThat(call("GET",path,null,null).status).isEqualTo(401);
        assertThat(call("GET",path,user,null).status).isEqualTo(403);
        assertThat(call("POST",path+"/products/"+p+"/analyze",org,null).status).isEqualTo(403);
        var r=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(r.status).as(r.raw).isEqualTo(200);
        var report=(Map<?,?>)r.body.get("report");assertThat(report.get("confidence")).isEqualTo("LOW");
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(call("POST",path+"/products/"+p+"/analyze",admin,null).id()).isEqualTo(r.id());
        assertThat(call("POST",path+"/"+r.id()+"/reject",admin,Map.of("reason","Недостаточно истории")).status).isEqualTo(200);
        assertThat(call("POST",path+"/"+r.id()+"/apply",admin,Map.of("reason","Повторное подтверждение","percent",5)).status).isEqualTo(409);
    }
    @Test void smartPricingRejectsStaleInventoryAndSupersedesOldDecision()throws Exception {
        long p=product();lot(p,"SMART-OLD",10,3);String path=base+"/smart-pricing";
        long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();lot(p,"SMART-NEW",3,9);
        assertThat(call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Старый расчёт","percent",10)).status).isEqualTo(409);
        var updated=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(updated.id()).isNotEqualTo(id);
        assertThat(call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Заменённый расчёт","percent",10)).status).isEqualTo(409);
    }
    @Test void smartPricingHonorsFloorAndRollbackWritesActualHistory()throws Exception {
        var body=new HashMap<>(productBody("SMART-FLOOR-"+UUID.randomUUID()));body.put("minimumPrice",950);
        long p=call("POST",base+"/products",admin,body).id();lot(p,"SMART",10,5);String path=base+"/smart-pricing";
        long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();
        assertThat(call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Ниже минимума","percent",6)).status).isEqualTo(409);
        var applied=call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Проверяем спрос","percent",5));assertThat(applied.status).as(applied.raw).isEqualTo(200);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isEqualTo(5);
        assertThat(call("GET",path+"/products/"+p+"/history",admin,null).raw).contains("Проверяем спрос","unitsBefore7Days","revenueAfter");
        assertThat(call("POST",path+"/"+id+"/rollback",admin,Map.of("reason","Отмена эксперимента")).status).isEqualTo(200);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(call("POST",path+"/"+id+"/rollback",admin,Map.of("reason","Повторно")).status).isEqualTo(409);
    }
    @Test void smartRollbackCannotOverwriteLaterManualPriceChange()throws Exception {
        long p=product();lot(p,"SMART",10,5);String path=base+"/smart-pricing";
        long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();
        assertThat(call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Эксперимент","percent",5)).status).isEqualTo(200);
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("reason","Другое решение","percent",10)).status).isEqualTo(200);
        assertThat(call("POST",path+"/"+id+"/rollback",admin,Map.of("reason","Устаревший откат")).status).isEqualTo(409);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isEqualTo(10);
    }
    @Test void smartPricingConfirmationIsAtomicAndOnlyAppliesOnce()throws Exception {
        long p=product();lot(p,"SMART",10,5);String path=base+"/smart-pricing";
        long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();var pool=Executors.newFixedThreadPool(2);
        try{var first=pool.submit(()->call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Первый","percent",5)).status);
            var second=pool.submit(()->call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Второй","percent",5)).status);
            assertThat(List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }finally{pool.shutdownNow();}
        var history=json.readValue(call("GET",path+"/products/"+p+"/history",admin,null).raw,List.class);assertThat(history).hasSize(1);
    }
    @Test void smartPricingBlocksExpiredLotsAndDoesNotCallExternalAiWhenDisabled()throws Exception {
        long p=product();long lot=lot(p,"SMART",4,1).id();var l=lots.findById(lot).orElseThrow();l.setExpiryDate(LocalDate.now().minusDays(1));lots.save(l);String path=base+"/smart-pricing";
        var r=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(((Map<?,?>)r.body.get("report")).get("action")).isEqualTo("BLOCKED");
        assertThat(call("POST",path+"/"+r.id()+"/apply",admin,Map.of("reason","Просрочено","percent",5)).status).isEqualTo(409);
        var ai=call("POST",path+"/"+r.id()+"/explain",admin,null);assertThat(ai.status).isEqualTo(200);assertThat(ai.body.get("mode")).isEqualTo("ENGINE");
    }
    @Test void smartPricingUsesShippedHistoryAndActualPromotionOutcomes()throws Exception {
        long p=product();lot(p,"SMART",10,30);long pending=order(p,2);String path=base+"/smart-pricing";
        var before=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(((Map<?,?>)((Map<?,?>)before.body.get("report")).get("sales")).get("units30")).isEqualTo(0);
        call("PUT",base+"/orders/"+pending+"/status",admin,Map.of("status","SHIPPED","reason","Реальная отгрузка"));
        assertThat(call("POST",path+"/"+before.id()+"/apply",admin,Map.of("reason","Продажи изменились","percent",5)).status).isEqualTo(409);
        var r=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(((Map<?,?>)((Map<?,?>)r.body.get("report")).get("sales")).get("units30")).isEqualTo(2);
        assertThat(call("POST",path+"/"+r.id()+"/apply",admin,Map.of("reason","Акция","percent",5)).status).isEqualTo(200);
        long next=order(p,1);call("PUT",base+"/orders/"+next+"/status",admin,Map.of("status","SHIPPED","reason","После акции"));
        var history=json.readValue(call("GET",path+"/products/"+p+"/history",admin,null).raw,List.class);var first=(Map<?,?>)history.getFirst();
        assertThat(first.get("unitsBefore7Days")).isEqualTo(2);assertThat(first.get("unitsAfter")).isEqualTo(1);assertThat(new java.math.BigDecimal(first.get("revenueAfter").toString())).isEqualByComparingTo("950");
    }
    @Test void smartPricingDoesNotTreatUnknownLegacyCostAsZero()throws Exception {
        long id=product();var p=products.findById(id).orElseThrow();p.setQuantityOnHand(5);p.setUnitCost(null);p.setExpiryDate(LocalDate.now().plusDays(5));products.save(p);
        var result=call("POST",base+"/smart-pricing/products/"+id+"/analyze",admin,null);assertThat(result.status).isEqualTo(409);assertThat(result.raw).contains("себестоимость");assertThat(lots.findByProductId(id)).isEmpty();
    }
    private void enableDecisionAi()throws Exception {
        var current=call("GET",base+"/smart-pricing/settings",admin,null);var body=new HashMap<>(current.body);
        body.put("externalAiEnabled",true);body.put("scheduledAiEnabled",true);body.put("aiDailyBudget",3);
        var result=call("PUT",base+"/smart-pricing/settings",admin,body);assertThat(result.status).as(result.raw).isEqualTo(200);
    }
    private Map<String,Object> aiReply(int percent){return Map.of("mode","AI","suggestedPercent",percent,"reviewAfterHours",12,"requiresConfirmation",true,"explanation","Выбран допустимый эксперимент, данных мало","risks",List.of("Нет причинной оценки"),"model","test-model:free");}
    @Test void aiStrategyNeedsConsentThenPersistsSourceAndUsesSelectedScenario()throws Exception {
        long p=product();lot(p,"AI",10,5);String path=base+"/smart-pricing";long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();
        assertThat(call("POST",path+"/"+id+"/ai-decide",admin,null).status).isEqualTo(409);enableDecisionAi();
        var claim=smartPricing.claimAi(id,false);var completed=smartPricing.completeAi(claim,aiReply(5),false);
        assertThat(completed.get("source")).isEqualTo("AI");assertThat(completed.get("selectedPercent")).isEqualTo(5);assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(call("POST",path+"/"+id+"/apply",admin,Map.of("reason","Подтверждаю AI-сценарий")).status).isEqualTo(200);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isEqualTo(5);
    }
    @Test void emergencyStopInvalidatesAiInFlightAndAiCannotSelectOutsideEngineScenarios()throws Exception {
        long p=product();lot(p,"AI",10,5);String path=base+"/smart-pricing";long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();enableDecisionAi();
        var claim=smartPricing.claimAi(id,true);assertThatThrownBy(()->smartPricing.completeAi(claim,aiReply(80),true)).isInstanceOf(IllegalStateException.class);
        call("POST",path+"/stop",admin,null);assertThatThrownBy(()->smartPricing.completeAi(claim,aiReply(5),true)).isInstanceOf(IllegalStateException.class);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
    }
    @Test void aiUnavailableKeepsEngineAndLowConfidenceAiNeverAutoApplies()throws Exception {
        long p=product();lot(p,"AI",10,5);String path=base+"/smart-pricing";long id=call("POST",path+"/products/"+p+"/analyze",admin,null).id();enableDecisionAi();
        var result=call("POST",path+"/"+id+"/ai-decide",admin,null);assertThat(result.status).as(result.raw).isEqualTo(200);assertThat(result.body.get("source")).isEqualTo("ENGINE");
        assertThat(call("GET",path+"/ai-status",user,null).status).isEqualTo(403);
        assertThat(call("GET",path+"/ai-status",admin,null).raw).doesNotContain("sk-or-v1-","Bearer");
        var setting=smartSettings.findById(1L).orElseThrow();setting.lastAiRequestAt=java.time.LocalDateTime.now().minusMinutes(2);setting.mode="AUTOPILOT";setting.allowLowConfidence=true;setting.allowedCategoriesJson="[\"продукты\"]";smartSettings.save(setting);
        var claim=smartPricing.claimAi(id,false);smartPricing.completeAi(claim,aiReply(5),false);
        assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
    }
    @Test void competitorImportUsesVerifiedIdentityAndInvalidatesStalePricingDecision()throws Exception {
        long p=product();lot(p,"MARKET",10,30);var product=products.findById(p).orElseThrow();product.setBarcode("4006381333931");products.save(product);String path=base+"/competitors";
        var input=Map.of("name","Тестовый разрешённый источник","baseUrl","https://prices.example.org","active",true,"permissionConfirmed",true);
        assertThat(call("POST",path,user,input).status).isEqualTo(403);long source=call("POST",path,admin,input).id();
        String date=java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusHours(1).withNano(0).toString();
        String csv="sku,barcode,unit,price,currency,observedAt,url\n"+product.getSku()+",4006381333931,шт,950.00,KZT,"+date+",https://prices.example.org/item";
        var body=Map.of("sourceId",source,"csv",csv,"identityConfirmed",true);
        var imported=call("POST",path+"/import",admin,body);assertThat(imported.status).as(imported.raw).isEqualTo(200);assertThat(imported.body.get("imported")).isEqualTo(1);
        assertThat(call("POST",path+"/import",admin,body).body.get("duplicates")).isEqualTo(1);
        var compared=call("GET",path+"/products/"+p,admin,null);var context=(Map<?,?>)compared.body.get("comparison");assertThat(context.get("position")).isEqualTo("HIGHER");assertThat(new java.math.BigDecimal(context.get("medianPrice").toString())).isEqualByComparingTo("950");
        long decision=call("POST",base+"/smart-pricing/products/"+p+"/analyze",admin,null).id();
        String next=java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(30).withNano(0).toString();
        assertThat(call("POST",path+"/import",admin,Map.of("sourceId",source,"identityConfirmed",true,"csv",csv.replace(date,next).replace("950.00","900.00"))).status).isEqualTo(200);
        assertThat(call("POST",base+"/smart-pricing/"+decision+"/apply",admin,Map.of("percent",5,"reason","Старые рыночные данные")).status).isEqualTo(409);
        var off=new HashMap<String,Object>(input);off.put("active",false);assertThat(call("PUT",path+"/"+source,admin,off).status).isEqualTo(200);
        assertThat(((Map<?,?>)call("GET",path+"/products/"+p,admin,null).body.get("comparison")).get("status")).isEqualTo("NO_VERIFIED_PRICES");
    }
    @Test void competitorImportIsAtomicAndRejectsWrongCurrencyPackageAndSource()throws Exception {
        long p=product();var product=products.findById(p).orElseThrow();product.setBarcode("4006381333931");products.save(product);String path=base+"/competitors";
        long source=call("POST",path,admin,Map.of("name","Тестовый CSV","baseUrl","https://csv.example.org","active",true,"permissionConfirmed",true)).id();
        String date=java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusHours(1).withNano(0).toString();
        String header="sku,barcode,unit,price,currency,observedAt,url\n",row=product.getSku()+",4006381333931,шт,950.00,KZT,"+date+",https://csv.example.org/item";
        for(String bad:List.of(row.replace("KZT","USD"),row.replace("шт","кг"),row.replace("4006381333931","4006381333932"),row.replace("csv.example.org","other.example.org"))){
            var result=call("POST",path+"/import",admin,Map.of("sourceId",source,"identityConfirmed",true,"csv",header+row+"\n"+bad));assertThat(result.status).as(result.raw).isEqualTo(400);
            var history=(List<?>)call("GET",path+"/products/"+p,admin,null).body.get("history");assertThat(history).isEmpty();
        }
        String old=header+row.replace(date,java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusDays(8).withNano(0).toString());
        assertThat(call("POST",path+"/import",admin,Map.of("sourceId",source,"identityConfirmed",true,"csv",old)).status).isEqualTo(200);
        assertThat(((Map<?,?>)call("GET",path+"/products/"+p,admin,null).body.get("comparison")).get("status")).isEqualTo("NO_VERIFIED_PRICES");
    }
    private long verifiedCharity()throws Exception {
        long id=users.findByEmail(orgEmail).orElseThrow().getId();var v=new OrganizationVerification();v.setUserId(id);v.setOrganizationName("Синтетическая организация для теста");v.setRegistrationNumber("TEST-"+UUID.randomUUID());v.setStatus("VERIFIED");organizationVerifications.save(v);return id;
    }
    private void charityAcceptance(long organizationId)throws Exception {
        var result=call("PUT",base+"/social-proposals/organizations/"+organizationId,admin,Map.of("region","Тестовый регион","categories",List.of("Продукты"),"storageConditions","Условия подтверждены только в тестовом сценарии","minimumRemainingDays",1,"active",true,"informationConfirmed",true));assertThat(result.status).as(result.raw).isEqualTo(200);
    }
    @Test void socialProposalDoesNotReserveBeforeExplicitVerifiedApprovalAndUsesExistingHandover()throws Exception {
        long p=product();lot(p,"DONATE",10,2);long decision=call("POST",base+"/smart-pricing/products/"+p+"/analyze",admin,null).id();String path=base+"/social-proposals";
        assertThat(call("POST",path+"/decisions/"+decision,user,null).status).isEqualTo(403);
        var proposal=call("POST",path+"/decisions/"+decision,admin,null);assertThat(proposal.status).as(proposal.raw).isEqualTo(200);assertThat(proposal.body.get("status")).isEqualTo("PROPOSED");assertThat(call("POST",path+"/decisions/"+decision,admin,null).id()).isEqualTo(proposal.id());
        assertThat(products.findById(p).orElseThrow().getStatus()).isEqualTo(com.aria.enums.ProductStatus.ACTIVE);assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(10);
        long organization=verifiedCharity();var approval=new HashMap<String,Object>(Map.of("organizationId",organization,"quantity",5,"validUntil",LocalDate.now().plusDays(2).toString(),"reason","Согласовано","suitabilityConfirmed",true,"transportConfirmed",true));
        assertThat(call("POST",path+"/"+proposal.id()+"/approve",admin,approval).status).isEqualTo(409);charityAcceptance(organization);
        approval.put("suitabilityConfirmed",false);assertThat(call("POST",path+"/"+proposal.id()+"/approve",admin,approval).status).isEqualTo(400);approval.put("suitabilityConfirmed",true);
        var approved=call("POST",path+"/"+proposal.id()+"/approve",admin,approval);assertThat(approved.status).as(approved.raw).isEqualTo(200);assertThat(products.findById(p).orElseThrow().getStatus()).isEqualTo(com.aria.enums.ProductStatus.RESERVED);
        assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(10);long offer=((Number)approved.body.get("offerId")).longValue();
        assertThat(call("POST",base+"/social/"+offer+"/confirm",admin,Map.of("reason","Фактическая выдача подтверждена")).status).isEqualTo(200);assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(5);
        assertThat(call("GET",path,admin,null).raw).contains("TRANSFERRED");
    }
    @Test void socialProposalRejectsStaleStockAndRevokedVerificationBeforeHandover()throws Exception {
        long p=product();lot(p,"DONATE",10,2);long decision=call("POST",base+"/smart-pricing/products/"+p+"/analyze",admin,null).id();String path=base+"/social-proposals";long proposal=call("POST",path+"/decisions/"+decision,admin,null).id();long organization=verifiedCharity();charityAcceptance(organization);
        var approved=call("POST",path+"/"+proposal+"/approve",admin,Map.of("organizationId",organization,"quantity",10,"validUntil",LocalDate.now().plusDays(2).toString(),"reason","Согласовано","suitabilityConfirmed",true,"transportConfirmed",true));assertThat(approved.status).as(approved.raw).isEqualTo(200);
        var v=organizationVerifications.findByUserId(organization).orElseThrow();v.setStatus("PENDING_VERIFICATION");organizationVerifications.save(v);long offer=((Number)approved.body.get("offerId")).longValue();
        assertThat(call("POST",base+"/social/"+offer+"/confirm",admin,Map.of("reason","Проверка отменена")).status).isEqualTo(409);assertThat(inventory.available(products.findById(p).orElseThrow())).isEqualTo(10);
        assertThat(call("POST",path+"/"+proposal+"/cancel",admin,Map.of("reason","Не подтверждена организация")).status).isEqualTo(200);assertThat(products.findById(p).orElseThrow().getStatus()).isEqualTo(com.aria.enums.ProductStatus.CANDIDATE);
    }
    private long statisticalPricingProduct()throws Exception {
        var body=new HashMap<>(productBody("STAT-"+UUID.randomUUID()));body.put("unitPrice",1600);
        long id=call("POST",base+"/products",admin,body).id();lot(id,"STAT",5000,3);var p=products.findById(id).orElseThrow();p.setReceivedAt(LocalDate.now().minusDays(40));products.save(p);
        int[] prices={800,1000,1250,1600},quantities={938,600,384,234};
        for(int i=0;i<4;i++){
            var from=LocalDate.now().minusDays(24-i*6).atStartOfDay();var price=java.math.BigDecimal.valueOf(prices[i]);
            var change=new PriceChange();change.productId=id;change.changedAt=from;change.actor="test-statistics";change.reason="Синтетический период только для проверки";
            change.oldBasePrice=java.math.BigDecimal.valueOf(i==0?1600:prices[i-1]);change.newBasePrice=price;change.oldPercent=0;change.newPercent=0;change.costAtChange=java.math.BigDecimal.valueOf(800);change.availableAtChange=5000;priceChanges.save(change);
            var order=new com.aria.entity.Order();order.setStatus("SHIPPED");order.setShippedAt(from.plusDays(1));order.setOwnerEmail("statistics@test.local");order.setTotalAmount(price.multiply(java.math.BigDecimal.valueOf(quantities[i])));order.getItems().add(new OrderItem(p,quantities[i],price));orders.save(order);
        }return id;
    }
    private Reply configureAutopilot(String category,int maximum,boolean lowConfidence)throws Exception {
        String path=base+"/smart-pricing";var current=call("GET",path+"/settings",admin,null);
        var body=new HashMap<String,Object>();body.put("version",current.body.get("version"));body.put("mode","AUTOPILOT");body.put("maximumDiscount",maximum);body.put("cooldownHours",24);body.put("allowLowConfidence",lowConfidence);body.put("allowRestore",false);body.put("allowedCategories",List.of(category));return call("PUT",path+"/settings",admin,body);
    }
    @Test void autopilotRequiresExplicitBoundsAndHonorsCategoryConfidenceAndDiscountLimits()throws Exception {
        long p=statisticalPricingProduct();String path=base+"/smart-pricing";
        var first=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(first.status).as(first.raw).isEqualTo(200);
        assertThat(((Map<?,?>)first.body.get("report")).get("action")).isEqualTo("DISCOUNT");assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(configureAutopilot("*",90,true).status).isEqualTo(400);
        assertThat(configureAutopilot("Электроника",90,true).status).isEqualTo(200);
        call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(configureAutopilot("Продукты",90,false).status).isEqualTo(200);
        call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(configureAutopilot("Продукты",1,true).status).isEqualTo(200);
        call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
        assertThat(configureAutopilot("Продукты",90,true).status).isEqualTo(200);
        var applied=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(applied.status).as(applied.raw).isEqualTo(200);assertThat(applied.body.get("status")).isEqualTo("APPLIED");assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isBetween(1,50);
        assertThat(call("POST",path+"/stop",admin,null).body.get("mode")).isEqualTo("RECOMMEND");
        assertThat(call("POST",path+"/"+applied.id()+"/rollback",admin,Map.of("reason","Откат автоматического решения")).status).isEqualTo(200);
        call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isZero();
    }
    @Test void autopilotCooldownPreventsFrequentChangesAndSettingsCannotBeOverwrittenStale()throws Exception {
        long p=statisticalPricingProduct();String path=base+"/smart-pricing";
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",5,"reason","Недавнее ручное изменение")).status).isEqualTo(200);
        var config=configureAutopilot("Продукты",90,true);assertThat(config.status).isEqualTo(200);
        var result=call("POST",path+"/products/"+p+"/analyze",admin,null);assertThat(result.status).as(result.raw).isEqualTo(200);assertThat(result.body.get("status")).isEqualTo("PENDING");assertThat(products.findById(p).orElseThrow().getDiscountPercent()).isEqualTo(5);
        assertThat(auditEvents.findTop100ByOrderByCreatedAtDescIdDesc()).anyMatch(e->"SMART_AUTOPILOT_SKIPPED".equals(e.getAction())&&e.getDetails().contains("Пауза"));
        var stale=new HashMap<String,Object>(config.body);stale.put("version",-1);assertThat(call("PUT",path+"/settings",admin,stale).status).isEqualTo(409);
        assertThat(call("POST",path+"/stop",user,null).status).isEqualTo(403);
        assertThat(call("PUT",path+"/settings",user,config.body).status).isEqualTo(403);
    }
    @Test void soldOutDiscountCanBeClearedWithoutMakingExpiredStockSellable()throws Exception {
        long p=product();lot(p,"EMPTY",1,5);assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",20,"reason","Срок")).status).isEqualTo(200);
        order(p,1);assertThat(inventory.available(products.findById(p).orElseThrow())).isZero();
        assertThat(call("PUT",base+"/pricing/"+p,admin,Map.of("percent",0,"reason","Скидка закончена")).status).isEqualTo(200);
        assertThat(call("GET","/api/products/"+p,null,null).status).isEqualTo(404);
    }
}
