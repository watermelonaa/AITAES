package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仪表盘完整数据 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    /** 概览统计卡片 */
    private OverviewStatsDTO overview;

    /** 全校评分分布 */
    private List<ScoreDistributionDTO> scoreDistribution;

    /** 学院排名 Top10 */
    private List<CollegeRankItem> collegeRanking;

    /** 学期趋势 */
    private TrendDTO trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollegeRankItem {
        private String college;
        private Long teacherCount;
        private Long evaluatedCourseCount;
        private java.math.BigDecimal avgScore;
    }
}
