package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    /**
     * 学生端取广告：只返回投放中、且在投放时间窗内的记录。
     */
    @Select("""
            SELECT * FROM banner
             WHERE position = #{position}
               AND status = 1
               AND (start_time IS NULL OR start_time <= NOW())
               AND (end_time   IS NULL OR end_time   >= NOW())
             ORDER BY sort ASC, id DESC
             LIMIT #{limit}
            """)
    List<Banner> selectActive(@Param("position") String position, @Param("limit") int limit);
}
