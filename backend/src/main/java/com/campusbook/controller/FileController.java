package com.campusbook.controller;

import com.campusbook.common.BizException;
import com.campusbook.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Tag(name = "文件")
@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /**
     * 图片上传。只有登录用户能传，按年月分目录，文件名用 UUID 避免撞名。
     * 返回相对路径，前端拼上服务地址即可访问（WebConfig 里已把 /uploads/** 映射到本地目录）。
     */
    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("请选择要上传的图片");

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        if (!ALLOWED.contains(ext)) throw new BizException("只支持 jpg / png / webp / gif 格式");

        // 光看后缀不够：把内容当图片解码一次，不是图片直接拒掉
        try (java.io.InputStream in = file.getInputStream()) {
            if (javax.imageio.ImageIO.read(in) == null) {
                throw new BizException("这个文件不是有效的图片");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("这个文件不是有效的图片");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new BizException("图片不能超过 20MB，当前约 " + (file.getSize() / 1024 / 1024) + "MB");
        }

        try {
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            // transferTo 对相对路径会解析到 Tomcat 的临时目录，这里必须转成绝对路径
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path dir = base.resolve(month);
            Files.createDirectories(dir);

            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("url", "/uploads/" + month + "/" + name);
            out.put("size", file.getSize());
            out.put("name", original);
            return R.ok(out);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BizException("上传失败：" + e.getMessage());
        }
    }
}
