package com.example.demo.Controller;

import java.sql.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.Models.Product;
import com.example.demo.Models.User;
import com.example.demo.Repositories.ProductRepository;
import com.example.demo.Repositories.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/")
public class Homecontroller {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ProductRepository productRepository;


    // @GetMapping("dashboard")
    // public String getindex() {
    //     return "dashboard";
    // }

    @GetMapping("signup")
    public String getsignup() {
        return "signup";
    }
    @GetMapping("signin")
    public String getsignin() {
        return "signin";
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().toString().equals("anonymousUser")) {
            return "redirect:/index";
        }
        return "redirect:/home";
    }

    @GetMapping("/index")
    public String showIndex(Model model) {
        List<Product> products = productRepository.findAll(); 
        model.addAttribute("products", products);          
        return "index";                                     
    }

    @GetMapping("/home")
    public String showHome(Model model) {
        List<Product> products = productRepository.findAll();
        model.addAttribute("products", products);
        return "home";
    }

    @PostMapping("/signup")
    public String postSignup(
        @RequestParam("username") String username,
        @RequestParam("email") String email,
        @RequestParam("password") String password,
        @RequestParam(name = "role", required = false) String role // Role is no longer used from form
        // @RequestParam("create_at") String create_at
        ) {
        
        User new_user = new User();
        new_user.setUsername(username);
        new_user.setEmail(email);
        new_user.setPassword(passwordEncoder.encode(password));
        new_user.setRole("CUSTOMER"); // role user : ADMIN, MANAGER, CUSTOMER 
        new_user.setCreate_at(new Date(System.currentTimeMillis()));
        userRepository.save(new_user);

        return "redirect:/signin";
    }

    @GetMapping("/signout")
    public String getsignout(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrManager = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN") || r.getAuthority().equals("ROLE_MANAGER"));
        boolean isUser = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_USER") || r.getAuthority().equals("ROLE_CUSTOMER"));

        if (isAdminOrManager) {
            model.addAttribute("backUrl", "/dashboard");
        } else if (isUser) {
            model.addAttribute("backUrl", "/index");
        } else {
            model.addAttribute("backUrl", "/home"); 
        }
        return "signout";
    }
    @PostMapping("/signout")
    public String handleSignOut(HttpSession session) {
        session.invalidate(); 
        return "redirect:/signin"; 
    }
    // @Bean
    // public PasswordEncoder passwordEncoder(){
    //     return new BCryptPasswordEncoder();

    // }
    
    // @Bean
    // public AuthenticationProvider authenticationProvider(){
    //     DaoAuthenticationProvider provider=new DaoAuthenticationProvider();
    //     provider.setUserDetailsService(null);


    // }
}
