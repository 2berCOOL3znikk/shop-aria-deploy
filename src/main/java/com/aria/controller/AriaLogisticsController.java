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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aria/logistics")
public class AriaLogisticsController {
    private static final int LOT_THRESHOLD = 10;
    private final ProductRepository products;
    private final TransferLotRepository lots;
    private final TransferRecordRepository transferRecords;

    public AriaLogisticsController(ProductRepository products, TransferLotRepository lots, TransferRecordRepository transferRecords) {
        this.products = products;
        this.lots = lots;
        this.transferRecords = transferRecords;
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
        int waitingUnits = waiting.stream().mapToInt(p -> p.getQuantityOnHand() == null ? 0 : p.getQuantityOnHand()).sum();
        List<Map<String, Object>> draftLots = lots.findAll().stream().filter(lot -> "DRAFT".equals(lot.getState())).map(lot -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", lot.getId());
            row.put("partner", lot.getPartner());
            row.put("itemCount", lot.getItems().size());
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

    @PostMapping("/lots/{lotId}/confirm-transfer")
    @Transactional
    public Map<String, Object> confirmTransfer(@PathVariable Long lotId) {
        TransferLot lot = lots.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лот не найден"));
        if (!"DRAFT".equals(lot.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лот не ожидает подтверждения передачи");
        }
        LocalDate today = LocalDate.now();
        for (Product product : lot.getItems()) {
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
                    lot.getPartner() == null ? "" : lot.getPartner(), transferredAt));
            product.setStatus(ProductStatus.TRANSFERRED);
            product.setQuantityOnHand(0);
        });
        transferRecords.saveAll(snapshots);
        lot.setState("TRANSFERRED");
        lots.save(lot);
        return Map.of("id", lot.getId(), "state", lot.getState(), "itemCount", lot.getItems().size());
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