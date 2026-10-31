package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusbook.entity.BookOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookOrderMapper extends BaseMapper<BookOrder> {

    String BASE_SELECT = """
            SELECT o.*,
                   b.title       AS book_title,
                   b.cover_img   AS book_cover,
                   bu.nickname   AS buyer_nickname,
                   su.nickname   AS seller_nickname
              FROM book_order o
              LEFT JOIN book b     ON o.book_id   = b.id
              LEFT JOIN sys_user bu ON o.buyer_id  = bu.id
              LEFT JOIN sys_user su ON o.seller_id = su.id
            """;

    @Select("""
            <script>
            """ + BASE_SELECT + """
            <where>
              <if test="buyerId != null">  AND o.buyer_id  = #{buyerId}  </if>
              <if test="sellerId != null"> AND o.seller_id = #{sellerId} </if>
              <if test="status != null">   AND o.status    = #{status}   </if>
            </where>
            ORDER BY o.created_at DESC
            </script>
            """)
    IPage<BookOrder> pageOrders(IPage<BookOrder> page,
                                @Param("buyerId") Long buyerId,
                                @Param("sellerId") Long sellerId,
                                @Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM book_order WHERE status = #{status}")
    long countByStatus(@Param("status") int status);

    @Select("SELECT COUNT(*) FROM book_order")
    long countAll();

    @Select("""
            SELECT DATE_FORMAT(created_at, '%m-%d') AS name, COUNT(*) AS cnt
              FROM book_order
             WHERE created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
             GROUP BY name ORDER BY MIN(created_at)
            """)
    List<Map<String, Object>> statDailyOrders();

    @Select("SELECT IFNULL(SUM(price),0) FROM book_order WHERE status = 2")
    java.math.BigDecimal sumCompletedAmount();
}
