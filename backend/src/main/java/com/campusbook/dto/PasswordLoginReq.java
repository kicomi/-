package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginReq {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
