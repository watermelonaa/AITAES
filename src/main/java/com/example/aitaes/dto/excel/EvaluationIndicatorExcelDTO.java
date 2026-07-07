package com.example.aitaes.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 评价指标 Excel DTO（EasyExcel 列映射）
 * 注：parentIndicatorNo 在导入策略中解析为 parentId
 */
@Data
public class EvaluationIndicatorExcelDTO {

    @ExcelProperty("指标编号")
    private String indicatorNo;

    @ExcelProperty("指标名称")
    private String indicatorName;

    @ExcelProperty("指标类别")
    private String category;

    @ExcelProperty("权重")
    private BigDecimal weight;

    @ExcelProperty("父级指标编号")
    private String parentIndicatorNo;

    @ExcelProperty("指标层级")
    private Integer level;

    @ExcelProperty("指标说明")
    private String description;

    @ExcelProperty("排序号")
    private Integer sortOrder;
}
