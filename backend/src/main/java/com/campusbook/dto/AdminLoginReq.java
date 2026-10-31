package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginReq {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
