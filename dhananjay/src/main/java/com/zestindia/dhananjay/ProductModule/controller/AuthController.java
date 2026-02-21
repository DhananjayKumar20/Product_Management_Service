package com.zestindia.dhananjay.ProductModule.controller;

import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.AuthResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.LoginRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.SignupRequestDTO;
import com.zestindia.dhananjay.ProductModule.service.Interface.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService userService;

//<<------------------------------------Signup---------------------------------------->>

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDTO> signup(@RequestBody SignupRequestDTO request) {
        return ResponseEntity.ok(userService.signup(request));
    }

//<<------------------------------------Login----------------------------------------->>

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }
}
