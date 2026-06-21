package com.example.demo.Security;

import com.example.demo.Models.CartItemDto;
import com.example.demo.Models.CheckoutRequest;
import com.example.demo.Models.PaymentMethod;
import com.example.demo.Models.User;
import com.example.demo.Service.CheckoutService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private CheckoutService checkoutService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession(false);

        if (session != null) {
            @SuppressWarnings("unchecked")
            List<CartItemDto> pendingCart = (List<CartItemDto>) session.getAttribute("pendingCart");

            if (pendingCart != null && !pendingCart.isEmpty()) {
                User loggedInUser = (User) authentication.getPrincipal();
                PaymentMethod method = PaymentMethod.PAY_AT_PICKUP;
                Object pendingMethod = session.getAttribute("pendingPaymentMethod");
                if (pendingMethod instanceof PaymentMethod pm) {
                    method = pm;
                }
                try {
                    CheckoutRequest checkoutRequest = new CheckoutRequest();
                    checkoutRequest.setItems(pendingCart);
                    checkoutRequest.setPaymentMethod(method);
                    CheckoutService.CheckoutResult result = checkoutService.checkout(loggedInUser, checkoutRequest);
                    session.removeAttribute("pendingCart");
                    session.removeAttribute("pendingPaymentMethod");
                    response.sendRedirect(result.redirectUrl());
                    return;
                } catch (Exception e) {
                    System.err.println("Error processing pending cart after login: " + e.getMessage());
                }
            }
        }

        var roles = authentication.getAuthorities();
        if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN") || r.getAuthority().equals("ROLE_MANAGER")
                || r.getAuthority().equals("ROLE_OWNER") || r.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            response.sendRedirect("/dashboard");
        } else if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_KITCHEN"))) {
            response.sendRedirect("/admin/kitchen");
        } else if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_CASHIER") || r.getAuthority().equals("ROLE_WAITER"))) {
            response.sendRedirect("/admin/pos");
        } else if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_CUSTOMER") || r.getAuthority().equals("ROLE_USER"))) {
            response.sendRedirect("/index");
        } else {
            response.sendRedirect("/home");
        }
    }
}