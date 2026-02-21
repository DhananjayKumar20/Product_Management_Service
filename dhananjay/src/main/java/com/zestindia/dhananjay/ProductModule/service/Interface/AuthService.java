package com.zestindia.dhananjay.ProductModule.service.Interface;

import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.AuthResponseDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.LoginRequestDTO;
import com.zestindia.dhananjay.ProductModule.dto.AuthDTO.SignupRequestDTO;
import org.jspecify.annotations.Nullable;

public interface AuthService {
    @Nullable AuthResponseDTO signup(SignupRequestDTO request);

    @Nullable AuthResponseDTO login(LoginRequestDTO request);
}
