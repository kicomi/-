package com.campusbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbook.entity.Category;
import com.campusbook.entity.Course;
import com.campusbook.entity.Major;
import com.campusbook.entity.Tag;
import com.campusbook.mapper.CategoryMapper;
import com.campusbook.mapper.CourseMapper;
import com.campusbook.mapper.MajorMapper;
import com.campusbook.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DictService {

    private final MajorMapper majorMapper;
    private final CourseMapper courseMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public List<Major> majors() {
        return majorMapper.selectList(new LambdaQueryWrapper<Major>().orderByAsc(Major::getId));
    }

    public List<Course> courses(Long majorId) {
        LambdaQueryWrapper<Course> w = new LambdaQueryWrapper<>();
        if (majorId != null) w.eq(Course::getMajorId, majorId);
        w.orderByAsc(Course::getId);
        return courseMapper.selectList(w);
    }

    public List<Category> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public List<Tag> tags() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    public Map<String, Object> all() {
        Map<String, Object> m = new HashMap<>();
        m.put("majors", majors());
        m.put("courses", courses(null));
        m.put("categories", categories());
        m.put("tags", tags());
        return m;
    }
}
