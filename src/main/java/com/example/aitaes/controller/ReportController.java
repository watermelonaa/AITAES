package com.example.aitaes.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.common.Result;
import com.example.aitaes.entity.Report;
import com.example.aitaes.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 报告管理控制器
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 生成课程评价报告 */
    @PostMapping("/generate/course/{courseId}")
    public Result<Report> generateCourseReport(
            @PathVariable Long courseId,
            @RequestParam(required = false) String semester) {
        return Result.success(reportService.generateCourseReport(courseId, semester));
    }

    /** 生成教师评价报告 */
    @PostMapping("/generate/teacher/{teacherId}")
    public Result<Report> generateTeacherReport(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String semester) {
        return Result.success(reportService.generateTeacherReport(teacherId, semester));
    }

    /** 生成学院报告 */
    @PostMapping("/generate/college")
    public Result<Report> generateCollegeReport(
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String semester) {
        return Result.success(reportService.generateCollegeReport(college, semester));
    }

    /** 分页查询报告列表 */
    @GetMapping
    public Result<IPage<Report>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String semester) {
        return Result.success(reportService.list(pageNum, pageSize, reportType, semester));
    }

    /** 查看报告详情 */
    @GetMapping("/{id}")
    public Result<Report> getById(@PathVariable Long id) {
        return Result.success(reportService.getById(id));
    }

    /** 删除报告 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.success();
    }
}
