package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仪表盘概览统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatsDTO {

    /** 教师总数 */
    private Long totalTeachers;

    /** 学生总数 */
    private Long totalStudents;

    /** 课程总数 */
    private Long totalCourses;

    /** 评价记录总数 */
    private Long totalEvaluations;

    /** 全校综合平均分 */
    private BigDecimal averageOverallScore;

    /** 参评课程数 */
    private Long evaluatedCourseCount;
}
