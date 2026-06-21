package com.example.demo.Service;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final IngredientInventoryService ingredientInventoryService;

    public SupplierService(
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            IngredientInventoryService ingredientInventoryService) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.ingredientInventoryService = ingredientInventoryService;
    }

    public List<Supplier> findAll() { return supplierRepository.findAll(); }

    @Transactional
    public Supplier save(Supplier supplier) { return supplierRepository.save(supplier); }

    public List<PurchaseOrder> findAllOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional
    public PurchaseOrder createOrder(Long supplierId, List<PurchaseOrderItem> items, String invoiceNumber) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setInvoiceNumber(invoiceNumber);
        po.setStatus(PurchaseOrderStatus.ORDERED);
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            item.setPurchaseOrder(po);
            po.getItems().add(item);
            total = total.add(item.getLineTotal());
        }
        po.setTotalAmount(total);
        supplier.setOutstandingBalance(supplier.getOutstandingBalance().add(total));
        supplierRepository.save(supplier);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder receiveOrder(Long orderId) {
        PurchaseOrder po = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            return po;
        }
        for (PurchaseOrderItem item : po.getItems()) {
            ingredientInventoryService.adjustStock(
                    item.getIngredient().getId(),
                    item.getQuantity(),
                    InventoryTransactionType.PURCHASE,
                    "PO #" + po.getId());
        }
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setReceivedDate(LocalDate.now());
        return purchaseOrderRepository.save(po);
    }
}
