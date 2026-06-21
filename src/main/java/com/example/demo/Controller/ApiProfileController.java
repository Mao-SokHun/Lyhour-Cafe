package com.example.demo.Controller;

import com.example.demo.Models.User;
import com.example.demo.dto.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ApiProfileController {

    @GetMapping
    public ResponseEntity<UserProfileResponse> profile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole()));
    }
}
