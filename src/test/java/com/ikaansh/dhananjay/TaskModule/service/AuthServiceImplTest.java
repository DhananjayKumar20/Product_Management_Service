package com.ikaansh.dhananjay.TaskModule.service;

import com.ikaansh.dhananjay.TaskModule.config.JwtUtils;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.AuthResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.LoginRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.SignupRequestDTO;
import com.ikaansh.dhananjay.TaskModule.entity.User;
import com.ikaansh.dhananjay.TaskModule.entity.UserType;
import com.ikaansh.dhananjay.TaskModule.exception.AuthenticationException;
import com.ikaansh.dhananjay.TaskModule.exception.ForbiddenOperationException;
import com.ikaansh.dhananjay.TaskModule.exception.InvalidCredentialsException;
import com.ikaansh.dhananjay.TaskModule.exception.UserAlreadyExistsException;
import com.ikaansh.dhananjay.TaskModule.repository.UserRepository;
import com.ikaansh.dhananjay.TaskModule.service.Implementation.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtils, true);
    }

    private SignupRequestDTO signupRequest(String email, String type) {
        SignupRequestDTO req = new SignupRequestDTO();
        req.setEmail(email);
        req.setName("Dhananjay");
        req.setPassword("Password@123");
        req.setUserType(type);
        return req;
    }

    private User savedUser(Long id, String email, UserType type, String encodedPassword) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setName("Dhananjay");
        u.setUserType(type);
        u.setPassword(encodedPassword);
        return u;
    }

    // ---------------- signup ----------------

    @Test
    void signup_success_hashesPasswordAndNormalizesEmail() {
        when(userRepository.existsByEmail("dhananjay@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtUtils.generateToken(7L, "dhananjay@example.com")).thenReturn("jwt-token");

        AuthResponseDTO response = authService.signup(signupRequest("  Dhananjay@Example.com ", "user"));

        assertEquals(7L, response.getId());
        assertEquals("dhananjay@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Signup successful", response.getMessage());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("HASHED", captor.getValue().getPassword());
        assertEquals(UserType.USER, captor.getValue().getUserType());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void signup_existingEmail_throwsUserAlreadyExists() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.signup(signupRequest("a@b.com", "USER")));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void signup_parallelDoubleClick_uniqueIndexViolation_throwsUserAlreadyExists() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.signup(signupRequest("a@b.com", "USER")));

        verify(jwtUtils, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void signup_invalidUserType_throwsAuthenticationException() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.signup(signupRequest("a@b.com", "SUPERUSER")));

        assertTrue(ex.getMessage().contains("USER, ADMIN"));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void signup_blankName_throwsAuthenticationException() {
        SignupRequestDTO req = signupRequest("a@b.com", "USER");
        req.setName("  ");

        assertThrows(AuthenticationException.class, () -> authService.signup(req));
    }

    @Test
    void signup_nullRequest_throwsAuthenticationException() {
        assertThrows(AuthenticationException.class, () -> authService.signup(null));
    }

    @Test
    void signup_adminDisabled_throwsForbidden() {
        AuthServiceImpl restricted = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtils, false);

        assertThrows(ForbiddenOperationException.class,
                () -> restricted.signup(signupRequest("admin@b.com", "ADMIN")));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void signup_adminAllowed_createsAdmin() {
        when(userRepository.existsByEmail("admin@b.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtils.generateToken(1L, "admin@b.com")).thenReturn("t");

        authService.signup(signupRequest("admin@b.com", "ADMIN"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals(UserType.ADMIN, captor.getValue().getUserType());
    }

    // ---------------- login ----------------

    @Test
    void login_success_returnsToken() {
        User user = savedUser(3L, "a@b.com", UserType.USER, "HASHED");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "HASHED")).thenReturn(true);
        when(jwtUtils.generateToken(3L, "a@b.com")).thenReturn("jwt-token");

        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("A@B.com");
        req.setPassword("Password@123");

        AuthResponseDTO response = authService.login(req);

        assertEquals(3L, response.getId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("x@b.com")).thenReturn(Optional.empty());

        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("x@b.com");
        req.setPassword("whatever1");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throwsSameMessageAsUnknownEmail() {
        User user = savedUser(3L, "a@b.com", UserType.USER, "HASHED");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "HASHED")).thenReturn(false);

        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("a@b.com");
        req.setPassword("wrong-pass");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(jwtUtils, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void login_missingFields_throwsAuthenticationException() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("a@b.com");

        assertThrows(AuthenticationException.class, () -> authService.login(req));
    }
}
