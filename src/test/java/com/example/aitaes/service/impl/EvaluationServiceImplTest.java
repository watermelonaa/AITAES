package com.example.aitaes.service.impl;

import com.example.aitaes.common.BusinessException;
import com.example.aitaes.dto.CourseEvaluationDTO;
import com.example.aitaes.dto.TeacherEvaluationDTO;
import com.example.aitaes.entity.*;
import com.example.aitaes.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 评价计算引擎单元测试
 *
 * 测试方法：等价类划分、边界值分析、场景法
 * 测试框架：JUnit 5 + Mockito
 * 模式：Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("评价计算引擎")
class EvaluationServiceImplTest {

    @Mock private CourseMapper courseMapper;
    @Mock private TeacherMapper teacherMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private EvaluationScoreMapper scoreMapper;
    @Mock private EvaluationIndicatorMapper indicatorMapper;

    @InjectMocks
    private EvaluationServiceImpl service;

    // ===== 测试数据工厂方法 =====

    /** 创建标准指标体系（4个一级指标 + 各4个二级指标） */
    private List<EvaluationIndicator> buildIndicators() {
        return List.of(
                // 一级指标
                indicator(1L, "I01", "教学态度", "教学态度", 0.2500, null, 1),
                indicator(2L, "I02", "教学内容", "教学内容", 0.2500, null, 1),
                indicator(3L, "I03", "教学方法", "教学方法", 0.2500, null, 1),
                indicator(4L, "I04", "教学效果", "教学效果", 0.2500, null, 1),
                // 二级指标 — 教学态度(4个)
                indicator(5L,  "I01-01", "备课充分",     "教学态度", 0.0625, 1L, 2),
                indicator(6L,  "I01-02", "按时上下课",   "教学态度", 0.0625, 1L, 2),
                indicator(7L,  "I01-03", "课堂纪律管理", "教学态度", 0.0625, 1L, 2),
                indicator(8L,  "I01-04", "耐心答疑",     "教学态度", 0.0625, 1L, 2),
                // 二级指标 — 教学内容(4个)
                indicator(9L,  "I02-01", "内容充实",     "教学内容", 0.0625, 2L, 2),
                indicator(10L, "I02-02", "理论联系实际", "教学内容", 0.0625, 2L, 2),
                indicator(11L, "I02-03", "内容前沿",     "教学内容", 0.0625, 2L, 2),
                indicator(12L, "I02-04", "重点突出",     "教学内容", 0.0625, 2L, 2),
                // 二级指标 — 教学方法(4个)
                indicator(13L, "I03-01", "方法灵活",     "教学方法", 0.0625, 3L, 2),
                indicator(14L, "I03-02", "启发式教学",   "教学方法", 0.0625, 3L, 2),
                indicator(15L, "I03-03", "课堂互动",     "教学方法", 0.0625, 3L, 2),
                indicator(16L, "I03-04", "信息技术运用", "教学方法", 0.0625, 3L, 2),
                // 二级指标 — 教学效果(4个)
                indicator(17L, "I04-01", "知识掌握",     "教学效果", 0.0625, 4L, 2),
                indicator(18L, "I04-02", "解决问题能力", "教学效果", 0.0625, 4L, 2),
                indicator(19L, "I04-03", "学习兴趣激发", "教学效果", 0.0625, 4L, 2),
                indicator(20L, "I04-04", "整体满意度",   "教学效果", 0.0625, 4L, 2)
        );
    }

    private EvaluationIndicator indicator(Long id, String no, String name,
                                          String category, double weight, Long parentId, int level) {
        EvaluationIndicator i = new EvaluationIndicator();
        i.setId(id);
        i.setIndicatorNo(no);
        i.setIndicatorName(name);
        i.setCategory(category);
        i.setWeight(BigDecimal.valueOf(weight));
        i.setParentId(parentId);
        i.setLevel(level);
        return i;
    }

    /** 创建一个评分记录 */
    private EvaluationScore score(Long courseId, Long studentId, Long indicatorId, double scoreVal) {
        EvaluationScore s = new EvaluationScore();
        s.setCourseId(courseId);
        s.setStudentId(studentId);
        s.setIndicatorId(indicatorId);
        s.setScore(BigDecimal.valueOf(scoreVal));
        s.setSemester("2025-2026-1");
        return s;
    }

    /** 创建课程 */
    private Course course(Long id, String no, String name, Long teacherId) {
        Course c = new Course();
        c.setId(id);
        c.setCourseNo(no);
        c.setCourseName(name);
        c.setTeacherId(teacherId);
        c.setSemester("2025-2026-1");
        return c;
    }

