package com.example.aitaes.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 考核成绩导入 Excel DTO
 * <p>
 * 对应模板格式：序号, 学号, 姓名, 第1题得分, 扣分主要知识点,
 * 第2题得分, 扣分主要知识点, ..., 第N题得分, 扣分主要知识点, 总成绩, 最薄弱知识点
 * <p>
 * 注意：题目列是动态的（数量可变），固定列使用 @ExcelProperty 映射，
 * 动态题目列在 {@link com.example.aitaes.strategy.AssessmentImportStrategy} 中处理。
 */
@Data
public class AssessmentRecordExcelDTO {

    @ExcelProperty("序号")
    private Integer seq;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    /** 总成绩 */
    @ExcelProperty("总成绩")
    private String totalScore;

    /** 最薄弱知识点 */
    @ExcelProperty("最薄弱知识点")
    private String weakestKp;

    /**
     * 以下为动态列（第1-5题的得分和扣分知识点），
     * 实际解析时由 ImportStrategy 根据 header 动态处理
     */
    @ExcelProperty("第1题得分")
    private String q1Score;

    @ExcelProperty("扣分主要知识点")
    private String q1DeductionKp;

    @ExcelProperty("第2题得分")
    private String q2Score;

    @ExcelProperty("扣分主要知识点")
    private String q2DeductionKp;

    @ExcelProperty("第3题得分")
    private String q3Score;

    @ExcelProperty("扣分主要知识点")
    private String q3DeductionKp;

    @ExcelProperty("第4题得分")
    private String q4Score;

    @ExcelProperty("扣分主要知识点")
    private String q4DeductionKp;

    @ExcelProperty("第5题得分")
    private String q5Score;

    @ExcelProperty("扣分主要知识点")
    private String q5DeductionKp;
}
