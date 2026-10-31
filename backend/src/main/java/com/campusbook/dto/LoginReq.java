package com.campusbook.dto;

import lombok.Data;

@Data
public class LoginReq {
    /** 微信登录 code；演示模式下传 demo-<openid> 或任意字符串即可 */
    private String code;
    private String nickname;
    private String avatar;
}
