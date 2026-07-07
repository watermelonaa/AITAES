package com.example.aitaes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程-学生选课关联表
 */
@Data
@TableName("t_course_student")
public class CourseStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 学生ID */
    private Long studentId;

    /** 选课学期 */
    private String semester;

    private LocalDateTime createTime;
}
