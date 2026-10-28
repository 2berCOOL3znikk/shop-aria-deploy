package com.aria.controller;

import com.aria.entity.Product;
import com.aria.entity.TransferLot;
import com.aria.entity.TransferRecord;
import com.aria.enums.ProductStatus;
import com.aria.repository.ProductRepository;
import com.aria.repository.TransferLotRepository;
import com.aria.repository.TransferRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/aria/logistics")
public class AriaLogisticsController {
    private static final int LOT_THRESHOLD = 10;
    private static final Set<String> ACT_STAGES = Set.of("DRAFT", "CONFIRMED", "PRINTED", "SIGNED");
    private final ProductRepository products;
    private final TransferLotRepository lots;
    private final TransferRecordRepository transferRecords;
    private final com.aria.service.InventoryService inventory;

    public AriaLogisticsController(ProductRepository products, TransferLotRepository lots, TransferRecordRepository transferRecords, com.aria.service.InventoryService inventory) {
        this.products = products;
        this.lots = lots;
        this.transferRecords = transferRecords;
        this.inventory=inventory;
    }

    @GetMapping("/queue")
    @Transactional(readOnly = true)
    public Map<String, Object> queue() {
        LocalDate today = LocalDate.now();
        List<Product> candidates = products.findByStatus(ProductStatus.CANDIDATE);
        List<Product> expired = candidates.stream()
                .filter(p -> p.getExpiryDate() != null && p.getExpiryDate().isBefore(today)).toList();
        List<Product> critical = candidates.stream()
                .filter(p -> p.getExpiryDate() != null && !p.getExpiryDate().isBefore(today))
                .filter(p -> ChronoUnit.DAYS.between(today, p.getExpiryDate()) <= 3).toList();
        List<Product> waiting = candidates.stream()
                .filter(p -> p.getExpiryDate() == null || ChronoUnit.DAYS.between(today, p.getExpiryDate()) > 3)
                .toList();
        int waitingUnits = waiting.stream().mapToInt(p -> Math.max(0, p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand())).sum();
        List<Map<String, Object>> draftLots = lots.findAll().stream().filter(lot -> ACT_STAGES.contains(lot.getState())).map(lot -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", lot.getId());
            row.put("partner", lot.getPartner());
            row.put("itemCount", lot.getItems().size());
            row.put("quantity", lot.getItems().stream().mapToInt(p -> Math.max(0, p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand())).sum());
            row.put("createdAt", lot.getCreatedAt());
            row.put("state", lot.getState());
            return row;
        }).toList();
        return Map.of(
                "threshold", LOT_THRESHOLD,
                "waitingUnits", waitingUnits,
                "waitingProgress", Math.min(100, (waitingUnits * 100) / LOT_THRESHOLD),
                "waitingReady", waitingUnits >= LOT_THRESHOLD,
                "waiting", waiting.stream().map(this::productRow).toList(),
                "critical", critical.stream().map(this::productRow).toList(),
                "expired", expired.stream().map(this::productRow).toList(),
                "draftLots", draftLots
        );
    }

    @PostMapping("/lots/{lotId}/confirm")
    @Transactional
    public Map<String, Object> confirmAct(@PathVariable Long lotId) {
        return advanceAct(lotId, "DRAFT", "CONFIRMED");
    }

    @PostMapping("/lots/{lotId}/mark-printed")
    @Transactional
    public Map<String, Object> markActPrinted(@PathVariable Long lotId) {
        return advanceAct(lotId, "CONFIRMED", "PRINTED");
    }

    @PostMapping("/lots/{lotId}/sign")
    @Transactional
    public Map<String, Object> signAct(@PathVariable Long lotId) {
        return advanceAct(lotId, "PRINTED", "SIGNED");
    }

    @PostMapping("/lots/{lotId}/confirm-transfer")
    @Transactional
    public Map<String, Object> confirmTransfer(@PathVariable Long lotId) {
        TransferLot lot = findLot(lotId);
        if (!"SIGNED".equals(lot.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сначала распечатайте и подпишите акт обеими сторонами");
        }
        LocalDate today = LocalDate.now();
        for (Product product : lot.getItems()) {
            inventory.lock(product.getId());
            if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(today)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Просроченный товар нельзя передать");
            }
            if (product.getStatus() != ProductStatus.RESERVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Состав лота изменён; обновите страницу");
            }
        }
        LocalDateTime transferredAt = LocalDateTime.now();
        List<TransferRecord> snapshots = new ArrayList<>();
        lot.getItems().forEach(product -> {
            snapshots.add(new TransferRecord(lot.getId(), product.getId(), product.getSku(), product.getName(),
                    product.getQuantityOnHand() == null ? 0 : product.getQuantityOnHand(), product.getUnitCost(),
                    lot.getPartner() == null ? "" : lot.getPartner(), transferredAt, product.getAriaReason()));
            inventory.consume(product,product.getQuantityOnHand(),"TRANSFER_STOCK");
            product.setStatus(ProductStatus.TRANSFERRED);
            product.setQuantityOnHand(0);
        });
        transferRecords.saveAll(snapshots);
        lot.setState("TRANSFERRED");
        lots.save(lot);
        return Map.of("id", lot.getId(), "state", lot.getState(), "itemCount", lot.getItems().size());
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> transferHistory() {
        List<Map<String, Object>> history = new ArrayList<>();
        for (TransferLot lot : lots.findAll()) {
            List<TransferRecord> snapshots = transferRecords.findByLotIdOrderByIdAsc(lot.getId());
            List<Map<String, Object>> items = new ArrayList<>();
            int quantity = 0;
            BigDecimal total = BigDecimal.ZERO;
            if (!snapshots.isEmpty()) {
                for (TransferRecord record : snapshots) {
                    int itemQuantity = record.getQuantity() == null ? 0 : record.getQuantity();
                    BigDecimal unitCost = record.getUnitCost() == null ? BigDecimal.ZERO : record.getUnitCost();
                    BigDecimal amount = unitCost.multiply(BigDecimal.valueOf(itemQuantity));
                    quantity += itemQuantity;
                    total = total.add(amount);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("sku", record.getSku());
                    item.put("name", record.getProductName());
                    item.put("quantity", itemQuantity);
                    item.put("reason", record.getReason());
                    item.put("unitCost", record.getUnitCost());
                    item.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
                    items.add(item);
                }
            } else {
                for (Product product : lot.getItems()) {
                    int itemQuantity = product.getQuantityOnHand() == null ? 0 : product.getQuantityOnHand();
                    BigDecimal unitCost = product.getUnitCost() == null ? BigDecimal.ZERO : product.getUnitCost();
                    BigDecimal amount = unitCost.multiply(BigDecimal.valueOf(itemQuantity));
                    quantity += itemQuantity;
                    total = total.add(amount);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("sku", product.getSku());
                    item.put("name", product.getName());
                    item.put("quantity", itemQuantity);
                    item.put("reason", product.getAriaReason());
                    item.put("unitCost", product.getUnitCost());
                    item.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
                    items.add(item);
                }
            }
            LocalDateTime occurredAt = snapshots.isEmpty() ? lot.getCreatedAt() : snapshots.get(0).getTransferredAt();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", lot.getId());
            row.put("partner", lot.getPartner());
            row.put("createdAt", occurredAt);
            row.put("state", lot.getState());
            row.put("itemCount", items.size());
            row.put("quantity", quantity);
            row.put("totalAmount", total.setScale(2, RoundingMode.HALF_UP));
            row.put("items", items);
            history.add(row);
        }
        history.sort((left, right) -> {
            LocalDateTime a = (LocalDateTime) left.get("createdAt");
            LocalDateTime b = (LocalDateTime) right.get("createdAt");
            if (a == null) return b == null ? 0 : 1;
            if (b == null) return -1;
            return b.compareTo(a);
        });
        return history;
    }

    private Map<String, Object> advanceAct(Long lotId, String expected, String next) {
        TransferLot lot = findLot(lotId);
        if (!expected.equals(lot.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Недопустимый переход статуса акта");
        }
        lot.setState(next);
        lots.save(lot);
        return Map.of("id", lot.getId(), "state", lot.getState(), "itemCount", lot.getItems().size());
    }

    private TransferLot findLot(Long lotId) {
        return lots.findById(lotId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лот не найден"));
    }

    private Map<String, Object> productRow(Product product) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", product.getId());
        row.put("name", product.getName());
        row.put("sku", product.getSku());
        row.put("quantity", product.getQuantityOnHand() == null ? 0 : product.getQuantityOnHand());
        row.put("expiryDate", product.getExpiryDate());
        row.put("daysToExpiry", product.getExpiryDate() == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), product.getExpiryDate()));
        row.put("reason", product.getAriaReason());
        return row;
    }
}
