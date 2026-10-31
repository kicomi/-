package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbook.common.BizException;
import com.campusbook.entity.Major;
import com.campusbook.entity.User;
import com.campusbook.mapper.MajorMapper;
import com.campusbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final MajorMapper majorMapper;
    private final NotificationService notifyService;

    /** 把天数转成人话：0 表示永久 */
    private String durationText(int days) {
        return days > 0 ? days + " 天" : "永久";
    }

    public User get(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        fillMajor(u);
        // 实名信息只在管理端可见，学生端接口一律清空
        u.setRealName(null);
        u.setRealPhone(null);
        return u;
    }

    public void fillMajor(User u) {
        if (u == null) return;
        // 手动填的专业优先展示，没填才回落到字典表
        if (u.getCustomMajor() != null && !u.getCustomMajor().isBlank()) {
            u.setMajorName(u.getCustomMajor());
            return;
        }
        if (u.getMajorId() == null) return;
        Major m = majorMapper.selectById(u.getMajorId());
        if (m != null) u.setMajorName(m.getName());
    }

    public IPage<User> page(int page, int size, String keyword, String sort) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 纯数字且确实是某个用户的 id 时，直接只返回那一个，
            // 否则手机号里的数字会把模糊匹配撑爆（搜 "3" 能命中两百条）
            if (kw.matches("\\d+") && userMapper.selectById(Long.valueOf(kw)) != null) {
                w.eq(User::getId, Long.valueOf(kw));
            } else {
                w.and(q -> q.like(User::getNickname, kw)
                        .or().like(User::getStudentNo, kw)
                        .or().like(User::getPhone, kw));
            }
        }
        // 默认新用户在前；点表头切成正序
        if ("idAsc".equals(sort)) w.orderByAsc(User::getId);
        else w.orderByDesc(User::getId);
        IPage<User> r = userMapper.selectPage(p, w);
        Map<Long, String> mm = majorMapper.selectList(null).stream()
                .collect(Collectors.toMap(Major::getId, Major::getName));
        r.getRecords().forEach(u -> {
            // 优先用手动填写的专业名，没有再取字典
            String custom = u.getCustomMajor();
            u.setMajorName(custom != null && !custom.isBlank() ? custom : mm.get(u.getMajorId()));
        });
        return r;
    }

    /**
     * 管理员调整信用分。
     * 用户自己只能通过交易涨分，人工干预属于纠错（比如误判导致的扣分），
     * 所以必须留原因，并通知本人。
     */
    public User adjustCredit(Long id, Integer score, String reason) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getRole())) throw new BizException("不能修改管理员信用分");
        if (score == null || score < 0 || score > 100) throw new BizException("信用分必须在 0-100 之间");

        int old = u.getCreditScore() == null ? 100 : u.getCreditScore();
        u.setCreditScore(score);
        userMapper.updateById(u);

        String delta = (score >= old ? "+" : "") + (score - old);
        notifyService.push(id, "system", "信用分已调整",
                "管理员将你的信用分从 " + old + " 调整为 " + score + "（" + delta + "）"
                        + (reason == null || reason.isBlank() ? "" : "，原因：" + reason),
                null, null);
        return u;
    }

    /**
     * 管理员重置昵称。违规昵称（含联系方式、广告、脏话）会被改成"同学 + id"，
     * 并在站内通知里告知本人，避免用户莫名其妙发现名字被改。
     */
    public User resetNickname(Long id, String nickname) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getRole())) throw new BizException("不能修改管理员昵称");
        if (nickname == null || nickname.isBlank()) throw new BizException("昵称不能为空");
        if (nickname.length() > 20) throw new BizException("昵称不能超过 20 个字");

        String old = u.getNickname();
        u.setNickname(nickname.trim());
        userMapper.updateById(u);

        notifyService.push(id, "system", "昵称已被管理员修改",
                "原昵称「" + (old == null ? "" : old) + "」可能违反了社区规范，现已调整为「"
                        + u.getNickname() + "」。如需申诉请联系管理员。",
                null, null);
        return u;
    }

    public void updateStatus(Long id, Integer status) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        u.setStatus(status);
        if (status == 1) {
            // 解除封禁时把到期时间和原因一起清掉，避免残留
            u.setBanUntil(null);
            u.setBanReason(null);
        }
        userMapper.updateById(u);
    }

    /**
     * 封禁用户。days <= 0 表示永久封禁。
     * 永久封禁用 status=0 + banUntil=null 表示；临时封禁额外记一个到期时间，
     * 登录时会自动判断是否已过期。
     */
    public User ban(Long id, int days, String reason) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getRole())) throw new BizException("不能封禁管理员账号");
        u.setStatus(0);
        u.setBanReason(reason);
        u.setBanUntil(days > 0 ? LocalDateTime.now().plusDays(days) : null);
        userMapper.updateById(u);

        // 被封的人下次登录就进不来了，必须在站内留个说法
        notifyService.push(id, "system", "账号已被封禁",
                "封禁时长：" + durationText(days)
                        + (reason == null || reason.isBlank() ? "" : "；原因：" + reason)
                        + "。如有异议请联系管理员。",
                null, null);
        return decorate(u);
    }

    public User unban(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        u.setStatus(1);
        u.setBanUntil(null);
        u.setBanReason(null);
        userMapper.updateById(u);

        notifyService.push(id, "system", "账号已恢复",
                "你的账号封禁已解除，可以正常发布和交易了。", null, null);
        return decorate(u);
    }

    /** 禁言。days <= 0 表示永久禁言。管理端先占位，等小程序聊天功能上线后生效。 */
    public User mute(Long id, int days, String reason) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if ("admin".equals(u.getRole())) throw new BizException("不能禁言管理员账号");
        u.setMuteStatus(1);
        u.setMuteReason(reason);
        u.setMuteUntil(days > 0 ? LocalDateTime.now().plusDays(days) : null);
        userMapper.updateById(u);

        notifyService.push(id, "system", "你已被禁言",
                "禁言时长：" + durationText(days)
                        + (reason == null || reason.isBlank() ? "" : "；原因：" + reason)
                        + "。禁言期间无法发送聊天消息。",
                null, null);
        return decorate(u);
    }

    public User unmute(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        u.setMuteStatus(0);
        u.setMuteUntil(null);
        u.setMuteReason(null);
        userMapper.updateById(u);

        notifyService.push(id, "system", "禁言已解除",
                "你的禁言已解除，可以正常和同学沟通了。", null, null);
        return decorate(u);
    }

    /**
     * 生成状态文案。封禁/禁言的临时状态到期后在这里顺手复位，
     * 省得再写定时任务（校园系统量级很小，登录时顺带检查就够了）。
     */
    public User decorate(User u) {
        if (u == null) return null;
        LocalDateTime now = LocalDateTime.now();

        if (u.getStatus() != null && u.getStatus() == 0
                && u.getBanUntil() != null && u.getBanUntil().isBefore(now)) {
            u.setStatus(1);
            u.setBanUntil(null);
            u.setBanReason(null);
            userMapper.updateById(u);
        }
        if (u.getMuteStatus() != null && u.getMuteStatus() == 1
                && u.getMuteUntil() != null && u.getMuteUntil().isBefore(now)) {
            u.setMuteStatus(0);
            u.setMuteUntil(null);
            u.setMuteReason(null);
            userMapper.updateById(u);
        }

        StringBuilder sb = new StringBuilder();
        if (u.getStatus() != null && u.getStatus() == 0) {
            sb.append(u.getBanUntil() == null ? "已永久封禁" : "封禁至 " + u.getBanUntil().toLocalDate());
        } else {
            sb.append("正常");
        }
        if (u.getMuteStatus() != null && u.getMuteStatus() == 1) {
            sb.append(u.getMuteUntil() == null ? " · 永久禁言" : " · 禁言至 " + u.getMuteUntil().toLocalDate());
        }
        u.setStatusText(sb.toString());
        return u;
    }

    public void updateProfile(Long id, User patch) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(404, "用户不存在");
        if (patch.getNickname() != null && !patch.getNickname().isBlank()) {
            String nick = patch.getNickname().trim();
            // 数据库列是 varchar(20)，不拦下来会直接抛 SQL 异常给前端
            if (nick.length() > 20) throw new BizException("昵称不能超过 20 个字");
            u.setNickname(nick);
        }
        if (patch.getAvatar() != null) u.setAvatar(patch.getAvatar());
        if (patch.getPhone() != null) u.setPhone(patch.getPhone());
        if (patch.getMajorId() != null) u.setMajorId(patch.getMajorId());
        if (patch.getGrade() != null) u.setGrade(patch.getGrade());
        if (patch.getGender() != null) u.setGender(patch.getGender());
        if (patch.getSignature() != null) {
            if (patch.getSignature().length() > 120) throw new BizException("个性签名不能超过 120 个字");
            u.setSignature(patch.getSignature());
        }
        // 专业允许手动输入；传了自定义名就不再依赖字典 id
        if (patch.getCustomMajor() != null) {
            String cm = patch.getCustomMajor().trim();
            u.setCustomMajor(cm.isEmpty() ? null : cm);
            if (!cm.isEmpty()) u.setMajorId(null);
        }
        // 实名信息只接受注册时采集，这里不允许覆盖
        userMapper.updateById(u);
    }

    public List<User> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return userMapper.selectBatchIds(ids);
    }
}
