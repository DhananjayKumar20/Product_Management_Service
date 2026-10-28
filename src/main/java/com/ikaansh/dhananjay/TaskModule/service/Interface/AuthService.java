package com.ikaansh.dhananjay.TaskModule.service.Interface;

import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.AuthResponseDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.LoginRequestDTO;
import com.ikaansh.dhananjay.TaskModule.dto.AuthDTO.SignupRequestDTO;

public interface AuthService {
    AuthResponseDTO signup(SignupRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);
}
