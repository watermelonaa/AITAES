package com.example.aitaes.strategy;

import com.example.aitaes.enums.ImportType;
import com.example.aitaes.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 考试成绩导入策略（期中/期末）
 * <p>
 * 继承 {@link AbstractAssessmentImportStrategy}，assessment_type = MIDTERM 或 FINAL
 * （由文件名第二段决定）。
 * <p>
 * Excel 格式与作业成绩相同。
 * 文件名格式：{课程编号}_EXAM_SCORE_{描述}.xlsx
 * 如：CS-NET-001_EXAM_SCORE_期中考试.xlsx
 */
@Slf4j
@Component
public class ExamScoreImportStrategy extends AbstractAssessmentImportStrategy {

    public ExamScoreImportStrategy(AssessmentMapper assessmentMapper,
                                    AssessmentRecordMapper recordMapper,
                                    RecordKpDeductionMapper deductionMapper,
                                    StudentMapper studentMapper,
                                    CourseMapper courseMapper,
                                    StudentKpMasteryMapper masteryMapper) {
        super(assessmentMapper, recordMapper, deductionMapper,
                studentMapper, courseMapper, masteryMapper);
    }

    @Override
    public ImportType getSupportedType() {
        return ImportType.EXAM_SCORE;
    }

    @Override
    protected void parseFileName(String filename) {
        // 先调用基类解析
        super.parseFileName(filename);
        // EXAM_SCORE 类型的文件名第二段可能是 MIDTERM 或 FINAL
        // 基类已从文件名第二段设置 assessmentType
        if (assessmentType == null && filename != null) {
            String name = filename.replace(".xlsx", "").replace(".xls", "");
            String[] parts = name.split("_");
            if (parts.length >= 2 && "EXAM_SCORE".equalsIgnoreCase(parts[1])) {
                // 文件名第三段可作为 assessmentType（MIDTERM/FINAL）
                if (parts.length >= 3
                        && ("MIDTERM".equalsIgnoreCase(parts[2]) || "FINAL".equalsIgnoreCase(parts[2]))) {
                    this.assessmentType = parts[2].toUpperCase();
                    this.assessmentName = parts.length >= 4 ? parts[3] : parts[2];
                } else {
                    this.assessmentType = "MIDTERM";
                    this.assessmentName = parts.length >= 3 ? parts[2] : "考试成绩";
                }
            }
        }
    }
}
