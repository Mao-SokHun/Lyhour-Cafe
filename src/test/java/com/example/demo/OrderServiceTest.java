package com.example.demo;

import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Models.CartItemDto;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createOrderDecreasesStockAndMarksPayment() {
        User customer = userRepository.findByUsername("walkin");
        assertNotNull(customer);

        Product product = productRepository.findAll().getFirst();
        int initialStock = product.getStock();

        CartItemDto item = new CartItemDto();
        item.setProductId(product.getId());
        item.setQuantity(1);
        item.setSize("Medium");
        item.setPrice(BigDecimal.valueOf(product.getPrice()));

        var order = orderService.createOrder(customer, List.of(item),
                com.example.demo.Models.OrderSource.ONLINE, PaymentMethod.ONLINE_MOCK);

        assertNotNull(order.getId());
        assertEquals(PaymentMethod.ONLINE_MOCK, order.getPaymentMethod());
        assertEquals(com.example.demo.Models.PaymentStatus.PAID, order.getPaymentStatus());
        assertNotNull(order.getPaymentReference());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(initialStock - 1, updated.getStock());
    }

    @Test
    void createOrderFailsWhenInsufficientStock() {
        User customer = userRepository.findByUsername("walkin");
        Product product = productRepository.findAll().getFirst();
        product.setStock(0);
        productRepository.save(product);

        CartItemDto item = new CartItemDto();
        item.setProductId(product.getId());
        item.setQuantity(1);
        item.setPrice(BigDecimal.valueOf(product.getPrice()));

        assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(customer, List.of(item)));
    }
}
