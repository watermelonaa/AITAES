package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.AttendanceExcelDTO;
import com.example.aitaes.entity.Attendance;
import com.example.aitaes.entity.Student;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.AttendanceMapper;
import com.example.aitaes.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 考勤记录导入策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceImportStrategy implements ImportStrategy {

    private final AttendanceMapper attendanceMapper;
    private final StudentMapper studentMapper;

    private Long courseId;
    private String semester;

    @Override
    public ImportType getSupportedType() {
        return ImportType.ATTENDANCE;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<AttendanceExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, AttendanceExcelDTO.class, listener)
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<AttendanceExcelDTO> dtoList) {
        for (AttendanceExcelDTO dto : dtoList) {
            try {
                if (dto.getStudentNo() == null || dto.getStudentNo().isBlank()) continue;

                Student student = studentMapper.selectOne(
                        new LambdaQueryWrapper<Student>()
                                .eq(Student::getStudentNo, dto.getStudentNo()));
                if (student == null) continue;

                Attendance att = new Attendance();
                att.setCourseId(courseId);
                att.setStudentId(student.getId());
                att.setStatus(dto.getStatus());
                att.setWeekNo(dto.getWeekNo());
                att.setPeriod(dto.getPeriod());
                att.setSemester(semester);
                att.setRemark(dto.getRemark());

                if (dto.getAttendanceDate() != null) {
                    try {
                        att.setAttendanceDate(LocalDate.parse(dto.getAttendanceDate(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    } catch (Exception e) {
                        att.setAttendanceDate(LocalDate.now());
                    }
                }

                // 检查是否已存在（同课程+同学生+同日期）
                Attendance existing = attendanceMapper.selectOne(
                        new LambdaQueryWrapper<Attendance>()
                                .eq(Attendance::getCourseId, courseId)
                                .eq(Attendance::getStudentId, student.getId())
                                .eq(Attendance::getAttendanceDate, att.getAttendanceDate()));
                if (existing == null) {
                    attendanceMapper.insert(att);
                }
            } catch (Exception e) {
                log.warn("考勤记录插入失败: {}", e.getMessage());
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
