package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterReq {
    /** 手机号或学号，作为登录账号 */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 实名信息，仅管理端可见 */
    private String realName;
    private String studentNo;

    private Long majorId;
    /** 手动输入的专业名，优先于 majorId */
    private String customMajor;
    private String grade;
    private String gender;
}
