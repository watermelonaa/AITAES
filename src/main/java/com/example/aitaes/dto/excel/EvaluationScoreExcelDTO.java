package com.example.aitaes.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价分数 Excel DTO（EasyExcel 列映射）
 * 注：courseNo/studentNo/indicatorNo 在导入策略中解析为 courseId/studentId/indicatorId
 */
@Data
public class EvaluationScoreExcelDTO {

    @ExcelProperty("课程编号")
    private String courseNo;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("指标编号")
    private String indicatorNo;

    @ExcelProperty("评分")
    private BigDecimal score;

    @ExcelProperty("评价意见")
    private String comment;

    @ExcelProperty("学期")
    private String semester;

    @ExcelProperty("评价时间")
    private String evaluateTime;
}
