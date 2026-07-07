package com.example.aitaes.controller;

import com.example.aitaes.common.Result;
import com.example.aitaes.dto.CourseEvaluationDTO;
import com.example.aitaes.dto.TeacherEvaluationDTO;
import com.example.aitaes.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学评价控制器
 * <p>
 * 提供课程评价、教师评价、学院排名等查询接口。
 * 所有计算在请求时实时执行（指标体系已缓存）。
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * 获取单门课程的综合评价
     *
     * @param courseId 课程ID
     * @param semester 学期（可选，如：2025-2026-1）
     */
    @GetMapping("/course/{courseId}")
    public Result<CourseEvaluationDTO> getCourseEvaluation(
            @PathVariable Long courseId,
            @RequestParam(required = false) String semester) {
        CourseEvaluationDTO result = evaluationService.calculateCourseScore(courseId, semester);
        return Result.success(result);
    }

    /**
     * 获取教师综合评价（汇总该教师所有课程的评教结果）
     *
     * @param teacherId 教师ID
     * @param semester  学期（可选）
     */
    @GetMapping("/teacher/{teacherId}")
    public Result<TeacherEvaluationDTO> getTeacherEvaluation(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String semester) {
        TeacherEvaluationDTO result = evaluationService.calculateTeacherScore(teacherId, semester);
        return Result.success(result);
    }

    /**
     * 获取学院教师评价排名
     *
     * @param college  学院名称（可选，不传则全量）
     * @param semester 学期（可选）
     */
    @GetMapping("/college")
    public Result<List<TeacherEvaluationDTO>> getCollegeRanking(
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String semester) {
        List<TeacherEvaluationDTO> result = evaluationService.getCollegeRanking(college, semester);
        return Result.success(result);
    }

    /**
     * 获取学期全院系概览
     *
     * @param semester 学期（如：2025-2026-1）
     */
    @GetMapping("/semester/{semester}")
    public Result<List<TeacherEvaluationDTO>> getSemesterOverview(
            @PathVariable String semester) {
        List<TeacherEvaluationDTO> result = evaluationService.getSemesterOverview(semester);
        return Result.success(result);
    }
}
