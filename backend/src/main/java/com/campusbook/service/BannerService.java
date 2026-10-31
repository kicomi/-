package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.common.BizException;
import com.campusbook.entity.Banner;
import com.campusbook.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    public List<Banner> list(String position) {
        LambdaQueryWrapper<Banner> w = new LambdaQueryWrapper<>();
        if (position != null && !position.isBlank()) w.eq(Banner::getPosition, position);
        w.orderByAsc(Banner::getSort).orderByDesc(Banner::getId);
        return bannerMapper.selectList(w);
    }

    public List<Banner> active(String position, int limit) {
        return bannerMapper.selectActive(
                position == null || position.isBlank() ? "home_banner" : position,
                Math.max(1, Math.min(limit, 20)));
    }

    public Banner save(Banner banner) {
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            throw new BizException("广告标题不能为空");
        }
        if (banner.getPosition() == null || banner.getPosition().isBlank()) {
            banner.setPosition("home_banner");
        }
        if (banner.getStatus() == null) banner.setStatus(1);
        if (banner.getSort() == null) banner.setSort(0);

        if (banner.getId() == null) {
            banner.setViewCount(0);
            banner.setClickCount(0);
            bannerMapper.insert(banner);
        } else {
            Banner old = bannerMapper.selectById(banner.getId());
            if (old == null) throw new BizException(404, "广告不存在");
            // 浏览/点击是运营数据，不能被编辑表单覆盖
            banner.setViewCount(old.getViewCount());
            banner.setClickCount(old.getClickCount());
            bannerMapper.updateById(banner);
        }
        return bannerMapper.selectById(banner.getId());
    }

    public void toggle(Long id, Integer status) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) throw new BizException(404, "广告不存在");
        b.setStatus(status);
        b.setUpdatedAt(LocalDateTime.now());
        bannerMapper.updateById(b);
    }

    public void delete(Long id) {
        bannerMapper.deleteById(id);
    }

    /** 学生端曝光一次 +1，用于评估投放效果 */
    public void addView(Long id) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) return;
        b.setViewCount((b.getViewCount() == null ? 0 : b.getViewCount()) + 1);
        bannerMapper.updateById(b);
    }
}
