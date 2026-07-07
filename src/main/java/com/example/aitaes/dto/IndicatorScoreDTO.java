package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 评价指标得分 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorScoreDTO {

    /** 指标ID */
    private Long indicatorId;

    /** 指标编号 */
    private String indicatorNo;

    /** 指标名称 */
    private String indicatorName;

    /** 指标类别 */
    private String category;

    /** 指标层级 1=一级 2=二级 */
    private Integer level;

    /** 父级指标ID */
    private Long parentId;

    /** 平均分 */
    private BigDecimal avgScore;

    /** 权重 */
    private BigDecimal weight;

    /** 加权得分 (avgScore × weight) */
    private BigDecimal weightedScore;

    /** 评价人数 */
    private Integer evaluationCount;
}
