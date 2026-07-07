package com.example.aitaes.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 教师评价汇总 Excel 导出 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEvalExportDTO {

    @ExcelProperty("工号")
    @ColumnWidth(14)
    private String teacherNo;

    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String name;

    @ExcelProperty("学院")
    @ColumnWidth(16)
    private String college;

    @ExcelProperty("职称")
    @ColumnWidth(12)
    private String title;

    @ExcelProperty("课程数")
    @ColumnWidth(10)
    private Integer courseCount;

    @ExcelProperty("综合平均分")
    @ColumnWidth(14)
    private BigDecimal avgScore;

    @ExcelProperty("教学态度")
    @ColumnWidth(12)
    private BigDecimal attitudeScore;

    @ExcelProperty("教学内容")
    @ColumnWidth(12)
    private BigDecimal contentScore;

    @ExcelProperty("教学方法")
    @ColumnWidth(12)
    private BigDecimal methodScore;

    @ExcelProperty("教学效果")
    @ColumnWidth(12)
    private BigDecimal effectScore;
}
