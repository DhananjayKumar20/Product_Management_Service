package com.zestindia.dhananjay.ProductModule.dto.AuthDTO;

import lombok.Data;

@Data
public class SignupRequestDTO {
    private String email;
    private String name;
    private String password;
    private String userType; // ADMIN / APPLICANT
}