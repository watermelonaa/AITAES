package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.CourseExcelDTO;
import com.example.aitaes.entity.Course;
import com.example.aitaes.entity.Teacher;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.CourseMapper;
import com.example.aitaes.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程数据导入策略
 * 需要解析 teacherNo → teacherId 外键
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseImportStrategy implements ImportStrategy {

    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;

    @Override
    public ImportType getSupportedType() {
        return ImportType.COURSE;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<CourseExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, CourseExcelDTO.class, listener)
                .excelType(getExcelType(originalFilename))
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<CourseExcelDTO> dtoList) {
        // 1. 预查重复课程编号
        List<String> courseNos = dtoList.stream()
                .map(CourseExcelDTO::getCourseNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!courseNos.isEmpty()) {
            existingNos = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>()
                            .in(Course::getCourseNo, courseNos)
            ).stream().map(Course::getCourseNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 解析 teacherNo → teacherId
        List<String> teacherNos = dtoList.stream()
                .map(CourseExcelDTO::getTeacherNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Long> teacherNoToId = Map.of();
        if (!teacherNos.isEmpty()) {
            teacherNoToId = teacherMapper.selectList(
                    new LambdaQueryWrapper<Teacher>()
                            .in(Teacher::getTeacherNo, teacherNos)
            ).stream().collect(Collectors.toMap(Teacher::getTeacherNo, Teacher::getId));
        }

        // 3. 过滤 + 转换
        Map<String, Long> finalTeacherNoToId = teacherNoToId;
        List<Course> courses = dtoList.stream()
                .filter(dto -> dto.getCourseNo() != null && !dto.getCourseNo().isBlank())
                .filter(dto -> !existingNos.contains(dto.getCourseNo()))
                .map(dto -> toEntity(dto, finalTeacherNoToId))
                .toList();

        // 4. 批量插入
        if (!courses.isEmpty()) {
            courses.forEach(courseMapper::insert);
            log.debug("批量插入课程数据 {} 条", courses.size());
        }
    }

    private Course toEntity(CourseExcelDTO dto, Map<String, Long> teacherNoToId) {
        Course course = new Course();
        BeanUtils.copyProperties(dto, course, "teacherNo");
        // 解析外键
        if (dto.getTeacherNo() != null && teacherNoToId.containsKey(dto.getTeacherNo())) {
            course.setTeacherId(teacherNoToId.get(dto.getTeacherNo()));
        }
        return course;
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
