package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String studentNo;
    private String nickname;
    private String avatar;
    private String phone;
    private Long majorId;
    private String grade;
    private String role;
    @JsonIgnore
    private String password;

    private String gender;
    private String signature;

    /**
     * 手动输入的专业名，优先于 majorId 关联的专业。
     * updateStrategy=ALWAYS：MyBatis-Plus 默认跳过 null 字段，
     * 不加这个注解用户在资料页清空专业时永远删不掉。
     */
    @com.baomidou.mybatisplus.annotation.TableField(
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private String customMajor;

    /** 实名信息：注册时采集，只在管理端展示，学生端接口会置空 */
    private String realName;
    private String realPhone;

    private Integer creditScore;
    private Integer status;

    /** 临时封禁到期时间；status=0 时有效，null 表示永久封禁 */
    private LocalDateTime banUntil;
    private String banReason;

    /** 0 正常 1 禁言 */
    private Integer muteStatus;
    /** 禁言到期时间；null 表示永久禁言 */
    private LocalDateTime muteUntil;
    private String muteReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String majorName;

    /** 给前端直接用的状态文案，省得页面里再拼一遍 */
    @TableField(exist = false)
    private String statusText;
}
