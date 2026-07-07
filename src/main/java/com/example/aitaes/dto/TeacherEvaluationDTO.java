package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教师评价汇总 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEvaluationDTO {

    /** 教师ID */
    private Long teacherId;

    /** 工号 */
    private String teacherNo;

    /** 教师姓名 */
    private String name;

    /** 学院 */
    private String college;

    /** 职称 */
    private String title;

    /** 教授的课程数 */
    private Integer courseCount;

    /** 综合平均分 */
    private BigDecimal averageOverallScore;

    /** 四个维度的平均得分 */
    private List<CategoryScoreDTO> categoryAverages;

    /** 各课程得分明细 */
    private List<CourseEvaluationDTO> courseDetails;
}
