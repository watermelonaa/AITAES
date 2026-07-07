package com.example.aitaes.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.CategoryScoreDTO;
import com.example.aitaes.dto.CourseEvaluationDTO;
import com.example.aitaes.dto.TeacherEvaluationDTO;
import com.example.aitaes.dto.excel.CourseEvalExportDTO;
import com.example.aitaes.dto.excel.TeacherEvalExportDTO;
import com.example.aitaes.entity.Course;
import com.example.aitaes.mapper.CourseMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导出服务
 * 使用 EasyExcel 将评价数据导出为 .xlsx 文件并通过 HTTP 响应下载
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final EvaluationService evaluationService;
    private final CourseMapper courseMapper;

    /** 导出单门课程评价明细 */
    public void exportCourseEvaluation(Long courseId, String semester,
                                        HttpServletResponse response) throws IOException {
        CourseEvaluationDTO eval = evaluationService.calculateCourseScore(courseId, semester);

        CourseEvalExportDTO dto = toCourseExportDTO(eval);
        writeExcel(response, "课程评价_" + eval.getCourseName(),
                List.of(dto), CourseEvalExportDTO.class);
    }

    /** 导出某教师全部课程评价 */
    public void exportTeacherCourses(Long teacherId, String semester,
                                      HttpServletResponse response) throws IOException {
        TeacherEvaluationDTO teacherEval = evaluationService.calculateTeacherScore(teacherId, semester);

        List<CourseEvalExportDTO> rows = new ArrayList<>();
        if (teacherEval.getCourseDetails() != null) {
            for (CourseEvaluationDTO cd : teacherEval.getCourseDetails()) {
                rows.add(toCourseExportDTO(cd));
            }
        }

        writeExcel(response, "教师评价_" + teacherEval.getName(),
                rows, CourseEvalExportDTO.class);
    }

    /** 导出学院排名 */
    public void exportCollegeRanking(String college, String semester,
                                      HttpServletResponse response) throws IOException {
        List<TeacherEvaluationDTO> ranking = evaluationService.getCollegeRanking(college, semester);

        List<TeacherEvalExportDTO> rows = new ArrayList<>();
        for (TeacherEvaluationDTO te : ranking) {
            rows.add(toTeacherExportDTO(te));
        }

        String fileName = (college != null ? college : "全校") + "_教师排名";
        writeExcel(response, fileName, rows, TeacherEvalExportDTO.class);
    }

    /** 导出指定学期的全部课程评价 */
    public void exportSemesterCourses(String semester, HttpServletResponse response)
            throws IOException {
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getSemester, semester));

        List<CourseEvalExportDTO> rows = new ArrayList<>();
        for (Course course : courses) {
            try {
                CourseEvaluationDTO eval = evaluationService.calculateCourseScore(
                        course.getId(), semester);
                rows.add(toCourseExportDTO(eval));
            } catch (Exception e) {
                // 跳过无评价数据的课程
            }
        }

        writeExcel(response, "学期评价_" + semester, rows, CourseEvalExportDTO.class);
    }

    // ==================== 工具方法 ====================

    private CourseEvalExportDTO toCourseExportDTO(CourseEvaluationDTO eval) {
        return CourseEvalExportDTO.builder()
                .courseNo(eval.getCourseNo())
                .courseName(eval.getCourseName())
                .teacherName(eval.getTeacherName())
                .semester(eval.getSemester())
                .studentCount(eval.getTotalStudents())
                .overallScore(eval.getOverallScore())
                .attitudeScore(getCategoryScore(eval, "教学态度"))
                .contentScore(getCategoryScore(eval, "教学内容"))
                .methodScore(getCategoryScore(eval, "教学方法"))
                .effectScore(getCategoryScore(eval, "教学效果"))
                .build();
    }

    private TeacherEvalExportDTO toTeacherExportDTO(TeacherEvaluationDTO te) {
        return TeacherEvalExportDTO.builder()
                .teacherNo(te.getTeacherNo())
                .name(te.getName())
                .college(te.getCollege())
                .title(te.getTitle())
                .courseCount(te.getCourseCount())
                .avgScore(te.getAverageOverallScore())
                .attitudeScore(getCategoryAvg(te, "教学态度"))
                .contentScore(getCategoryAvg(te, "教学内容"))
                .methodScore(getCategoryAvg(te, "教学方法"))
                .effectScore(getCategoryAvg(te, "教学效果"))
                .build();
    }

    private BigDecimal getCategoryScore(CourseEvaluationDTO eval, String category) {
        if (eval.getCategoryScores() == null) return BigDecimal.ZERO;
        return eval.getCategoryScores().stream()
                .filter(c -> category.equals(c.getCategory()))
                .findFirst()
                .map(CategoryScoreDTO::getScore)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getCategoryAvg(TeacherEvaluationDTO eval, String category) {
        if (eval.getCategoryAverages() == null) return BigDecimal.ZERO;
        return eval.getCategoryAverages().stream()
                .filter(c -> category.equals(c.getCategory()))
                .findFirst()
                .map(CategoryScoreDTO::getScore)
                .orElse(BigDecimal.ZERO);
    }

    private <T> void writeExcel(HttpServletResponse response, String fileName,
                                 List<T> data, Class<T> clazz) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + encoded + ".xlsx");

        EasyExcel.write(response.getOutputStream(), clazz)
                .sheet("Sheet1")
                .doWrite(data);
    }
}
