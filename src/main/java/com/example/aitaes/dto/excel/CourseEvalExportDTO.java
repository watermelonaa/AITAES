package com.example.aitaes.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课程评价 Excel 导出 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvalExportDTO {

    @ExcelProperty("课程编号")
    @ColumnWidth(14)
    private String courseNo;

    @ExcelProperty("课程名称")
    @ColumnWidth(22)
    private String courseName;

    @ExcelProperty("授课教师")
    @ColumnWidth(12)
    private String teacherName;

    @ExcelProperty("学期")
    @ColumnWidth(16)
    private String semester;

    @ExcelProperty("参评人数")
    @ColumnWidth(10)
    private Integer studentCount;

    @ExcelProperty("综合得分")
    @ColumnWidth(12)
    private BigDecimal overallScore;

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
