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

    private final RateLimitFilter rateLimitFilter;

    private final TwoFactorAuthenticationFilter twoFactorAuthenticationFilter;



    public SecurityConfig(

            UserService userService,

            CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,

            JwtAuthenticationFilter jwtAuthenticationFilter,

            RateLimitFilter rateLimitFilter,

            TwoFactorAuthenticationFilter twoFactorAuthenticationFilter) {

        this.userService = userService;

        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        this.rateLimitFilter = rateLimitFilter;

        this.twoFactorAuthenticationFilter = twoFactorAuthenticationFilter;

    }



    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity

            .cors(cors -> {})

            .authenticationProvider(authenticationProvider())

            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

            .addFilterAfter(twoFactorAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/v1/auth/**").permitAll()

                .requestMatchers("/api/v1/mobile/config").permitAll()

                .requestMatchers("/api/v1/stripe/webhook", "/api/v1/payments/stripe/status").permitAll()

                .requestMatchers("/api/v1/products/**", "/api/v1/branches/**").permitAll()

                .requestMatchers("GET", "/api/v1/reservations").permitAll()

                .requestMatchers("POST", "/api/v1/reservations").permitAll()

                .requestMatchers("/admin/**", "/dashboard/**", "/products/**", "/users/**")
                        .hasAnyRole("ADMIN", "MANAGER", "CASHIER", "KITCHEN", "OWNER", "SUPER_ADMIN", "WAITER")

                .requestMatchers("/index", "/customer/**", "/order/success", "/api/v1/**").authenticated()

                .requestMatchers("/", "/home", "/signup", "/signin", "/signout", "/cart", "/order/checkout",
                        "/order/khqr-pay", "/reservations", "/qr/**", "/forgot-password", "/reset-password",
                        "/manifest.json", "/sw.js",
                        "/css/**", "/javascript/**", "/images/**", "/h2-console/**", "/ws/**").permitAll()

                .requestMatchers("/2fa", "/2fa/**", "/account/2fa/**").authenticated()

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


