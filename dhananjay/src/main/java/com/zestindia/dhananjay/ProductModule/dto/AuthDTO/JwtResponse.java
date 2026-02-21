package com.zestindia.dhananjay.ProductModule.dto.AuthDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
}