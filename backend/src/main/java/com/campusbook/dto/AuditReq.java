package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuditReq {
    /** approve | reject | offline */
    @NotBlank
    private String action;
    private String reason;
}
