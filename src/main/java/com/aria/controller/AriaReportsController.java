package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.TransferLot;
import com.aria.entity.TransferRecord;
import com.aria.repository.ProductRepository;
import com.aria.repository.TransferLotRepository;
import com.aria.repository.TransferRecordRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/aria/reports")
public class AriaReportsController {
    private final ProductRepository products;
    private final TransferLotRepository lots;
    private final TransferRecordRepository records;

    public AriaReportsController(ProductRepository products, TransferLotRepository lots, TransferRecordRepository records) {
        this.products = products;
        this.lots = lots;
        this.records = records;
    }

    @GetMapping(value = "/inventory.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> inventoryCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFSKU,Наименование,Категория,Остаток,Цена KZT,Статус,Годен до\r\n");
        for (Product product : products.findAll()) {
            csv.append(csv(product.getSku())).append(',')
                    .append(csv(product.getName())).append(',')
                    .append(csv(product.getCategory())).append(',')
                    .append(product.getQuantityOnHand() == null ? 0 : product.getQuantityOnHand()).append(',')
                    .append(product.getUnitPrice() == null ? "" : product.getUnitPrice().toPlainString()).append(',')
                    .append(csv(product.getStatus() == null ? "" : product.getStatus().name())).append(',')
                    .append(csv(product.getExpiryDate() == null ? "" : product.getExpiryDate().toString())).append("\r\n");
        }
        return csvDownload("aria-inventory.csv", csv.toString());
    }

    @GetMapping(value = "/acts.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> actsCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFЛот,Организация,Дата передачи,SKU,Наименование,Количество,Себестоимость за единицу KZT\r\n");
        for (TransferRecord record : records.findAllByOrderByTransferredAtDesc()) {
            csv.append(record.getLotId()).append(',')
                    .append(csv(record.getPartner())).append(',')
                    .append(csv(record.getTransferredAt().toString())).append(',')
                    .append(csv(record.getSku())).append(',')
                    .append(csv(record.getProductName())).append(',')
                    .append(record.getQuantity()).append(',')
                    .append(record.getUnitCost() == null ? "" : record.getUnitCost().toPlainString()).append("\r\n");
        }
        return csvDownload("aria-transfer-acts.csv", csv.toString());
    }

    @GetMapping(value = "/acts/{lotId}/printable", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> printableAct(@PathVariable Long lotId) {
        TransferLot lot = lots.findById(lotId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лот не найден"));
        List<ActRow> rows = new ArrayList<>();
        List<TransferRecord> snapshots = records.findByLotIdOrderByIdAsc(lotId);
        if (!snapshots.isEmpty()) {
            snapshots.forEach(item -> rows.add(new ActRow(item.getSku(), item.getProductName(), item.getQuantity(), item.getUnitCost())));
        } else {
            for (Product product : lot.getItems()) {
                rows.add(new ActRow(product.getSku(), product.getName(), product.getQuantityOnHand() == null ? 0 : product.getQuantityOnHand(), product.getUnitCost()));
            }
        }
        StringBuilder table = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        for (ActRow row : rows) {
            BigDecimal line = (row.unitCost() == null ? BigDecimal.ZERO : row.unitCost()).multiply(BigDecimal.valueOf(row.quantity()));
            total = total.add(line);
            table.append("<tr><td>").append(html(row.sku())).append("</td><td>").append(html(row.name())).append("</td><td>")
                    .append(row.quantity()).append("</td><td>").append(row.unitCost() == null ? "—" : row.unitCost().toPlainString())
                    .append("</td><td>").append(line.toPlainString()).append("</td></tr>");
        }
        LocalDate date = lot.getCreatedAt() == null ? LocalDate.now() : lot.getCreatedAt().toLocalDate();
        String document = """
                <!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Акт ARIA-%d</title><style>
                body{font:15px Arial,sans-serif;max-width:1050px;margin:34px auto;padding:0 24px;color:#24343d}
                h1{font-size:26px}table{width:100%%;border-collapse:collapse;margin:22px 0}th,td{border:1px solid #cbd5d8;padding:9px;text-align:left}th{background:#edf2ef}
                .sign{display:flex;justify-content:space-between;gap:30px;margin-top:70px}.toolbar{display:flex;gap:10px;margin-bottom:22px}
                button{padding:10px 16px;border:0;border-radius:8px;background:#557d6c;color:white;cursor:pointer}
                @media print{.toolbar{display:none}body{margin:0 auto;padding:0 10mm}}
                </style></head><body><div class="toolbar"><button onclick="window.print()">Печать / сохранить в PDF</button></div>
                <h1>Акт приёма-передачи ARIA-%d</h1><p>Дата: %s</p><p>Передающая сторона: ARIA · Smart Retail &amp; Social Impact</p>
                <p>Принимающая организация: %s</p><p>Статус лота: %s</p>
                <table><thead><tr><th>SKU</th><th>Товар</th><th>Количество</th><th>Себестоимость за единицу, ₸</th><th>Сумма, ₸</th></tr></thead><tbody>%s</tbody></table>
                <p><strong>Итого:</strong> %s ₸</p>
                <p>Подписи сторон подтверждают фактическое количество и состояние переданных позиций.</p>
                <div class="sign"><span>Передал: __________________________</span><span>Принял: __________________________</span></div>
                </body></html>
                """.formatted(lotId, lotId, date, html(lot.getPartner()), html(lot.getState()), table, total.toPlainString());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(document);
    }

    private ResponseEntity<String> csvDownload(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private String csv(String value) {
        if (value == null) return "";
        String safe = value.matches("^[=+@-].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String html(String value) {
        return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }

    private record ActRow(String sku, String name, int quantity, BigDecimal unitCost) {}
}