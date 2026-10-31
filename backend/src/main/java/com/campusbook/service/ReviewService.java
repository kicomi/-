package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.BizException;
import com.campusbook.dto.ReviewReq;
import com.campusbook.entity.BookOrder;
import com.campusbook.entity.Review;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookOrderMapper;
import com.campusbook.mapper.ReviewMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookOrderMapper orderMapper;
    private final UserMapper userMapper;
    private final NotificationService notifyService;

    @Transactional
    public Review create(Long userId, ReviewReq req) {
        BookOrder o = orderMapper.selectById(req.getOrderId());
        if (o == null) throw new BizException(404, "订单不存在");
        if (!Objects.equals(o.getBuyerId(), userId)) throw new BizException(403, "只有买家可以评价");
        if (o.getStatus() != 2) throw new BizException("订单未完成，暂不能评价");
        Long exist = reviewMapper.selectCount(new LambdaQueryWrapper<Review>().eq(Review::getOrderId, o.getId()));
        if (exist != null && exist > 0) throw new BizException("该订单已评价");

        Review r = new Review();
        r.setOrderId(o.getId());
        r.setBookId(o.getBookId());
        r.setFromUserId(userId);
        r.setToUserId(o.getSellerId());
        r.setRating(req.getRating() == null ? 5 : req.getRating());
        r.setContent(req.getContent());
        r.setTags(req.getTags());
        // 评价配图，前端传 JSON 数组字符串，这里只做长度兜底
        String imgs = req.getImages();
        r.setImages(imgs != null && imgs.length() > 1000 ? imgs.substring(0, 1000) : imgs);
        reviewMapper.insert(r);

        // 告诉卖家有人评价了他，否则他只知道"交易完成"
        notifyService.push(o.getSellerId(), "order", "收到一条新评价",
                "买家给了 " + r.getRating() + " 星："
                        + (r.getContent() == null || r.getContent().isBlank() ? "（没有填写内容）" : r.getContent()),
                "order", o.getId());

        User seller = userMapper.selectById(o.getSellerId());
        if (seller != null) {
            int s = (seller.getCreditScore() == null ? 100 : seller.getCreditScore()) + 1;
            seller.setCreditScore(Math.min(100, s));
            userMapper.updateById(seller);
        }
        return r;
    }

    public List<Review> listByBook(Long bookId) {
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getBookId, bookId).orderByDesc(Review::getId));
        return withNickname(list);
    }

    /** 修改评价：只有作者本人能改，评分会同步重算信用分太复杂，这里只改内容 */
    @Transactional
    public Review update(Long userId, Long id, com.campusbook.dto.ReviewUpdateReq req) {
        Review r = reviewMapper.selectById(id);
        if (r == null) throw new BizException(404, "评价不存在");
        if (!Objects.equals(r.getFromUserId(), userId)) throw new BizException(403, "只能修改自己的评价");
        if (req.getRating() != null) r.setRating(req.getRating());
        if (req.getContent() != null) r.setContent(req.getContent());
        if (req.getImages() != null) r.setImages(req.getImages());
        reviewMapper.updateById(r);
        return r;
    }

    /** 删除评价 */
    @Transactional
    public void delete(Long userId, Long id) {
        Review r = reviewMapper.selectById(id);
        if (r == null) throw new BizException(404, "评价不存在");
        if (!Objects.equals(r.getFromUserId(), userId)) throw new BizException(403, "只能删除自己的评价");
        reviewMapper.deleteById(id);
    }

    /** 某条订单对应的评价，订单页「查看评价」用 */
    public Review byOrder(Long userId, Long orderId) {
        Review r = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId).last("limit 1"));
        if (r == null) throw new BizException(404, "该订单还没有评价");
        if (!Objects.equals(r.getFromUserId(), userId)) throw new BizException(403, "无权查看");
        return r;
    }

    /** 书籍评价分页，评价独立页用 */
    public com.campusbook.common.PageResult<Review> pageByBook(Long bookId, int pageNo, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Review> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, size);
        com.baomidou.mybatisplus.core.metadata.IPage<Review> r = reviewMapper.selectPage(p,
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getBookId, bookId)
                        .orderByDesc(Review::getId));
        withNickname(r.getRecords());
        return com.campusbook.common.PageResult.of(r.getTotal(), pageNo, size, r.getRecords());
    }

    /** 这本书的评价概况：平均分、条数、各星级分布 */
    public Map<String, Object> summary(Long bookId) {
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getBookId, bookId));
        Map<String, Object> out = new LinkedHashMap<>();
        int total = list.size();
        int sum = 0;
        int[] dist = new int[5];
        for (Review r : list) {
            int rating = r.getRating() == null ? 5 : r.getRating();
            sum += rating;
            dist[Math.min(Math.max(rating - 1, 0), 4)]++;
        }
        out.put("total", total);
        out.put("avg", total == 0 ? 0 : Math.round(sum * 10.0 / total) / 10.0);
        out.put("dist", dist);
        return out;
    }

    public List<Review> listByUser(Long userId) {
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getToUserId, userId).orderByDesc(Review::getId));
        return withNickname(list);
    }

    private List<Review> withNickname(List<Review> list) {
        if (list.isEmpty()) return list;
        Set<Long> ids = list.stream().map(Review::getFromUserId).collect(Collectors.toSet());
        Map<Long, String> names = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
        list.forEach(r -> r.setFromNickname(names.getOrDefault(r.getFromUserId(), "同学")));
        return list;
    }
}
