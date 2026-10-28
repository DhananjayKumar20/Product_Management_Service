package com.ikaansh.dhananjay.TaskModule.config;

import com.ikaansh.dhananjay.TaskModule.exception.JwtValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test-secret-1234";

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expirationMs", 60_000L);
        jwtUtils.init();
    }

    @Test
    void generateAndParse_roundTrip() {
        String token = jwtUtils.generateToken(5L, "a@b.com");

        assertTrue(jwtUtils.validate(token));
        assertEquals("a@b.com", jwtUtils.getEmailFromJwt(token));
        assertEquals(5L, jwtUtils.getUserIdFromJwt(token));
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtUtils.generateToken(5L, "a@b.com");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThrows(JwtValidationException.class, () -> jwtUtils.parseToken(tampered));
    }

    @Test
    void malformedToken_isRejected() {
        assertThrows(JwtValidationException.class, () -> jwtUtils.parseToken("not-a-jwt"));
    }

    @Test
    void expiredToken_isRejected() {
        ReflectionTestUtils.setField(jwtUtils, "expirationMs", -1000L);
        String token = jwtUtils.generateToken(5L, "a@b.com");

        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> jwtUtils.parseToken(token));
        assertEquals("JWT expired", ex.getMessage());
    }

    @Test
    void tokenSignedWithOtherSecret_isRejected() {
        JwtUtils other = new JwtUtils();
        ReflectionTestUtils.setField(other, "secret", "another-secret-another-secret-another-secret-9876");
        ReflectionTestUtils.setField(other, "expirationMs", 60_000L);
        other.init();

        String foreign = other.generateToken(1L, "a@b.com");

        assertThrows(JwtValidationException.class, () -> jwtUtils.parseToken(foreign));
    }

    @Test
    void shortSecret_failsFast() {
        JwtUtils bad = new JwtUtils();
        ReflectionTestUtils.setField(bad, "secret", "short");

        assertThrows(IllegalArgumentException.class, bad::init);
    }
}