    /** 创建教师 */
    private Teacher teacher(Long id, String no, String name, String college, String title) {
        Teacher t = new Teacher();
        t.setId(id);
        t.setTeacherNo(no);
        t.setName(name);
        t.setCollege(college);
        t.setTitle(title);
        return t;
    }

    // ===== 实际测试用例 =====

    @Nested
    @DisplayName("课程评价 - calculateCourseScore")
    class CourseEvaluation {

        /**
         * 等价类：有效课程ID + 有效学期 → 正常返回评价结果
         */
        @Test
        @DisplayName("多学生多指标评分 → 正常计算综合得分和维度得分")
        void shouldCalculateCorrectly_WhenMultipleStudentsRate() {
            // ===== Given: 准备数据 =====
            List<EvaluationIndicator> indicators = buildIndicators();
            when(indicatorMapper.selectList(any())).thenReturn(indicators);

            Course mockCourse = course(1L, "CS101", "数据结构与算法", 1L);
            when(courseMapper.selectById(1L)).thenReturn(mockCourse);

            Teacher mockTeacher = teacher(1L, "T001", "张建国", "计算机学院", "教授");
            when(teacherMapper.selectById(1L)).thenReturn(mockTeacher);

            // 2 个学生，每人给 16 个二级指标打分（满分 100）
            List<EvaluationScore> scores = List.of(
                    score(1L, 1L, 5L, 90), score(1L, 1L, 6L, 88), score(1L, 1L, 7L, 85),
                    score(1L, 1L, 8L, 92), score(1L, 1L, 9L, 80), score(1L, 1L, 10L, 85),
                    score(1L, 1L, 11L, 82), score(1L, 1L, 12L, 88), score(1L, 1L, 13L, 86),
                    score(1L, 1L, 14L, 90), score(1L, 1L, 15L, 85), score(1L, 1L, 16L, 87),
                    score(1L, 1L, 17L, 88), score(1L, 1L, 18L, 85), score(1L, 1L, 19L, 82),
                    score(1L, 1L, 20L, 93),
                    // 第二个学生的评分
                    score(1L, 2L, 5L, 85), score(1L, 2L, 6L, 82), score(1L, 2L, 7L, 80),
                    score(1L, 2L, 8L, 88), score(1L, 2L, 9L, 82), score(1L, 2L, 10L, 80),
                    score(1L, 2L, 11L, 78), score(1L, 2L, 12L, 85), score(1L, 2L, 13L, 82),
                    score(1L, 2L, 14L, 86), score(1L, 2L, 15L, 80), score(1L, 2L, 16L, 84),
                    score(1L, 2L, 17L, 85), score(1L, 2L, 18L, 82), score(1L, 2L, 19L, 80),
                    score(1L, 2L, 20L, 88)
            );
            when(scoreMapper.selectList(any())).thenReturn(scores);

            // ===== When: 调用被测方法 =====
            CourseEvaluationDTO result = service.calculateCourseScore(1L, "2025-2026-1");

            // ===== Then: 断言验证 =====
            assertNotNull(result, "不应返回 null");
            assertEquals(1L, result.getCourseId());
            assertEquals("CS101", result.getCourseNo());
            assertEquals("数据结构与算法", result.getCourseName());
            assertEquals(1L, result.getTeacherId());
            assertEquals("张建国", result.getTeacherName());
            assertEquals(2, result.getTotalStudents(), "参评学生数应为 2");

            // 综合分应在 0-100 之间
            assertNotNull(result.getOverallScore());
            assertTrue(result.getOverallScore().compareTo(BigDecimal.ZERO) > 0, "综合分应 > 0");
            assertTrue(result.getOverallScore().compareTo(BigDecimal.valueOf(100)) <= 0, "综合分应 <= 100");

            // 应有 4 个维度得分
            assertEquals(4, result.getCategoryScores().size(), "应有 4 个维度");

            // 应有 16 个二级指标得分
            assertEquals(16, result.getIndicatorScores().size(), "应有 16 个二级指标");

            // ===== 验证 Mock 交互 =====
            verify(courseMapper).selectById(1L);
            verify(teacherMapper).selectById(1L);
            verify(scoreMapper).selectList(any());
            verify(indicatorMapper, atLeastOnce()).selectList(any());
        }

