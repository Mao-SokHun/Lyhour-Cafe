package com.example.demo;

import com.example.demo.Models.User;
import com.example.demo.Repositories.OrderRepository;
import com.example.demo.Repositories.UserRepository;
import com.example.demo.Security.JwtService;
import com.example.demo.Service.AppSettingService;
import com.example.demo.Service.NotificationService;
import com.example.demo.Service.TelegramService;
import com.example.demo.dto.TelegramAuthRequest;
import com.example.demo.dto.TelegramAuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppSettingService appSettingService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        telegramService = new TelegramService(
                userRepository,
                orderRepository,
                jwtService,
                notificationService,
                appSettingService,
                passwordEncoder,
                objectMapper
        );
    }

    @Test
    void verifyInitDataInDemoModeAllowsValidation() {
        when(appSettingService.get(eq("telegram.bot_token"), anyString())).thenReturn("");
        boolean valid = telegramService.verifyInitData("auth_date=1620000000&hash=dummy&user=%7B%22id%22%3A12345%7D");
        assertTrue(valid, "Should allow in demo/dev mode when bot token is empty");
    }

    @Test
    void authenticateTelegramUserCreatesNewUserWhenNotExists() {
        when(appSettingService.get(eq("telegram.bot_token"), anyString())).thenReturn("");
        when(userRepository.findByTelegramUserId(12345L)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        User savedUser = new User();
        savedUser.setId(99L);
        savedUser.setUsername("tg_12345");
        savedUser.setRole("CUSTOMER");
        savedUser.setTelegramUserId(12345L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        TelegramAuthRequest req = new TelegramAuthRequest(
                null,
                12345L,
                "tg_tester",
                "Sokha",
                "Mao",
                null
        );

        TelegramAuthResponse resp = telegramService.authenticateTelegramUser(req);

        assertNotNull(resp);
        assertEquals("mock-jwt-token", resp.token());
        assertEquals(12345L, resp.user().telegramUserId());
        assertEquals("Sokha", resp.user().firstName());
        verify(userRepository).save(any(User.class));
    }
}
