package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.StudentExcelDTO;
import com.example.aitaes.entity.Student;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.StudentMapper;
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
 * 学生数据导入策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentImportStrategy implements ImportStrategy {

    private final StudentMapper studentMapper;

    @Override
    public ImportType getSupportedType() {
        return ImportType.STUDENT;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<StudentExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, StudentExcelDTO.class, listener)
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<StudentExcelDTO> dtoList) {
        // 1. 预查重复学号
        List<String> studentNos = dtoList.stream()
                .map(StudentExcelDTO::getStudentNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!studentNos.isEmpty()) {
            existingNos = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>()
                            .in(Student::getStudentNo, studentNos)
            ).stream().map(Student::getStudentNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 过滤重复，转换为实体
        List<Student> students = dtoList.stream()
                .filter(dto -> dto.getStudentNo() != null && !dto.getStudentNo().isBlank())
                .filter(dto -> !existingNos.contains(dto.getStudentNo()))
                .map(this::toEntity)
                .toList();

        // 3. 批量插入
        if (!students.isEmpty()) {
            students.forEach(studentMapper::insert);
            log.debug("批量插入学生数据 {} 条", students.size());
        }
    }

    private Student toEntity(StudentExcelDTO dto) {
        Student student = new Student();
        BeanUtils.copyProperties(dto, student);
        return student;
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
