package com.example.aitaes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 类别得分 DTO（教学态度/教学内容/教学方法/教学效果）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryScoreDTO {

    /** 类别名称 */
    private String category;

    /** 类别权重 */
    private BigDecimal weight;

    /** 类别标准化得分 (0-100) */
    private BigDecimal score;

    /** 类别加权得分 */
    private BigDecimal weightedScore;
}
