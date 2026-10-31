package com.campusbook.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> list) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.page = page;
        p.size = size;
        p.list = list;
        return p;
    }
}
