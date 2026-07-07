package com.example.aitaes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_evaluation_score")
public class EvaluationScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 学生ID */
    private Long studentId;

    /** 评价指标ID */
    private Long indicatorId;

    /** 评分 */
    private BigDecimal score;

    /** 评价意见 */
    private String comment;

    /** 学期 */
    private String semester;

    /** 评价时间 */
    private LocalDateTime evaluateTime;
}
