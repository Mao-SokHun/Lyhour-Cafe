package com.example.demo.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column
    private String size;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtTimeOfOrder; 

    @PrePersist
    public void prePersist() {
        if (this.product != null && this.name_product == null) {
            this.name_product = product.getName();
        }
    }

    @Column(nullable = false)
    private String name_product;

    public String getName_product() {
        return name_product;
    }

    public void setName_product(String name_product) {
        this.name_product = name_product;
    }
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }

    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }

    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSize() { return size; }

    public void setSize(String size) { this.size = size; }

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }

    public BigDecimal getPriceAtTimeOfOrder() { return priceAtTimeOfOrder; }

    public void setPriceAtTimeOfOrder(BigDecimal price) { this.priceAtTimeOfOrder = price; }
}