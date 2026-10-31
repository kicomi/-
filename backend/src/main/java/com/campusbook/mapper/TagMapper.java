package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.name FROM tag t JOIN book_tag bt ON t.id = bt.tag_id WHERE bt.book_id = #{bookId}")
    List<String> selectNamesByBook(@Param("bookId") Long bookId);

    @Select("SELECT bt.book_id AS bookId, t.name AS name FROM tag t " +
            "JOIN book_tag bt ON t.id = bt.tag_id WHERE bt.book_id IN " +
            "<foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach>")
    List<java.util.Map<String, Object>> selectNamesByBooks(@Param("ids") List<Long> ids);
}
