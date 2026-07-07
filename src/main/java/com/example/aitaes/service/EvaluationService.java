package com.example.aitaes.service;

import com.example.aitaes.dto.CourseEvaluationDTO;
import com.example.aitaes.dto.TeacherEvaluationDTO;

import java.util.List;

/**
 * 教学评价计算服务接口
 */
public interface EvaluationService {

    /** 计算单门课程的综合评价（可按学期过滤） */
    CourseEvaluationDTO calculateCourseScore(Long courseId, String semester);

    /** 获取教师综合评价（汇总其所有课程） */
    TeacherEvaluationDTO calculateTeacherScore(Long teacherId, String semester);

    /** 按学院获取所有教师的评价排名 */
    List<TeacherEvaluationDTO> getCollegeRanking(String college, String semester);

    /** 获取指定学期的全院系概览 */
    List<TeacherEvaluationDTO> getSemesterOverview(String semester);
}
