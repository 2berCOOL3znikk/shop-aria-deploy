package com.aria.service;

import com.aria.entity.SocialOffer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** The same document is used by the owning recipient and authorized shop administrator. */
@Service
public class SocialActService {
    public ResponseEntity<String> document(SocialOffer offer,String organizationName) {
        if(offer.getUnitCostSnapshot()==null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Для старой передачи не сохранена стоимость. Требуется ручная сверка документа");
        String productName = esc(offer.getProductNameSnapshot()==null?offer.getProduct().getName():offer.getProductNameSnapshot());
        String productSku = esc(offer.getSkuSnapshot()==null?offer.getProduct().getSku():offer.getSkuSnapshot());
        String reason = esc(offer.getProduct().getAriaReason());
        java.math.BigDecimal unitCost = offer.getUnitCostSnapshot()==null?java.math.BigDecimal.ZERO:offer.getUnitCostSnapshot();
        int quantity = offer.getQuantityClaimed() == null ? 0 : offer.getQuantityClaimed();
        String html = """
                <!doctype html><html lang="ru"><meta charset="utf-8"><title>Акт передачи ARIA-%d</title>
                <style>body{font:15px Arial,sans-serif;max-width:1100px;margin:36px auto;padding:0 24px;color:#172b3a}table{border-collapse:collapse;width:100%%;margin-top:24px}th,td{border:1px solid #ccd6dc;padding:10px;text-align:left}th{background:#edf2f4}.sign{margin-top:72px;display:flex;justify-content:space-between}</style>
                <h1>Акт приёма-передачи ARIA-%d</h1><p>Дата: %s</p><p>Передающая сторона: ARIA Shop</p><p>Принимающая организация: %s</p>
                <p>Партия социальной полки. Количество зарезервировано организацией-получателем.</p>
                <table><tr><th>SKU</th><th>Наименование</th><th>Количество</th><th>Учётная стоимость за единицу, ₸</th><th>Основание</th></tr>
                <tr><td>%s</td><td>%s</td><td>%d</td><td>%s</td><td>%s</td></tr></table>
                <p><strong>Итого к передаче:</strong> %d шт. · <strong>Сумма оценки:</strong> %s ₸ · <strong>Оплата получателем:</strong> 0 ₸</p>
                <p>Черновик: проверьте количество, дату и реквизиты сторон перед подписанием.</p><div class="sign"><span>Передал: ____________________</span><span>Принял: ____________________</span></div>
                </html>
                """.formatted(offer.getId(), offer.getId(),
                offer.getClaimedAt() == null ? java.time.LocalDate.now() : offer.getClaimedAt().toLocalDate(),
                esc(organizationName), productSku, productName, quantity, unitCost.toPlainString(), reason,
                quantity, unitCost.multiply(java.math.BigDecimal.valueOf(quantity)).toPlainString());
        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8")
                .header("Cache-Control","private, no-store")
                .header("Content-Disposition", "attachment; filename=\"aria-social-act-" + offer.getId() + ".html\"").body(html);

    }
    private String esc(String value) {return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
}
