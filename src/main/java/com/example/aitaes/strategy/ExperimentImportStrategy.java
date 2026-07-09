package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.ExperimentExcelDTO;
import com.example.aitaes.entity.Experiment;
import com.example.aitaes.entity.Student;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.ExperimentMapper;
import com.example.aitaes.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 实验报告导入策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentImportStrategy implements ImportStrategy {

    private final ExperimentMapper experimentMapper;
    private final StudentMapper studentMapper;

    private Long courseId;
    private String semester;

    @Override
    public ImportType getSupportedType() {
        return ImportType.EXPERIMENT;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<ExperimentExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, ExperimentExcelDTO.class, listener)
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<ExperimentExcelDTO> dtoList) {
        for (ExperimentExcelDTO dto : dtoList) {
            try {
                if (dto.getStudentNo() == null || dto.getStudentNo().isBlank()) continue;

                Student student = studentMapper.selectOne(
                        new LambdaQueryWrapper<Student>()
                                .eq(Student::getStudentNo, dto.getStudentNo()));
                if (student == null) continue;

                Experiment exp = new Experiment();
                exp.setCourseId(courseId);
                exp.setStudentId(student.getId());
                exp.setExperimentName(dto.getExperimentName());
                exp.setExperimentNo(dto.getExperimentNo());
                exp.setSemester(semester);
                exp.setRemark(dto.getRemark());

                if (dto.getScore() != null && !dto.getScore().isBlank()) {
                    exp.setScore(new BigDecimal(dto.getScore()));
                }

                if (dto.getSubmitTime() != null && !dto.getSubmitTime().isBlank()) {
                    try {
                        exp.setSubmitTime(LocalDateTime.parse(dto.getSubmitTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } catch (Exception e) {
                        exp.setSubmitTime(LocalDateTime.now());
                    }
                }

                experimentMapper.insert(exp);
            } catch (Exception e) {
                log.warn("实验报告插入失败: {}", e.getMessage());
            }
        }
    }

    private ImportResultDTO buildResult(GenericExcelListener<?> listener) {
        ImportResultDTO result = new ImportResultDTO();
        result.setTotalRows(listener.getTotalRows());
        result.setSuccessRows(listener.getSuccessCount());
        result.setFailRows(listener.getFailCount());
        result.setStatus(listener.getFailCount() == 0 ? ImportStatus.SUCCESS.getCode()
                : listener.getSuccessCount() == 0 ? ImportStatus.FAILED.getCode()
                : ImportStatus.PARTIAL.getCode());
        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
