package com.zestindia.dhananjay.ProductModule.dto.AuthDTO;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
}