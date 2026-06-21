package com.example.demo.Config;

import com.example.demo.Models.Branch;
import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Repositories.BranchRepository;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Date;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedDatabase(
            UserRepository userRepository,
            ProductRepository productRepository,
            BranchRepository branchRepository,
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
                createUser(userRepository, passwordEncoder, "walkin", "walkin@cafe.com", "walkin123", "CUSTOMER");
            }

            if (productRepository.count() == 0) {
                seedProducts(productRepository, mainBranch);
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

    private void seedProducts(ProductRepository productRepository, Branch branch) {
        productRepository.save(product("Espresso", "Rich and bold single shot of espresso", 2.50, "Coffee",
                "https://images.unsplash.com/photo-1510591509098-f4fdc6d0ff04?w=400", 80, branch));
        productRepository.save(product("Americano", "Espresso with hot water for a smooth taste", 3.00, "Coffee",
                "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=400", 80, branch));
        productRepository.save(product("Cappuccino", "Espresso with steamed milk and thick foam", 4.50, "Coffee",
                "https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=400", 80, branch));
        productRepository.save(product("Latte", "Espresso with velvety steamed milk", 4.75, "Coffee",
                "https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=600&q=80", 80, branch));
        productRepository.save(product("Mocha", "Espresso with chocolate and steamed milk", 5.25, "Coffee",
                "https://images.unsplash.com/photo-1578374173705-a57ea2b2351b?w=400", 60, branch));
        productRepository.save(product("Cold Brew", "Slow-steeped iced coffee, smooth and refreshing", 4.00, "Coffee",
                "https://images.unsplash.com/photo-1517701603779-8fc7be7e9f8b?w=400", 50, branch));
        productRepository.save(product("Iced Latte", "Chilled espresso with milk over ice", 4.75, "Coffee",
                "https://images.unsplash.com/photo-1517487881594-2787a5cdc0e5?w=400", 70, branch));
        productRepository.save(product("Caramel Macchiato", "Espresso with caramel and foamed milk", 5.50, "Coffee",
                "https://images.unsplash.com/photo-1461022055485-6c2f097ff8df?w=400", 55, branch));

        productRepository.save(product("Chocolate Cake", "Moist chocolate layer cake slice", 4.50, "Cake",
                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400", 30, branch));
        productRepository.save(product("Cheesecake", "Creamy New York style cheesecake", 5.00, "Cake",
                "https://images.unsplash.com/photo-1524351199678-941a58a3df50?w=400", 25, branch));
        productRepository.save(product("Croissant", "Buttery, flaky French pastry", 3.25, "Cake",
                "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=400", 40, branch));

        productRepository.save(product("Fresh Orange Juice", "Freshly squeezed orange juice", 3.50, "Drink",
                "https://images.unsplash.com/photo-1621506289935-a8e4df240304?w=400", 45, branch));
        productRepository.save(product("Matcha Latte", "Premium green tea with steamed milk", 5.00, "Drink",
                "https://images.unsplash.com/photo-1515823064-df6a192ed1d4?w=400", 35, branch));
        productRepository.save(product("Smoothie Bowl", "Mixed berry smoothie bowl", 6.50, "Drink",
                "https://images.unsplash.com/photo-1590301157890-4810ed352733?w=400", 20, branch));
    }

    private Product product(String name, String description, double price, String category, String imageUrl, int stock, Branch branch) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setStock(stock);
        product.setBranch(branch);
        return product;
    }
}
