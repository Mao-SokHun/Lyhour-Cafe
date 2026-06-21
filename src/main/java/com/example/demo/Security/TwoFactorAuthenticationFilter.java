package com.example.demo.Security;

import com.example.demo.Models.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class TwoFactorAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_VERIFIED = "TWO_FACTOR_VERIFIED";
    private static final Set<String> ALLOWED = Set.of(
            "/2fa", "/2fa/verify", "/signout", "/perform_logout", "/css/", "/javascript/", "/images/", "/ws/");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            if (user.isTwoFactorEnabled()) {
                HttpSession session = request.getSession(false);
                boolean verified = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_VERIFIED));
                String path = request.getRequestURI();
                if (!verified && ALLOWED.stream().noneMatch(path::startsWith)) {
                    response.sendRedirect("/2fa");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    public static void markVerified(HttpSession session) {
        session.setAttribute(SESSION_VERIFIED, true);
    }

    public static void clearVerified(HttpSession session) {
        if (session != null) session.removeAttribute(SESSION_VERIFIED);
    }
}
