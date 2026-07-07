package com.example.aitaes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_evaluation_indicator")
public class EvaluationIndicator {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标编号 */
    private String indicatorNo;

    /** 指标名称 */
    private String indicatorName;

    /** 指标类别：教学态度/教学内容/教学方法/教学效果 */
    private String category;

    /** 权重 */
    private BigDecimal weight;

    /** 父级指标ID（支持二级指标体系） */
    private Long parentId;

    /** 指标层级：1-一级指标，2-二级指标 */
    private Integer level;

    /** 指标说明 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
