package com.example.aitaes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.*;
import com.example.aitaes.entity.*;
import com.example.aitaes.mapper.*;
import com.example.aitaes.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教学评价计算引擎
 * <p>
 * 核心算法：基于两级指标体系的加权综合评分
 * <pre>
 *   overallScore = Σ (indicator_avgScore × indicator_weight)  // 所有二级指标
 *   categoryScore = Σ (indicator_avgScore × indicator_weight) / categoryWeight  // 归一化到0-100
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final EvaluationScoreMapper scoreMapper;
    private final EvaluationIndicatorMapper indicatorMapper;

    /** 缓存指标树（系统启动后不变） */
    private volatile List<EvaluationIndicator> cachedIndicators;
    private volatile Map<Long, List<EvaluationIndicator>> cachedChildrenMap;

    @Override
    public CourseEvaluationDTO calculateCourseScore(Long courseId, String semester) {
        // 1. 加载课程和教师信息
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "课程不存在: id=" + courseId);
        }
        Teacher teacher = course.getTeacherId() != null
                ? teacherMapper.selectById(course.getTeacherId()) : null;

        // 2. 查询该课程的评价分数
        List<EvaluationScore> scores = queryScores(courseId, null, semester);

        // 3. 加载指标体系
        ensureIndicatorsLoaded();

        // 4. 计算指标得分
        List<IndicatorScoreDTO> indicatorScores = computeIndicatorScores(scores);

        // 5. 计算维度得分
        List<CategoryScoreDTO> categoryScores = computeCategoryScores(indicatorScores);

        // 6. 计算综合得分
        BigDecimal overallScore = indicatorScores.stream()
                .map(IndicatorScoreDTO::getWeightedScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. 统计参评学生数
        long studentCount = scores.stream()
                .map(EvaluationScore::getStudentId)
                .distinct().count();

        return CourseEvaluationDTO.builder()
                .courseId(course.getId())
                .courseNo(course.getCourseNo())
                .courseName(course.getCourseName())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null ? teacher.getName() : null)
                .semester(semester)
                .totalStudents((int) studentCount)
                .overallScore(overallScore)
                .categoryScores(categoryScores)
                .indicatorScores(indicatorScores)
                .build();
    }

    @Override
    public TeacherEvaluationDTO calculateTeacherScore(Long teacherId, String semester) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "教师不存在: id=" + teacherId);
        }

        // 1. 查询该教师的所有课程
        LambdaQueryWrapper<Course> courseQuery = new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, teacherId);
        if (semester != null && !semester.isBlank()) {
            courseQuery.eq(Course::getSemester, semester);
        }
        List<Course> courses = courseMapper.selectList(courseQuery);

        if (courses.isEmpty()) {
            return buildEmptyTeacherDTO(teacher);
        }

        // 2. 逐课程计算评价
        List<CourseEvaluationDTO> courseDetails = new ArrayList<>();
        for (Course course : courses) {
            try {
                courseDetails.add(calculateCourseScore(course.getId(), semester));
            } catch (Exception e) {
                log.warn("计算课程评价失败: courseId={}, msg={}", course.getId(), e.getMessage());
            }
        }

        // 3. 汇总教师整体评价
        return aggregateTeacherScores(teacher, courseDetails);
    }

    @Override
    public List<TeacherEvaluationDTO> getCollegeRanking(String college, String semester) {
        LambdaQueryWrapper<Teacher> query = new LambdaQueryWrapper<Teacher>();
        if (college != null && !college.isBlank()) {
            query.eq(Teacher::getCollege, college);
        }
        List<Teacher> teachers = teacherMapper.selectList(query);

        return teachers.stream()
                .map(t -> calculateTeacherScore(t.getId(), semester))
                .filter(dto -> dto.getCourseCount() > 0)  // 只返回有评教数据的
                .sorted(Comparator.comparing(
                        TeacherEvaluationDTO::getAverageOverallScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<TeacherEvaluationDTO> getSemesterOverview(String semester) {
        return getCollegeRanking(null, semester);
    }

    // ==================== 核心计算逻辑 ====================

    /**
     * 加载评价指标体系（带缓存）
     */
    private void ensureIndicatorsLoaded() {
        if (cachedIndicators == null) {
            synchronized (this) {
                if (cachedIndicators == null) {
                    cachedIndicators = indicatorMapper.selectList(
                            new LambdaQueryWrapper<EvaluationIndicator>()
                                    .orderByAsc(EvaluationIndicator::getSortOrder));
                    cachedChildrenMap = cachedIndicators.stream()
                            .filter(i -> i.getParentId() != null)
                            .collect(Collectors.groupingBy(EvaluationIndicator::getParentId));
                    log.info("评价指标体系已加载，共 {} 条指标", cachedIndicators.size());
                }
            }
        }
    }

    /**
     * 查询评价分数
     */
    private List<EvaluationScore> queryScores(Long courseId, Long studentId, String semester) {
        LambdaQueryWrapper<EvaluationScore> wrapper = new LambdaQueryWrapper<EvaluationScore>()
                .eq(courseId != null, EvaluationScore::getCourseId, courseId)
                .eq(studentId != null, EvaluationScore::getStudentId, studentId)
                .eq(semester != null && !semester.isBlank(), EvaluationScore::getSemester, semester);
        return scoreMapper.selectList(wrapper);
    }

    /**
     * 计算各指标平均得分（按二级指标分组）
     */
    private List<IndicatorScoreDTO> computeIndicatorScores(List<EvaluationScore> scores) {
        List<EvaluationIndicator> indicators = cachedIndicators;
        Map<Long, EvaluationIndicator> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(EvaluationIndicator::getId, i -> i));

        // 按 indicator_id 分组，计算每组的平均分和人数
        Map<Long, List<BigDecimal>> grouped = scores.stream()
                .filter(s -> s.getScore() != null)
                .collect(Collectors.groupingBy(
                        EvaluationScore::getIndicatorId,
                        Collectors.mapping(EvaluationScore::getScore, Collectors.toList())));

        List<IndicatorScoreDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<BigDecimal>> entry : grouped.entrySet()) {
            Long indicatorId = entry.getKey();
            List<BigDecimal> scoreList = entry.getValue();
            EvaluationIndicator indicator = indicatorMap.get(indicatorId);
            if (indicator == null) continue;

            BigDecimal avg = average(scoreList);
            BigDecimal weight = indicator.getWeight() != null ? indicator.getWeight() : BigDecimal.ZERO;

            result.add(IndicatorScoreDTO.builder()
                    .indicatorId(indicator.getId())
                    .indicatorNo(indicator.getIndicatorNo())
                    .indicatorName(indicator.getIndicatorName())
                    .category(indicator.getCategory())
                    .level(indicator.getLevel())
                    .parentId(indicator.getParentId())
                    .avgScore(avg)
                    .weight(weight)
                    .weightedScore(avg.multiply(weight).setScale(4, RoundingMode.HALF_UP))
                    .evaluationCount(scoreList.size())
                    .build());
        }
        return result;
    }

    /**
     * 汇总到四大维度（教学态度/教学内容/教学方法/教学效果）
     */
    private List<CategoryScoreDTO> computeCategoryScores(List<IndicatorScoreDTO> indicatorScores) {
        // 按类别分组
        Map<String, List<IndicatorScoreDTO>> byCategory = indicatorScores.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getCategory() != null ? i.getCategory() : "未分类"));

        // 获取每个类别的一级指标权重
        List<EvaluationIndicator> levelOnes = cachedIndicators.stream()
                .filter(i -> i.getLevel() != null && i.getLevel() == 1)
                .toList();

        List<CategoryScoreDTO> result = new ArrayList<>();
        for (EvaluationIndicator levelOne : levelOnes) {
            String category = levelOne.getCategory();
            BigDecimal categoryWeight = levelOne.getWeight() != null
                    ? levelOne.getWeight() : BigDecimal.ZERO;

            List<IndicatorScoreDTO> items = byCategory.getOrDefault(category, List.of());

            // 该维度下所有二级指标的加权得分之和
            BigDecimal weightedSum = items.stream()
                    .map(IndicatorScoreDTO::getWeightedScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 归一化到 0-100：除以类别权重
            BigDecimal normalizedScore = BigDecimal.ZERO;
            if (categoryWeight.compareTo(BigDecimal.ZERO) > 0) {
                normalizedScore = weightedSum.divide(categoryWeight, 2, RoundingMode.HALF_UP);
            }

            result.add(CategoryScoreDTO.builder()
                    .category(category)
                    .weight(categoryWeight)
                    .score(normalizedScore)
                    .weightedScore(weightedSum.setScale(4, RoundingMode.HALF_UP))
                    .build());
        }
        return result;
    }

    /**
     * 汇总教师综合评价
     */
    private TeacherEvaluationDTO aggregateTeacherScores(Teacher teacher,
                                                         List<CourseEvaluationDTO> courseDetails) {
        if (courseDetails.isEmpty()) {
            return buildEmptyTeacherDTO(teacher);
        }

        // 平均综合得分
        BigDecimal avgOverall = courseDetails.stream()
                .map(CourseEvaluationDTO::getOverallScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(courseDetails.size()), 2, RoundingMode.HALF_UP);

        // 按维度汇总
        List<CategoryScoreDTO> categoryAverages = computeCategoryAverages(courseDetails);

        return TeacherEvaluationDTO.builder()
                .teacherId(teacher.getId())
                .teacherNo(teacher.getTeacherNo())
                .name(teacher.getName())
                .college(teacher.getCollege())
                .title(teacher.getTitle())
                .courseCount(courseDetails.size())
                .averageOverallScore(avgOverall)
                .categoryAverages(categoryAverages)
                .courseDetails(courseDetails)
                .build();
    }

    private List<CategoryScoreDTO> computeCategoryAverages(List<CourseEvaluationDTO> courseDetails) {
        // 按类别名称收集所有课程的类别得分
        Map<String, List<BigDecimal>> byCategory = new LinkedHashMap<>();
        for (CourseEvaluationDTO course : courseDetails) {
            if (course.getCategoryScores() != null) {
                for (CategoryScoreDTO cat : course.getCategoryScores()) {
                    byCategory.computeIfAbsent(cat.getCategory(), k -> new ArrayList<>())
                            .add(cat.getScore());
                }
            }
        }

        return byCategory.entrySet().stream().map(entry -> {
            BigDecimal avg = average(entry.getValue());
            return CategoryScoreDTO.builder()
                    .category(entry.getKey())
                    .score(avg)
                    .build();
        }).toList();
    }

    private TeacherEvaluationDTO buildEmptyTeacherDTO(Teacher teacher) {
        return TeacherEvaluationDTO.builder()
                .teacherId(teacher.getId())
                .teacherNo(teacher.getTeacherNo())
                .name(teacher.getName())
                .college(teacher.getCollege())
                .title(teacher.getTitle())
                .courseCount(0)
                .averageOverallScore(BigDecimal.ZERO)
                .categoryAverages(List.of())
                .courseDetails(List.of())
                .build();
    }

    // ==================== 工具方法 ====================

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(values.size()), 2, RoundingMode.HALF_UP);
    }
}
