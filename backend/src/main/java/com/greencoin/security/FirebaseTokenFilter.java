package com.greencoin.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        log.info("Incoming request to: {} | Authorization header present: {}",
                request.getRequestURI(), (header != null));

        if (header != null && header.toLowerCase().startsWith("bearer ")) {
            String idToken = header.substring(7).trim();
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail();

                if (email == null) {
                    email = (String) decodedToken.getClaims().get("email");
                }

                log.info("Authenticated Firebase user: {}", email);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        uid, email, new ArrayList<>());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("Firebase token verification failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else if (header != null) {
            log.warn("Invalid Authorization header format. Expected 'Bearer <token>'");
        }

        filterChain.doFilter(request, response);
    }
}
