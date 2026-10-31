package com.campusbook.dto;

import com.campusbook.entity.Book;
import lombok.Data;

@Data
public class RecommendItem {
    private Book book;
    private Double score;
    private String reason;
}
