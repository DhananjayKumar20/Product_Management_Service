package com.zestindia.dhananjay.ProductModule.service.Implementation;

import com.zestindia.dhananjay.ProductModule.config.JwtUtils;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.AuthResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.LoginRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.SignupRequestDTO;
import com.zestindia.dhananjay.ProductModule.entity.User;
import com.zestindia.dhananjay.ProductModule.entity.UserType;
import com.zestindia.dhananjay.ProductModule.exception.AuthenticationException;
import com.zestindia.dhananjay.ProductModule.repository.UserRepository;
import com.zestindia.dhananjay.ProductModule.service.Interface.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

//<<------------------------------------Signup---------------------------------------->>

    @Override
    public AuthResponseDTO signup(SignupRequestDTO request) {

        // Validate mandatory fields
        if (request == null) {
            throw new AuthenticationException("Request body cannot be null");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AuthenticationException("Email cannot be null or empty");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new AuthenticationException("Name cannot be null or empty");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AuthenticationException("Password cannot be null or empty");
        }

        if (request.getUserType() == null || request.getUserType().trim().isEmpty()) {
            throw new AuthenticationException("User type is required");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationException("User already exists with email: " + request.getEmail());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(UserType.valueOf(request.getUserType()));
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser.getId(),savedUser.getEmail());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(savedUser.getId());
        response.setName(savedUser.getName());
        response.setEmail(savedUser.getEmail());
        response.setToken(token);
        response.setMessage("Signup successful");
        return response;
    }

//<<------------------------------------Login---------------------------------------->>

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid  password");
        }

        String token = jwtUtils.generateToken(user.getId(),user.getEmail());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setToken(token);
        response.setMessage("Login successful");
        return response;
    }
}
