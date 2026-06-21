package com.example.demo.Models;

import java.math.BigDecimal;

public class CartItemDto {
    private Long productId;
    private int quantity;
    private String size;
    private String note;
    private BigDecimal price; // price in one product

    // Getters and Setters
    public Long getProductId() { return productId; }

    public void setProductId(Long productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSize() { return size; }

    public void setSize(String size) { this.size = size; }

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }

    public BigDecimal getPrice() { return price; }

    public void setPrice(BigDecimal price) { this.price = price; }
}