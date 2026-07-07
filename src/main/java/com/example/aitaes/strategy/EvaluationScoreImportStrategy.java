package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.EvaluationScoreExcelDTO;
import com.example.aitaes.entity.*;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价分数数据导入策略
 * 需要解析 3 个外键：courseNo→courseId, studentNo→studentId, indicatorNo→indicatorId
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationScoreImportStrategy implements ImportStrategy {

    private final EvaluationScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;
    private final EvaluationIndicatorMapper indicatorMapper;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public ImportType getSupportedType() {
        return ImportType.SCORE;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<EvaluationScoreExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, EvaluationScoreExcelDTO.class, listener)
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<EvaluationScoreExcelDTO> dtoList) {
        // 1. 预查已存在的评分记录 (course_id, student_id, indicator_id) 组合
        // 先做 FK 解析再查重，因为 uniqueness 是基于 FK ID 的

        // 2. 解析 courseNo → courseId
        List<String> courseNos = dtoList.stream()
                .map(EvaluationScoreExcelDTO::getCourseNo)
                .filter(Objects::nonNull)
                .distinct().toList();
        Map<String, Long> courseNoToId = resolveCourseNos(courseNos);

        // 3. 解析 studentNo → studentId
        List<String> studentNos = dtoList.stream()
                .map(EvaluationScoreExcelDTO::getStudentNo)
                .filter(Objects::nonNull)
                .distinct().toList();
        Map<String, Long> studentNoToId = resolveStudentNos(studentNos);

        // 4. 解析 indicatorNo → indicatorId
        List<String> indicatorNos = dtoList.stream()
                .map(EvaluationScoreExcelDTO::getIndicatorNo)
                .filter(Objects::nonNull)
                .distinct().toList();
        Map<String, Long> indicatorNoToId = resolveIndicatorNos(indicatorNos);

        // 5. 预查重复（基于已解析的 FK）
        Set<String> existingKeys = buildExistingKeySet(dtoList,
                courseNoToId, studentNoToId, indicatorNoToId);

        // 6. 转换 + 过滤重复
        List<EvaluationScore> scores = dtoList.stream()
                .filter(dto -> dto.getCourseNo() != null && dto.getStudentNo() != null
                        && dto.getIndicatorNo() != null)
                .filter(dto -> {
                    Long cId = courseNoToId.get(dto.getCourseNo());
                    Long sId = studentNoToId.get(dto.getStudentNo());
                    Long iId = indicatorNoToId.get(dto.getIndicatorNo());
                    return cId != null && sId != null && iId != null
                            && !existingKeys.contains(cId + "-" + sId + "-" + iId);
                })
                .map(dto -> toEntity(dto, courseNoToId, studentNoToId, indicatorNoToId))
                .toList();

        // 7. 批量插入
        if (!scores.isEmpty()) {
            scores.forEach(scoreMapper::insert);
            log.debug("批量插入评价分数数据 {} 条", scores.size());
        }
    }

    private Map<String, Long> resolveCourseNos(List<String> courseNos) {
        if (courseNos.isEmpty()) return Map.of();
        return courseMapper.selectList(
                new LambdaQueryWrapper<Course>().in(Course::getCourseNo, courseNos)
        ).stream().collect(Collectors.toMap(Course::getCourseNo, Course::getId));
    }

    private Map<String, Long> resolveStudentNos(List<String> studentNos) {
        if (studentNos.isEmpty()) return Map.of();
        return studentMapper.selectList(
                new LambdaQueryWrapper<Student>().in(Student::getStudentNo, studentNos)
        ).stream().collect(Collectors.toMap(Student::getStudentNo, Student::getId));
    }

    private Map<String, Long> resolveIndicatorNos(List<String> indicatorNos) {
        if (indicatorNos.isEmpty()) return Map.of();
        return indicatorMapper.selectList(
                new LambdaQueryWrapper<EvaluationIndicator>()
                        .in(EvaluationIndicator::getIndicatorNo, indicatorNos)
        ).stream().collect(Collectors.toMap(
                EvaluationIndicator::getIndicatorNo, EvaluationIndicator::getId));
    }

    private Set<String> buildExistingKeySet(List<EvaluationScoreExcelDTO> dtoList,
                                             Map<String, Long> courseNoToId,
                                             Map<String, Long> studentNoToId,
                                             Map<String, Long> indicatorNoToId) {
        // 收集所有可能的 (courseId, studentId) 组合用于批量查重
        List<Long> courseIds = dtoList.stream()
                .map(d -> courseNoToId.get(d.getCourseNo()))
                .filter(Objects::nonNull).distinct().toList();
        List<Long> studentIds = dtoList.stream()
                .map(d -> studentNoToId.get(d.getStudentNo()))
                .filter(Objects::nonNull).distinct().toList();

        if (courseIds.isEmpty() || studentIds.isEmpty()) return Set.of();

        // 批量查询已有的评分记录
        List<EvaluationScore> existing = scoreMapper.selectList(
                new LambdaQueryWrapper<EvaluationScore>()
                        .in(EvaluationScore::getCourseId, courseIds)
                        .in(EvaluationScore::getStudentId, studentIds)
        );

        return existing.stream()
                .map(e -> e.getCourseId() + "-" + e.getStudentId() + "-" + e.getIndicatorId())
                .collect(Collectors.toSet());
    }

    private EvaluationScore toEntity(EvaluationScoreExcelDTO dto,
                                      Map<String, Long> courseNoToId,
                                      Map<String, Long> studentNoToId,
                                      Map<String, Long> indicatorNoToId) {
        EvaluationScore score = new EvaluationScore();
        BeanUtils.copyProperties(dto, score, "courseNo", "studentNo", "indicatorNo", "evaluateTime");

        // 外键解析
        score.setCourseId(courseNoToId.get(dto.getCourseNo()));
        score.setStudentId(studentNoToId.get(dto.getStudentNo()));
        score.setIndicatorId(indicatorNoToId.get(dto.getIndicatorNo()));

        // 时间解析
        if (dto.getEvaluateTime() != null && !dto.getEvaluateTime().isBlank()) {
            try {
                score.setEvaluateTime(LocalDateTime.parse(dto.getEvaluateTime(), DATE_FORMAT));
            } catch (Exception e) {
                score.setEvaluateTime(LocalDateTime.now());
            }
        }

        return score;
    }

    private ImportResultDTO buildResult(GenericExcelListener<?> listener) {
        ImportResultDTO result = new ImportResultDTO();
        result.setTotalRows(listener.getTotalRows());
        result.setSuccessRows(listener.getSuccessCount());
        result.setFailRows(listener.getFailCount());

        if (listener.getFailCount() == 0) {
            result.setStatus(ImportStatus.SUCCESS.getCode());
        } else if (listener.getSuccessCount() == 0) {
            result.setStatus(ImportStatus.FAILED.getCode());
        } else {
            result.setStatus(ImportStatus.PARTIAL.getCode());
        }

        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
