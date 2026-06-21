// package com.example.demo.Security;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.authentication.AuthenticationProvider;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;

// import com.example.demo.Service.UserService;

// @Configuration
// @EnableWebSecurity
// public class config {

//     @Autowired
//     private UserService userService;

//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//         httpSecurity
//             .authorizeHttpRequests(
//                 auth -> auth
//                 .requestMatchers("/signup", "/signin", "/css/**", "/js/**", "/images/**").permitAll()
//                 .anyRequest().authenticated()
//             )
//             .formLogin(
//                 httpform -> httpform
//                 .loginPage("/signin")
//                 .defaultSuccessUrl("/index", true)
//                 .permitAll()
//             )
//             .logout(
//                 logout -> logout
//                 .logoutUrl("/logout")
//                 .logoutSuccessUrl("/signin?logout")
//                 .permitAll()
//             );
//         return httpSecurity.build();
//     }

//     @Bean
//     public PasswordEncoder passwordEncoder(){
//         return new BCryptPasswordEncoder();

//     }
    
//     @Bean
//     public AuthenticationProvider authenticationProvider(){
//         DaoAuthenticationProvider provider=new DaoAuthenticationProvider();
//         provider.setUserDetailsService(userService);
//         provider.setPasswordEncoder(passwordEncoder());
//         return provider;

//     }

// }
