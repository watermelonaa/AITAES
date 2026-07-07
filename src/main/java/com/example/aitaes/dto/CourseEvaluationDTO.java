package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程评价结果 DTO — 一门课程的综合评价
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvaluationDTO {

    /** 课程ID */
    private Long courseId;

    /** 课程编号 */
    private String courseNo;

    /** 课程名称 */
    private String courseName;

    /** 授课教师ID */
    private Long teacherId;

    /** 授课教师姓名 */
    private String teacherName;

    /** 学期 */
    private String semester;

    /** 参评学生数 */
    private Integer totalStudents;

    /** 综合得分 (0-100) */
    private BigDecimal overallScore;

    /** 四个维度得分明细 */
    private List<CategoryScoreDTO> categoryScores;

    /** 全部二级指标得分明细 */
    private List<IndicatorScoreDTO> indicatorScores;
}
