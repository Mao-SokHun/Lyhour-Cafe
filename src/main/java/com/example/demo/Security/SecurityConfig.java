package com.example.demo.Security;



import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;



import com.example.demo.Service.UserService;



@Configuration

@EnableWebSecurity

@EnableMethodSecurity

public class SecurityConfig {



    private final UserService userService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;



    public SecurityConfig(

            UserService userService,

            CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,

            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.userService = userService;

        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

    }



    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity

            .cors(cors -> {})

            .authenticationProvider(authenticationProvider())

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/v1/auth/**").permitAll()

                .requestMatchers("/api/v1/stripe/webhook", "/api/v1/payments/stripe/status").permitAll()

                .requestMatchers("/api/v1/products/**", "/api/v1/branches/**").permitAll()

                .requestMatchers("GET", "/api/v1/reservations").permitAll()

                .requestMatchers("POST", "/api/v1/reservations").permitAll()

                .requestMatchers("/admin/**", "/dashboard/**", "/products/**", "/users/**").hasAnyRole("ADMIN", "MANAGER")

                .requestMatchers("/index", "/customer/**", "/order/success", "/api/v1/**").authenticated()

                .requestMatchers("/", "/home", "/signup", "/signin", "/signout", "/cart", "/order/checkout",

                        "/reservations", "/css/**", "/javascript/**", "/images/**", "/h2-console/**").permitAll()

                .anyRequest().authenticated()

            )

            .sessionManagement(session -> session

                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            .formLogin(form -> form

                .loginPage("/signin")

                .loginProcessingUrl("/signin")

                .successHandler(customAuthenticationSuccessHandler)

                .failureUrl("/signin?error=true")

                .permitAll()

            )

            .logout(logout -> logout

                .logoutRequestMatcher(new AntPathRequestMatcher("/perform_logout"))

                .logoutSuccessUrl("/signin?logout")

                .invalidateHttpSession(true)

                .deleteCookies("JSESSIONID")

                .permitAll()

            )

            .csrf(csrf -> csrf.ignoringRequestMatchers(

                    "/order/checkout",

                    "/admin/pos/checkout",

                    "/api/**"))

            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));



        return httpSecurity.build();

    }



    @Bean

    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();

    }



    @Bean

    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userService);

        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;

    }



    @Bean

    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

}


