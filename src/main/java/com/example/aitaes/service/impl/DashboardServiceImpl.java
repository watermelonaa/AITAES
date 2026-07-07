package com.example.aitaes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.*;
import com.example.aitaes.entity.*;
import com.example.aitaes.mapper.*;
import com.example.aitaes.service.DashboardService;
import com.example.aitaes.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘数据聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final EvaluationScoreMapper scoreMapper;
    private final EvaluationService evaluationService;

    @Override
    public OverviewStatsDTO getOverview(String semester) {
        Long totalTeachers = teacherMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalStudents = studentMapper.selectCount(new LambdaQueryWrapper<>());
        Long totalCourses = courseMapper.selectCount(new LambdaQueryWrapper<>());

        LambdaQueryWrapper<EvaluationScore> scoreQuery = new LambdaQueryWrapper<>();
        if (semester != null && !semester.isBlank()) {
            scoreQuery.eq(EvaluationScore::getSemester, semester);
        }
        Long totalEvaluations = scoreMapper.selectCount(scoreQuery);

        // 计算所有已评价课程的综合平均分
        List<Long> evaluatedCourseIds = scoreMapper.selectList(
                new LambdaQueryWrapper<EvaluationScore>()
                        .select(EvaluationScore::getCourseId)
                        .eq(semester != null && !semester.isBlank(),
                                EvaluationScore::getSemester, semester)
                        .groupBy(EvaluationScore::getCourseId)
        ).stream().map(EvaluationScore::getCourseId).distinct().toList();

        BigDecimal avgScore = BigDecimal.ZERO;
        if (!evaluatedCourseIds.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (Long courseId : evaluatedCourseIds) {
                try {
                    CourseEvaluationDTO eval = evaluationService.calculateCourseScore(courseId, semester);
                    sum = sum.add(eval.getOverallScore());
                    count++;
                } catch (Exception e) {
                    log.warn("计算课程评分失败 courseId={}", courseId);
                }
            }
            if (count > 0) {
                avgScore = sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
            }
        }

        return OverviewStatsDTO.builder()
                .totalTeachers(totalTeachers)
                .totalStudents(totalStudents)
                .totalCourses(totalCourses)
                .totalEvaluations(totalEvaluations)
                .averageOverallScore(avgScore)
                .evaluatedCourseCount((long) evaluatedCourseIds.size())
                .build();
    }

    @Override
    public List<ScoreDistributionDTO> getScoreDistribution(String semester) {
        // 计算所有课程得分并分桶
        List<Long> evaluatedCourseIds = scoreMapper.selectList(
                new LambdaQueryWrapper<EvaluationScore>()
                        .select(EvaluationScore::getCourseId)
                        .eq(semester != null && !semester.isBlank(),
                                EvaluationScore::getSemester, semester)
                        .groupBy(EvaluationScore::getCourseId)
        ).stream().map(EvaluationScore::getCourseId).distinct().toList();

        List<BigDecimal> courseScores = new ArrayList<>();
        for (Long courseId : evaluatedCourseIds) {
            try {
                CourseEvaluationDTO eval = evaluationService.calculateCourseScore(courseId, semester);
                courseScores.add(eval.getOverallScore());
            } catch (Exception ignored) { /* skip */ }
        }

        // 分桶统计
        int total = courseScores.size();
        String[] labels = {"<60", "60-69", "70-79", "80-89", "≥90"};
        double[] thresholds = {60, 70, 80, 90};

        List<ScoreDistributionDTO> result = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            long count;
            if (i == 0) {
                count = courseScores.stream()
                        .filter(s -> s.compareTo(new BigDecimal("60")) < 0).count();
            } else if (i == labels.length - 1) {
                count = courseScores.stream()
                        .filter(s -> s.compareTo(new BigDecimal("90")) >= 0).count();
            } else {
                BigDecimal low = new BigDecimal(String.valueOf((int) thresholds[i - 1]));
                BigDecimal high = new BigDecimal(String.valueOf((int) thresholds[i]));
                count = courseScores.stream()
                        .filter(s -> s.compareTo(low) >= 0 && s.compareTo(high) < 0).count();
            }
            result.add(ScoreDistributionDTO.builder()
                    .label(labels[i])
                    .count(count)
                    .percentage(total > 0 ? Math.round(count * 10000.0 / total) / 100.0 : 0.0)
                    .build());
        }
        return result;
    }

    @Override
    public TrendDTO getTrend() {
        // 获取所有学期
        List<String> semesters = scoreMapper.selectList(
                new LambdaQueryWrapper<EvaluationScore>()
                        .select(EvaluationScore::getSemester)
                        .groupBy(EvaluationScore::getSemester)
                        .orderByAsc(EvaluationScore::getSemester)
        ).stream().map(EvaluationScore::getSemester).filter(Objects::nonNull).distinct().toList();

        List<BigDecimal> overallScores = new ArrayList<>();
        Map<String, List<BigDecimal>> categoryMap = new LinkedHashMap<>();

        for (String semester : semesters) {
            List<Long> courseIds = scoreMapper.selectList(
                    new LambdaQueryWrapper<EvaluationScore>()
                            .select(EvaluationScore::getCourseId)
                            .eq(EvaluationScore::getSemester, semester)
                            .groupBy(EvaluationScore::getCourseId)
            ).stream().map(EvaluationScore::getCourseId).distinct().toList();

            // 学期综合分
            BigDecimal semesterAvg = BigDecimal.ZERO;
            Map<String, List<BigDecimal>> catScores = new LinkedHashMap<>();
            int courseCount = 0;
            for (Long courseId : courseIds) {
                try {
                    CourseEvaluationDTO eval = evaluationService.calculateCourseScore(courseId, semester);
                    semesterAvg = semesterAvg.add(eval.getOverallScore());
                    courseCount++;
                    if (eval.getCategoryScores() != null) {
                        for (CategoryScoreDTO cat : eval.getCategoryScores()) {
                            catScores.computeIfAbsent(cat.getCategory(), k -> new ArrayList<>())
                                    .add(cat.getScore());
                        }
                    }
                } catch (Exception ignored) { /* skip */ }
            }
            if (courseCount > 0) {
                semesterAvg = semesterAvg.divide(new BigDecimal(courseCount), 2, RoundingMode.HALF_UP);
            }
            overallScores.add(semesterAvg);

            // 各维度平均
            for (Map.Entry<String, List<BigDecimal>> entry : catScores.entrySet()) {
                BigDecimal avg = entry.getValue().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP);
                categoryMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(avg);
            }
        }

        List<TrendDTO.CategoryTrend> trends = categoryMap.entrySet().stream()
                .map(e -> TrendDTO.CategoryTrend.builder()
                        .category(e.getKey()).scores(e.getValue()).build())
                .toList();

        return TrendDTO.builder()
                .semesters(semesters)
                .overallScores(overallScores)
                .categoryTrends(trends)
                .build();
    }

    @Override
    public DashboardDTO getDashboard(String semester) {
        OverviewStatsDTO overview = getOverview(semester);
        List<ScoreDistributionDTO> distribution = getScoreDistribution(semester);
        TrendDTO trend = getTrend();

        // 学院排名
        List<Teacher> allTeachers = teacherMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, List<Teacher>> byCollege = allTeachers.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCollege() != null ? t.getCollege() : "未知"));

        List<DashboardDTO.CollegeRankItem> collegeRanking = new ArrayList<>();
        for (Map.Entry<String, List<Teacher>> entry : byCollege.entrySet()) {
            String college = entry.getKey();
            BigDecimal collegeAvg = BigDecimal.ZERO;
            int teacherCount = 0;
            long evalCount = 0;
            for (Teacher teacher : entry.getValue()) {
                try {
                    TeacherEvaluationDTO tEval = evaluationService.calculateTeacherScore(
                            teacher.getId(), semester);
                    if (tEval.getCourseCount() > 0) {
                        collegeAvg = collegeAvg.add(tEval.getAverageOverallScore());
                        teacherCount++;
                        evalCount += tEval.getCourseCount();
                    }
                } catch (Exception ignored) { /* skip */ }
            }
            if (teacherCount > 0) {
                collegeAvg = collegeAvg.divide(new BigDecimal(teacherCount), 2, RoundingMode.HALF_UP);
                collegeRanking.add(DashboardDTO.CollegeRankItem.builder()
                        .college(college)
                        .teacherCount((long) entry.getValue().size())
                        .evaluatedCourseCount(evalCount)
                        .avgScore(collegeAvg)
                        .build());
            }
        }
        collegeRanking.sort((a, b) -> b.getAvgScore().compareTo(a.getAvgScore()));

        return DashboardDTO.builder()
                .overview(overview)
                .scoreDistribution(distribution)
                .trend(trend)
                .collegeRanking(collegeRanking.size() > 10
                        ? collegeRanking.subList(0, 10) : collegeRanking)
                .build();
    }
}
