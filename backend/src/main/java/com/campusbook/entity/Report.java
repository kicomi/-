package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 举报记录：书籍违规或卖家纠纷 */
@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String detail;
    private Integer status;
    private String handleNote;
    private Long handlerId;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    @TableField(exist = false)
    private String reporterNickname;
    @TableField(exist = false)
    private String targetTitle;
    @TableField(exist = false)
    private String statusText;
}
