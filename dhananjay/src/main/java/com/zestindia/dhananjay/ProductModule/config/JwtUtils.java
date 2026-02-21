package com.zestindia.dhananjay.ProductModule.config;


import com.zestindia.dhananjay.ProductModule.exception.JwtValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private Key key;

    @Value("${jwt.secret}")
    private String secret;

    // Default expiration: 8 hours in milliseconds
    @Value("${jwt.expiration-ms:28800000}")
    private long expirationMs;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("jwt.secret must be >= 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    // Generate JWT token
    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key)
                .compact();
    }
    // Extract email from JWT
    public String getEmailFromJwt(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (JwtException e) {
            throw new JwtValidationException("Failed to parse JWT: " + e.getMessage());
        }
    }

    // Extract userId from JWT
    public Long getUserIdFromJwt(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("userId", Long.class);

        } catch (JwtException e) {
            throw new JwtValidationException("Failed to extract userId from JWT: " + e.getMessage());
        }
    }

    // Extract JWT token from current request
    public String extractTokenFromRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // Extract userId from current request
    public Long extractUserId() {
        String token = extractTokenFromRequest();
        if (token == null) throw new JwtValidationException("JWT token missing in request");
        return getUserIdFromJwt(token);
    }

    // Validate JWT token
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtValidationException("JWT expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new JwtValidationException("Unsupported JWT: " + e.getMessage());
        } catch (MalformedJwtException e) {
            throw new JwtValidationException("Malformed JWT: " + e.getMessage());
        } catch (SignatureException e) {
            throw new JwtValidationException("Invalid JWT signature: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("JWT claims string is empty: " + e.getMessage());
        }
    }
}
