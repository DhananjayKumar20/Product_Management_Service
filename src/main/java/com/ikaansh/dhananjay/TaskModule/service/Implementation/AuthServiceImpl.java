package com.ikaansh.dhananjay.TaskModule.service.Implementation;

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
import com.ikaansh.dhananjay.TaskModule.service.Interface.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final boolean allowAdminSignup;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           @Value("${app.security.allow-admin-signup:true}") boolean allowAdminSignup) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.allowAdminSignup = allowAdminSignup;
    }

//<<------------------------------------Signup---------------------------------------->>

    @Override
    @Transactional
    public AuthResponseDTO signup(SignupRequestDTO request) {

        if (request == null) {
            throw new AuthenticationException("Request body cannot be null");
        }
        if (isBlank(request.getEmail())) {
            throw new AuthenticationException("Email cannot be null or empty");
        }
        if (isBlank(request.getName())) {
            throw new AuthenticationException("Name cannot be null or empty");
        }
        if (isBlank(request.getPassword())) {
            throw new AuthenticationException("Password cannot be null or empty");
        }
        if (isBlank(request.getUserType())) {
            throw new AuthenticationException("User type is required");
        }

        String email = normalizeEmail(request.getEmail());
        UserType userType = parseUserType(request.getUserType());

        if (userType == UserType.ADMIN && !allowAdminSignup) {
            throw new ForbiddenOperationException("ADMIN signup is disabled");
        }

        // Fast path - cheap exists query instead of loading the whole row
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already exists with email: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(userType);
        user.setCreatedAt(Instant.now());

        User savedUser;
        try {
            // flush now so a double click / parallel signup hits the unique index right here
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate signup detected for {}", email);
            throw new UserAlreadyExistsException("User already exists with email: " + email);
        }

        String token = jwtUtils.generateToken(savedUser.getId(), savedUser.getEmail());
        return buildResponse(savedUser, token, "Signup successful");
    }

//<<------------------------------------Login---------------------------------------->>

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {

        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new AuthenticationException("Email and password are required");
        }

        // Same message for unknown email and wrong password - do not reveal which accounts exist
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getEmail());
        return buildResponse(user, token, "Login successful");
    }

    private AuthResponseDTO buildResponse(User user, String token, String message) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setToken(token);
        response.setMessage(message);
        return response;
    }

    private UserType parseUserType(String raw) {
        try {
            return UserType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid user type '" + raw + "'. Allowed values: USER, ADMIN");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
