package com.example.demo.Service;

import com.example.demo.Models.CustomerFavorite;
import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Repositories.CustomerFavoriteRepository;
import com.example.demo.Repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerFavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public CustomerService(CustomerFavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    public List<CustomerFavorite> getFavorites(User customer) {
        return favoriteRepository.findByCustomerId(customer.getId());
    }

    @Transactional
    public void toggleFavorite(User customer, Long productId) {
        if (favoriteRepository.existsByCustomerIdAndProductId(customer.getId(), productId)) {
            favoriteRepository.deleteByCustomerIdAndProductId(customer.getId(), productId);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            CustomerFavorite fav = new CustomerFavorite();
            fav.setCustomer(customer);
            fav.setProduct(product);
            favoriteRepository.save(fav);
        }
    }
}
