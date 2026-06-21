package com.example.demo.Config;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository userRepository,
            ProductRepository productRepository,
            BranchRepository branchRepository,
            CafeTableRepository cafeTableRepository,
            CouponRepository couponRepository,
            ProductVariantRepository productVariantRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Branch mainBranch = branchRepository.findByName("Lyhour Coffee — Main")
                    .orElseGet(() -> {
                        Branch branch = new Branch();
                        branch.setName("Lyhour Coffee — Main");
                        branch.setAddress("#390 Street Coffee, Phnom Penh, Cambodia");
                        branch.setPhone("+855 97 49 44 390");
                        branch.setActive(true);
                        return branchRepository.save(branch);
                    });

            if (userRepository.count() == 0) {
                createUser(userRepository, passwordEncoder, "admin", "admin@cafe.com", "admin123", "ADMIN");
                createUser(userRepository, passwordEncoder, "manager", "manager@cafe.com", "manager123", "MANAGER");
                createUser(userRepository, passwordEncoder, "cashier", "cashier@cafe.com", "cashier123", "CASHIER");
                createUser(userRepository, passwordEncoder, "kitchen", "kitchen@cafe.com", "kitchen123", "KITCHEN");
                createUser(userRepository, passwordEncoder, "walkin", "walkin@cafe.com", "walkin123", "CUSTOMER");
            }

            if (productRepository.count() == 0) {
                seedProducts(productRepository, productVariantRepository, mainBranch);
            }

            if (cafeTableRepository.count() == 0) {
                for (int i = 1; i <= 8; i++) {
                    CafeTable table = new CafeTable();
                    table.setTableNumber("T" + i);
                    table.setCapacity(i <= 4 ? 4 : 6);
                    table.setStatus(TableStatus.AVAILABLE);
                    table.setBranch(mainBranch);
                    cafeTableRepository.save(table);
                }
            }

            if (couponRepository.count() == 0) {
                Coupon welcome = new Coupon();
                welcome.setCode("WELCOME10");
                welcome.setType(CouponType.PERCENT);
                welcome.setValue(BigDecimal.TEN);
                welcome.setDescription("10% off your order");
                welcome.setValidFrom(LocalDate.now().minusDays(1));
                welcome.setValidUntil(LocalDate.now().plusMonths(6));
                welcome.setMaxUses(500);
                couponRepository.save(welcome);

                Coupon loyal = new Coupon();
                loyal.setCode("SAVE5");
                loyal.setType(CouponType.FIXED);
                loyal.setValue(BigDecimal.valueOf(5));
                loyal.setDescription("$5 off orders over $15");
                loyal.setValidFrom(LocalDate.now().minusDays(1));
                loyal.setValidUntil(LocalDate.now().plusMonths(3));
                loyal.setMaxUses(200);
                couponRepository.save(loyal);
            }
        };
    }

    private void createUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String email,
            String password,
            String role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreate_at(new Date(System.currentTimeMillis()));
        userRepository.save(user);
    }

    private void seedProducts(ProductRepository productRepository, ProductVariantRepository variantRepository, Branch branch) {
        Product latte = saveProduct(productRepository, "Latte", "Espresso with velvety steamed milk", 4.75, "Coffee",
                "https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=600&q=80", 80, branch, "COF-LAT", "885001", 1.50);
        seedVariants(variantRepository, latte);

        productRepository.save(product(productRepository, "Espresso", "Rich and bold single shot", 2.50, "Coffee",
                "https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=400", 80, branch, "COF-ESP", "885002", 0.80));
        productRepository.save(product(productRepository, "Cappuccino", "Espresso with steamed milk and foam", 4.50, "Coffee",
                "https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=400", 80, branch, "COF-CAP", "885003", 1.40));
        productRepository.save(product(productRepository, "Cheesecake", "Creamy New York style cheesecake", 5.00, "Cake",
                "https://images.unsplash.com/photo-1524351199678-941a58a3df50?w=400", 25, branch, "CAK-CHS", "885010", 2.00));
        productRepository.save(product(productRepository, "Croissant", "Buttery, flaky French pastry", 3.25, "Cake",
                "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=400", 40, branch, "CAK-CRO", "885011", 1.00));
        productRepository.save(product(productRepository, "Fresh Orange Juice", "Freshly squeezed orange juice", 3.50, "Drink",
                "https://images.unsplash.com/photo-1621506289935-a8e4df240304?w=400", 45, branch, "DRK-OJ", "885020", 1.20));
        productRepository.save(product(productRepository, "Matcha Latte", "Premium green tea with steamed milk", 5.00, "Drink",
                "https://images.unsplash.com/photo-1515823064-df6a192ed1d4?w=400", 35, branch, "DRK-MAT", "885021", 1.80));
    }

    private Product saveProduct(ProductRepository repo, String name, String desc, double price, String cat,
                                String img, int stock, Branch branch, String sku, String barcode, double cost) {
        return repo.save(product(repo, name, desc, price, cat, img, stock, branch, sku, barcode, cost));
    }

    private void seedVariants(ProductVariantRepository variantRepository, Product product) {
        variantRepository.save(variant(product, "Small", BigDecimal.valueOf(-0.50), false));
        variantRepository.save(variant(product, "Medium", BigDecimal.ZERO, true));
        variantRepository.save(variant(product, "Large", BigDecimal.valueOf(1.00), false));
    }

    private ProductVariant variant(Product product, String name, BigDecimal adj, boolean isDefault) {
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setName(name);
        v.setPriceAdjustment(adj);
        v.setDefault(isDefault);
        return v;
    }

    private Product product(ProductRepository repo, String name, String description, double price, String category,
                            String imageUrl, int stock, Branch branch, String sku, String barcode, double costPrice) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setStock(stock);
        product.setBranch(branch);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setCostPrice(costPrice);
        product.setAvailable(true);
        return product;
    }
}
