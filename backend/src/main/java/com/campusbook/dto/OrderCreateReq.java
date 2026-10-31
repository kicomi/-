package com.campusbook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderCreateReq {
    @NotNull
    private Long bookId;
    private String pickupPlace;
    private LocalDateTime pickupTime;
    private String remark;
}
