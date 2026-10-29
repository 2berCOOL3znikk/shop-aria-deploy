package com.aria.controller;
import com.aria.service.integrations.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController @RequestMapping("/api/admin/control/integrations")
public class IntegrationReadinessController {
 private final PosterPosAdapter poster;public IntegrationReadinessController(PosterPosAdapter p){poster=p;}
 @GetMapping public Object list(){return Map.of("checkedOn","2026-10-29","pos",List.of(
  Map.of("name","Poster POS","status","ADAPTER_PREPARED_NOT_CONNECTED","docs","https://dev.joinposter.com/docs/v3/start/index","required","Отдельный Poster access_token / OAuth, разрешение владельца аккаунта, account/точки, проверенное сопоставление product_id и утверждённое направление синхронизации. OpenRouter-ключ не подходит."),
  Map.of("name","iiko","status","NOT_CONNECTED","docs","https://api.iiko.help/docs/","required","Регистрация приложения и доступ организации: appId, clientSecret и apiKey согласно актуальному iiko API. Без платных подключений и без записи во внешнюю кассу.")),
  "competitors",List.of(
   Map.of("name","Kaspi.kz","status","CSV_ONLY","docs","https://guide.kaspi.kz/partner/ru/shop/api/goods","note","Документация относится к товарам продавца. Публичное API мониторинга конкурентов этим не подтверждено."),
   Map.of("name","Magnum","status","CSV_OR_PARTNER_FEED_REQUIRED","docs","https://magnum.kz/","note","Обнаружены каталог и портал поставщиков, но не подтверждён публичный API цен для мониторинга. Нужен разрешённый экспорт/API с условиями доступа."),
   Map.of("name","SMALL","status","CSV_OR_PARTNER_FEED_REQUIRED","docs","https://small.kz/","note","Публичная документация мониторинга не обнаружена; прямое чтение сайта вернуло 403. Ограничение не обходится."),
   Map.of("name","Arbuz.kz","status","CSV_OR_PARTNER_FEED_REQUIRED","docs","https://arbuz.kz/","note","Публичная документация мониторинга не обнаружена на проверенных официальных страницах. Нужен разрешённый источник с регионом и сопоставимыми SKU.")),"note","Ни один внешний POS или автоматический источник конкурентов не обозначен подключённым. CSV и внутренний POS работают независимо.");}
 public record PosterPlan(@NotNull @Valid PosController.Sale sale,@NotNull @Size(max=100) Map<@NotNull Long,@NotBlank @Pattern(regexp="[0-9]{1,20}") String> productIds){}
 @PostMapping("/poster/prepare") public Object plan(@Valid @RequestBody PosterPlan p){return poster.prepare(new PosAdapter.Sale(p.sale().requestKey(),p.sale().lines().stream().map(l->new PosAdapter.Line(l.productId(),l.quantity(),l.expectedPrice())).toList()),p.productIds());}
}
