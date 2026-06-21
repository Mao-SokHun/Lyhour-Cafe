package com.example.demo.Service;

import com.example.demo.Models.User;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Security.JwtService;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse login(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        User user = userRepository.findByUsername(username);
        return toAuthResponse(user);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()) != null) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.findByEmail(request.email()) != null) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("CUSTOMER");
        user.setCreate_at(new Date(System.currentTimeMillis()));
        userRepository.save(user);

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole());
    }
}
