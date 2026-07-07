package com.example.aitaes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.CourseEvaluationDTO;
import com.example.aitaes.dto.TeacherEvaluationDTO;
import com.example.aitaes.entity.Report;
import com.example.aitaes.mapper.ReportMapper;
import com.example.aitaes.service.EvaluationService;
import com.example.aitaes.service.ReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价报告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper;

    @Override
    public Report generateCourseReport(Long courseId, String semester) {
        CourseEvaluationDTO eval = evaluationService.calculateCourseScore(courseId, semester);

        Report report = new Report();
        report.setReportName(eval.getCourseName() + " - 评价报告");
        report.setReportType("COURSE");
        report.setSemester(semester);
        report.setCourseId(courseId);
        report.setTeacherId(eval.getTeacherId());
        report.setSummary(String.format("%s 综合得分 %.2f 分（参评学生 %d 人）",
                eval.getCourseName(), eval.getOverallScore(), eval.getTotalStudents()));
        report.setReportData(toJson(eval));
        report.setGenerateTime(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("生成课程报告: id={}, courseId={}", report.getId(), courseId);
        return report;
    }

    @Override
    public Report generateTeacherReport(Long teacherId, String semester) {
        TeacherEvaluationDTO eval = evaluationService.calculateTeacherScore(teacherId, semester);

        Report report = new Report();
        report.setReportName(eval.getName() + " - 教师评价报告");
        report.setReportType("TEACHER");
        report.setSemester(semester);
        report.setTeacherId(teacherId);
        report.setSummary(String.format("%s (%s) 综合平均分 %.2f 分（%d 门课程）",
                eval.getName(), eval.getCollege(), eval.getAverageOverallScore(),
                eval.getCourseCount()));
        report.setReportData(toJson(eval));
        report.setGenerateTime(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("生成教师报告: id={}, teacherId={}", report.getId(), teacherId);
        return report;
    }

    @Override
    public Report generateCollegeReport(String college, String semester) {
        List<TeacherEvaluationDTO> ranking = evaluationService.getCollegeRanking(college, semester);

        Report report = new Report();
        report.setReportName((college != null ? college : "全校") + " - 学院评价报告");
        report.setReportType("COLLEGE");
        report.setSemester(semester);
        report.setSummary(String.format("共 %d 名教师参评", ranking.size()));
        report.setReportData(toJson(ranking));
        report.setGenerateTime(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("生成学院报告: id={}, college={}", report.getId(), college);
        return report;
    }

    @Override
    public IPage<Report> list(int pageNum, int pageSize, String reportType, String semester) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (reportType != null && !reportType.isBlank()) {
            wrapper.eq(Report::getReportType, reportType);
        }
        if (semester != null && !semester.isBlank()) {
            wrapper.eq(Report::getSemester, semester);
        }
        wrapper.orderByDesc(Report::getGenerateTime);
        return reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Report getById(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "报告不存在: id=" + id);
        }
        return report;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        reportMapper.deleteById(id);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }
}
