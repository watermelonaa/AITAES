package com.example.aitaes.strategy;

import com.example.aitaes.enums.ImportType;
import com.example.aitaes.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 作业成绩导入策略
 * <p>
 * 继承 {@link AbstractAssessmentImportStrategy}，assessment_type = HOMEWORK。
 * 文件名格式：{课程编号}_HOMEWORK_{考核名称}.xlsx
 */
@Slf4j
@Component
public class AssessmentImportStrategy extends AbstractAssessmentImportStrategy {

    public AssessmentImportStrategy(AssessmentMapper assessmentMapper,
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
        return ImportType.HOMEWORK;
    }
}
