package com.example.aitaes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价报告
 */
@Data
@TableName("t_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报告名称 */
    private String reportName;

    /** 报告类型：TEACHER / COURSE / COLLEGE / SEMESTER */
    private String reportType;

    /** 学期 */
    private String semester;

    /** 关联课程ID */
    private Long courseId;

    /** 关联教师ID */
    private Long teacherId;

    /** 报告摘要 */
    private String summary;

    /** 报告详细数据（JSON） */
    private String reportData;

    /** AI分析结果 */
    private String aiAnalysis;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /** 逻辑删除（0=正常, 1=删除） */
    private Integer deleted;
}
