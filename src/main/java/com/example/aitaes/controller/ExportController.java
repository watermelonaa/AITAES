package com.example.aitaes.controller;

import com.example.aitaes.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Excel 导出控制器 — 文件下载
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /** 导出单门课程评价 */
    @GetMapping("/course/{courseId}")
    public void exportCourseEvaluation(
            @PathVariable Long courseId,
            @RequestParam(required = false) String semester,
            HttpServletResponse response) throws IOException {
        log.info("导出课程评价: courseId={}, semester={}", courseId, semester);
        exportService.exportCourseEvaluation(courseId, semester, response);
    }

    /** 导出教师全部课程评价 */
    @GetMapping("/teacher/{teacherId}")
    public void exportTeacherCourses(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String semester,
            HttpServletResponse response) throws IOException {
        log.info("导出教师评价: teacherId={}, semester={}", teacherId, semester);
        exportService.exportTeacherCourses(teacherId, semester, response);
    }

    /** 导出学院排名 */
    @GetMapping("/college")
    public void exportCollegeRanking(
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String semester,
            HttpServletResponse response) throws IOException {
        log.info("导出学院排名: college={}, semester={}", college, semester);
        exportService.exportCollegeRanking(college, semester, response);
    }

    /** 导出指定学期的全部课程评价 */
    @GetMapping("/semester/{semester}")
    public void exportSemesterCourses(
            @PathVariable String semester,
            HttpServletResponse response) throws IOException {
        log.info("导出学期课程评价: semester={}", semester);
        exportService.exportSemesterCourses(semester, response);
    }
}
