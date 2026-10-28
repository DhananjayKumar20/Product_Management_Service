package com.ikaansh.dhananjay.TaskModule.controller;

import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.AuthResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.LoginRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.SignupRequestDTO;
import com.ikaansh.dhananjay.TaskModule.service.Interface.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Signup and login (public)")
public class AuthController {

    private final AuthService userService;

//<<------------------------------------Signup---------------------------------------->>

    @Operation(summary = "Register a new user (USER or ADMIN)")
    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDTO> signup(@Valid @RequestBody SignupRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(request));
    }

//<<------------------------------------Login----------------------------------------->>

    @Operation(summary = "Login and receive a JWT")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