        /**
         * 边界值：单学生评分 → 平均分 = 该学生评分
         */
        @Test
        @DisplayName("边界值-单学生评分 → 平均分等于该学生评分")
        void shouldReturnExactScore_WhenSingleStudentRates() {
            when(indicatorMapper.selectList(any())).thenReturn(buildIndicators());
            when(courseMapper.selectById(1L)).thenReturn(course(1L, "CS101", "DS", 1L));
            when(teacherMapper.selectById(1L)).thenReturn(teacher(1L, "T001", "张老师", "CS", "教授"));

            // 仅 1 个学生给"备课充分"(I01-01) 打了 85 分
            EvaluationScore singleScore = score(1L, 1L, 5L, 85);
            when(scoreMapper.selectList(any())).thenReturn(List.of(singleScore));

            // When
            CourseEvaluationDTO result = service.calculateCourseScore(1L, "2025-2026-1");

            // Then: 该指标的平均分 = 85
            var indicatorScore = result.getIndicatorScores().stream()
                    .filter(i -> i.getIndicatorNo().equals("I01-01"))
                    .findFirst().orElseThrow();
            assertEquals(0, BigDecimal.valueOf(85).compareTo(indicatorScore.getAvgScore()),
                    "单学生时平均分应等于该学生评分");
        }

