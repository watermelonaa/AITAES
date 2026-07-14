package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.TeacherExcelDTO;
import com.example.aitaes.entity.Teacher;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师数据导入策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherImportStrategy implements ImportStrategy {

    private final TeacherMapper teacherMapper;

    @Override
    public ImportType getSupportedType() {
        return ImportType.TEACHER;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<TeacherExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, TeacherExcelDTO.class, listener)
                .excelType(getExcelType(originalFilename))
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<TeacherExcelDTO> dtoList) {
        // 1. 预查重复工号
        List<String> teacherNos = dtoList.stream()
                .map(TeacherExcelDTO::getTeacherNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!teacherNos.isEmpty()) {
            existingNos = teacherMapper.selectList(
                    new LambdaQueryWrapper<Teacher>()
                            .in(Teacher::getTeacherNo, teacherNos)
            ).stream().map(Teacher::getTeacherNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 过滤重复，转换为实体
        List<Teacher> teachers = dtoList.stream()
                .filter(dto -> dto.getTeacherNo() != null && !dto.getTeacherNo().isBlank())
                .filter(dto -> !existingNos.contains(dto.getTeacherNo()))
                .map(this::toEntity)
                .toList();

        // 3. 批量插入
        if (!teachers.isEmpty()) {
            teachers.forEach(teacherMapper::insert);
            log.debug("批量插入教师数据 {} 条", teachers.size());
        }
    }

    private Teacher toEntity(TeacherExcelDTO dto) {
        Teacher teacher = new Teacher();
        BeanUtils.copyProperties(dto, teacher);
        return teacher;
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

        // 只返回前 100 条错误
        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
