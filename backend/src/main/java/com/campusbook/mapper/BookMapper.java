package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusbook.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    String BASE_SELECT = """
            SELECT b.*,
                   u.nickname AS seller_nickname,
                   u.avatar   AS seller_avatar,
                   c.name     AS course_name,
                   m.name     AS major_name,
                   cat.name   AS category_name
              FROM book b
              LEFT JOIN sys_user u   ON b.seller_id   = u.id
              LEFT JOIN course c     ON b.course_id   = c.id
              LEFT JOIN major m      ON b.major_id    = m.id
              LEFT JOIN category cat ON b.category_id = cat.id
            """;

    @Select("""
            <script>
            """ + BASE_SELECT + """
            <where>
              <if test="status != null"> AND b.status = #{status} </if>
              <if test="keyword != null and keyword != ''">
                AND (b.title LIKE CONCAT('%', #{keyword}, '%')
                     OR b.author LIKE CONCAT('%', #{keyword}, '%')
                     OR b.isbn LIKE CONCAT('%', #{keyword}, '%')
                     OR b.publisher LIKE CONCAT('%', #{keyword}, '%')
                     OR b.title_pinyin LIKE CONCAT('%', LOWER(#{keyword}), '%'))
              </if>
              <if test="categoryId != null"> AND b.category_id = #{categoryId} </if>
              <if test="majorId != null">    AND b.major_id    = #{majorId}    </if>
              <if test="courseId != null">   AND b.course_id   = #{courseId}   </if>
              <if test="sellerId != null">   AND b.seller_id   = #{sellerId}   </if>
              <if test="excludeId != null">  AND b.id &lt;&gt; #{excludeId}     </if>
              <if test="priceMin != null">   AND b.price &gt;= #{priceMin}     </if>
              <if test="priceMax != null">   AND b.price &lt;= #{priceMax}     </if>
              <if test="conditionMin != null"> AND b.condition_level &gt;= #{conditionMin} </if>
              <if test="noteFlag != null">   AND b.note_flag = #{noteFlag}     </if>
            </where>
            <choose>
              <when test="sort == 'priceAsc'">  ORDER BY b.price ASC, b.id DESC </when>
              <when test="sort == 'priceDesc'"> ORDER BY b.price DESC, b.id DESC </when>
              <when test="sort == 'hot'">       ORDER BY b.view_count DESC, b.id DESC </when>
              <when test="sort == 'priceSaving'"> ORDER BY (b.original_price - b.price) DESC, b.id DESC </when>
              <otherwise>                        ORDER BY b.priority DESC, b.created_at DESC, b.id DESC </otherwise>
            </choose>
            </script>
            """)
    IPage<Book> searchPage(IPage<Book> page,
                           @Param("status") Integer status,
                           @Param("keyword") String keyword,
                           @Param("categoryId") Long categoryId,
                           @Param("majorId") Long majorId,
                           @Param("courseId") Long courseId,
                           @Param("sellerId") Long sellerId,
                           @Param("excludeId") Long excludeId,
                           @Param("priceMin") BigDecimal priceMin,
                           @Param("priceMax") BigDecimal priceMax,
                           @Param("conditionMin") Integer conditionMin,
                           @Param("noteFlag") Integer noteFlag,
                           @Param("sort") String sort);

    @Select("""
            <script>
            """ + BASE_SELECT + """
            WHERE b.id IN
            <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
            </script>
            """)
    List<Book> selectByIdsWithMeta(@Param("ids") List<Long> ids);

    @Select("""
            <script>
            """ + BASE_SELECT + """
            <where>
              <if test="status != null"> AND b.status = #{status} </if>
              <if test="categoryId != null"> AND b.category_id = #{categoryId} </if>
              <if test="majorId != null">    AND b.major_id    = #{majorId}    </if>
              <if test="excludeId != null">  AND b.id &lt;&gt; #{excludeId}     </if>
            </where>
            ORDER BY b.view_count DESC, b.favorite_count DESC, b.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<Book> selectHot(@Param("status") Integer status,
                         @Param("categoryId") Long categoryId,
                         @Param("majorId") Long majorId,
                         @Param("excludeId") Long excludeId,
                         @Param("limit") int limit);

    // ---------------- 统计 ----------------

    @Select("SELECT COUNT(*) FROM book WHERE status = #{status}")
    long countByStatus(@Param("status") int status);

    @Select("""
            SELECT cat.name AS name, COUNT(*) AS cnt
              FROM book b LEFT JOIN category cat ON b.category_id = cat.id
             GROUP BY cat.name ORDER BY cnt DESC
            """)
    List<Map<String, Object>> statByCategory();

    @Select("""
            SELECT m.name AS name, COUNT(*) AS cnt
              FROM book b LEFT JOIN major m ON b.major_id = m.id
             GROUP BY m.name ORDER BY cnt DESC
            """)
    List<Map<String, Object>> statByMajor();

    @Select("""
            SELECT CASE
                     WHEN price < 10  THEN '0-10元'
                     WHEN price < 20  THEN '10-20元'
                     WHEN price < 30  THEN '20-30元'
                     WHEN price < 50  THEN '30-50元'
                     WHEN price < 80  THEN '50-80元'
                     ELSE '80元以上'
                   END AS name,
                   COUNT(*) AS cnt
              FROM book GROUP BY name ORDER BY MIN(price)
            """)
    List<Map<String, Object>> statByPriceRange();

    @Select("""
            SELECT DATE_FORMAT(created_at, '%m-%d') AS name, COUNT(*) AS cnt
              FROM book
             WHERE created_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
             GROUP BY name ORDER BY MIN(created_at)
            """)
    List<Map<String, Object>> statDailyNewBooks();

    @Select("SELECT AVG(price) FROM book WHERE status = #{status}")
    BigDecimal avgPriceByStatus(@Param("status") int status);

    @Select("""
            SELECT b.id, b.title, b.original_price, b.price, b.seller_id
              FROM book b WHERE b.status = 1
             ORDER BY b.created_at DESC LIMIT #{limit}
            """)
    List<Book> latestOnSale(@Param("limit") int limit);
}
