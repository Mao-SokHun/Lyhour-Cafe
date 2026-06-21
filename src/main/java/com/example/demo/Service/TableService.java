package com.example.demo.Service;

import com.example.demo.Models.CafeTable;
import com.example.demo.Models.Order;
import com.example.demo.Models.TableStatus;
import com.example.demo.Repositories.CafeTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TableService {

    private final CafeTableRepository cafeTableRepository;
    private final AuditLogService auditLogService;

    public TableService(CafeTableRepository cafeTableRepository, AuditLogService auditLogService) {
        this.cafeTableRepository = cafeTableRepository;
        this.auditLogService = auditLogService;
    }

    public List<CafeTable> findAll() {
        return cafeTableRepository.findAllByOrderByTableNumberAsc();
    }

    public CafeTable findById(Long id) {
        return cafeTableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));
    }

    @Transactional
    public CafeTable save(CafeTable table) {
        if (table.getQrCode() == null || table.getQrCode().isBlank()) {
            String num = table.getTableNumber() != null
                    ? table.getTableNumber().replaceAll("\\D", "")
                    : "0";
            table.setQrCode("T" + num + "-LYHOUR");
        }
        CafeTable saved = cafeTableRepository.save(table);
        auditLogService.log("TABLE_SAVE", "Table " + saved.getTableNumber());
        return saved;
    }

    @Transactional
    public void assignToOrder(Long tableId, Order order) {
        if (tableId == null) {
            return;
        }
        CafeTable table = findById(tableId);
        if (table.getStatus() == TableStatus.RESERVED) {
            // allow order on reserved table
        } else if (table.getStatus() != TableStatus.AVAILABLE) {
            throw new IllegalStateException("Table " + table.getTableNumber() + " is not available");
        }
        table.setStatus(TableStatus.OCCUPIED);
        order.setCafeTable(table);
        cafeTableRepository.save(table);
    }

    @Transactional
    public void releaseTable(Order order) {
        if (order.getCafeTable() == null) {
            return;
        }
        CafeTable table = order.getCafeTable();
        table.setStatus(TableStatus.AVAILABLE);
        cafeTableRepository.save(table);
    }

    @Transactional
    public void updateStatus(Long tableId, TableStatus status) {
        CafeTable table = findById(tableId);
        table.setStatus(status);
        cafeTableRepository.save(table);
        auditLogService.log("TABLE_STATUS", table.getTableNumber() + " → " + status);
    }

    public long countAvailable() {
        return cafeTableRepository.countByStatus(TableStatus.AVAILABLE);
    }

    public CafeTable findByQrCode(String qrCode) {
        return cafeTableRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid table QR code"));
    }

    @Transactional
    public void mergeTables(Long targetId, Long sourceId) {
        CafeTable target = findById(targetId);
        CafeTable source = findById(sourceId);
        source.setMergedInto(target);
        source.setStatus(TableStatus.OCCUPIED);
        target.setCapacity(target.getCapacity() + source.getCapacity());
        cafeTableRepository.save(source);
        cafeTableRepository.save(target);
        auditLogService.log("TABLE_MERGE", source.getTableNumber() + " → " + target.getTableNumber());
    }

    @Transactional
    public void splitTable(Long tableId) {
        CafeTable table = findById(tableId);
        table.setMergedInto(null);
        table.setStatus(TableStatus.AVAILABLE);
        cafeTableRepository.save(table);
        auditLogService.log("TABLE_SPLIT", table.getTableNumber());
    }

    @Transactional
    public void transferTable(Long fromId, Long toId) {
        CafeTable from = findById(fromId);
        CafeTable to = findById(toId);
        if (to.getStatus() != TableStatus.AVAILABLE) {
            throw new IllegalStateException("Target table not available");
        }
        from.setStatus(TableStatus.AVAILABLE);
        to.setStatus(TableStatus.OCCUPIED);
        cafeTableRepository.save(from);
        cafeTableRepository.save(to);
        auditLogService.log("TABLE_TRANSFER", from.getTableNumber() + " → " + to.getTableNumber());
    }
}