        /**
         * 边界值：课程无任何评分数据
         */
        @Test
        @DisplayName("边界值-无评分数据 → 返回空结果不抛异常")
        void shouldNotThrow_WhenNoScores() {
            when(indicatorMapper.selectList(any())).thenReturn(buildIndicators());
            when(courseMapper.selectById(1L)).thenReturn(course(1L, "CS101", "DS", 1L));
            when(teacherMapper.selectById(1L)).thenReturn(teacher(1L, "T001", "张老师", "CS", "教授"));
            when(scoreMapper.selectList(any())).thenReturn(List.of());  // 无评分

            // When & Then: 不抛异常
            CourseEvaluationDTO result = service.calculateCourseScore(1L, "2025-2026-1");
            assertNotNull(result);
            assertEquals(0, result.getTotalStudents());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getOverallScore()), "无评分时综合分应为 0");
        }

        /**
         * 异常场景：课程不存在
         */
        @Test
        @DisplayName("异常场景-课程不存在 → 抛出 BusinessException")
        void shouldThrowException_WhenCourseNotFound() {
            when(courseMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.calculateCourseScore(999L, "2025-2026-1"));

            assertTrue(ex.getMessage().contains("课程不存在"));
            verify(courseMapper).selectById(999L);
            // 验证没有调用后续方法
            verify(scoreMapper, never()).selectList(any());
        }

        /**
         * 边界值：教师信息为 null（课程无关联教师）
         */
        @Test
        @DisplayName("边界值-课程无教师 → teacherName 为 null")
        void shouldHandleNullTeacher() {
            when(indicatorMapper.selectList(any())).thenReturn(buildIndicators());
            Course courseWithoutTeacher = course(1L, "CS101", "DS", null); // teacherId = null
            when(courseMapper.selectById(1L)).thenReturn(courseWithoutTeacher);
            when(scoreMapper.selectList(any())).thenReturn(List.of(score(1L, 1L, 5L, 80)));

            CourseEvaluationDTO result = service.calculateCourseScore(1L, "2025-2026-1");

            assertNull(result.getTeacherId());
            assertNull(result.getTeacherName());
        }
    }

    @Nested
    @DisplayName("教师评价 - calculateTeacherScore")
    class TeacherEvaluation {

        /**
         * 场景法：教师有多门课，每门有不同学生评分 → 汇总
         */
        @Test
        @DisplayName("正常场景-教师多门课程 → 平均分 = 各课程平均")
        void shouldAggregateMultipleCourses() {
            // 指标体系
            when(indicatorMapper.selectList(any())).thenReturn(buildIndicators());

            // 教师
            Teacher t = teacher(1L, "T001", "张建国", "计算机学院", "教授");
            when(teacherMapper.selectById(1L)).thenReturn(t);

            // 教师有 2 门课
            when(courseMapper.selectList(any())).thenReturn(List.of(
                    course(1L, "CS101", "数据结构", 1L),
                    course(2L, "CS301", "软件工程", 1L)
            ));

            // 第一门课的评分
            when(courseMapper.selectById(1L)).thenReturn(course(1L, "CS101", "数据结构", 1L));
            when(courseMapper.selectById(2L)).thenReturn(course(2L, "CS301", "软件工程", 1L));

            // 两门课的评分数据
            List<EvaluationScore> scores1 = List.of(score(1L, 1L, 5L, 90));
            List<EvaluationScore> scores2 = List.of(score(2L, 1L, 5L, 80));

            when(scoreMapper.selectList(any()))
                    .thenReturn(scores1)   // 第一次调用（第一门课）
                    .thenReturn(scores2);  // 第二次调用（第二门课）

            // When
            TeacherEvaluationDTO result = service.calculateTeacherScore(1L, "2025-2026-1");

            // Then
            assertEquals(1L, result.getTeacherId());
            assertEquals("张建国", result.getName());
            assertEquals(2, result.getCourseCount());
            assertNotNull(result.getAverageOverallScore());
            assertEquals(2, result.getCourseDetails().size());
        }

        /**
         * 边界值：教师没有课程
         */
        @Test
        @DisplayName("边界值-教师无课程 → courseCount=0, averageScore=0")
        void shouldReturnEmpty_WhenNoCourses() {
            Teacher t = teacher(1L, "T001", "张建国", "计算机学院", "教授");
            when(teacherMapper.selectById(1L)).thenReturn(t);
            when(courseMapper.selectList(any())).thenReturn(List.of());

            TeacherEvaluationDTO result = service.calculateTeacherScore(1L, "2025-2026-1");

            assertEquals(0, result.getCourseCount());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAverageOverallScore()));
            assertTrue(result.getCourseDetails().isEmpty());
        }

        /**
         * 异常场景：教师不存在
         */
        @Test
        @DisplayName("异常场景-教师不存在 → 抛出 BusinessException")
        void shouldThrow_WhenTeacherNotFound() {
            when(teacherMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> service.calculateTeacherScore(999L, "2025-2026-1"));
        }
    }

    @Nested
    @DisplayName("学院排名 - getCollegeRanking")
    class CollegeRanking {

        /**
         * 场景法：多个教师已评价后，排名按分数降序
         */
        @Test
        @DisplayName("正常场景-学院排名 → 按平均分降序排列")
        void shouldRankByScoreDescending() {
            // 指标体系（两门课计算都需要）
            when(indicatorMapper.selectList(any())).thenReturn(buildIndicators());

            // 学院有 2 位教师
            when(teacherMapper.selectList(any())).thenReturn(List.of(
                    teacher(1L, "T001", "张建国", "计算机学院", "教授"),
                    teacher(2L, "T002", "李美玲", "计算机学院", "副教授")
            ));
            when(teacherMapper.selectById(1L)).thenReturn(teacher(1L, "T001", "张建国", "CS", "教授"));
            when(teacherMapper.selectById(2L)).thenReturn(teacher(2L, "T002", "李美玲", "CS", "副教授"));

            // 张老师有 1 门课
            when(courseMapper.selectList(any()))
                    .thenReturn(List.of(course(1L, "CS101", "DS", 1L)))  // 张老师的课
                    .thenReturn(List.of(course(2L, "CS201", "NW", 2L))); // 李老师的课

            when(courseMapper.selectById(1L)).thenReturn(course(1L, "CS101", "DS", 1L));
            when(courseMapper.selectById(2L)).thenReturn(course(2L, "CS201", "NW", 2L));

            // 张老师的课评分高，李老师的课评分低
            List<EvaluationScore> highScores = List.of(score(1L, 1L, 5L, 95)); // 高分
            List<EvaluationScore> lowScores  = List.of(score(2L, 1L, 5L, 70)); // 低分

            when(scoreMapper.selectList(any()))
                    .thenReturn(highScores).thenReturn(lowScores);

            // When
            List<TeacherEvaluationDTO> ranking = service.getCollegeRanking("计算机学院", "2025-2026-1");

            // Then: 第一名应该是分数更高的
            assertEquals(2, ranking.size());
            assertTrue(
                    ranking.get(0).getAverageOverallScore()
                            .compareTo(ranking.get(1).getAverageOverallScore()) >= 0,
                    "第一名应 >= 第二名"
            );
        }

        /**
         * 边界值：学院无教师
         */
        @Test
        @DisplayName("边界值-学院无教师 → 返回空列表")
        void shouldReturnEmpty_WhenNoTeachers() {
            when(teacherMapper.selectList(any())).thenReturn(List.of());

            List<TeacherEvaluationDTO> result = service.getCollegeRanking("不存在的学院", null);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("学期概览 - getSemesterOverview")
    class SemesterOverview {

        @Test
        @DisplayName("正常场景-学期概览 → 返回全量排名")
        void shouldReturnOverview() {
            when(teacherMapper.selectList(any())).thenReturn(List.of());
            List<TeacherEvaluationDTO> result = service.getSemesterOverview("2025-2026-1");
            assertTrue(result.isEmpty());
        }
    }
}
