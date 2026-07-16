package com.example.aitaes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.ExamPaperCreateDTO;
import com.example.aitaes.dto.ExamResultDTO;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.RoundingMode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamService 单元测试")
class ExamServiceImplTest {

    @Mock private ExamPaperMapper examPaperMapper;
    @Mock private ExamPaperQuestionMapper examPaperQuestionMapper;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private AssessmentMapper assessmentMapper;
    @Mock private AssessmentRecordMapper assessmentRecordMapper;
    @Mock private CourseStudentMapper courseStudentMapper;
    @Mock private StudentMapper studentMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private TeacherMapper teacherMapper;
    @InjectMocks private ExamServiceImpl examService;

    private ExamPaper paper;
    private QuestionBank question;

    @BeforeEach
    void setUp() {
        paper = new ExamPaper();
        paper.setId(1L);
        paper.setCourseId(1L);
        paper.setTeacherId(1L);
        paper.setPaperName("期中考试");
        paper.setTotalScore(new BigDecimal("100.00"));
        paper.setStatus("DRAFT");

        question = new QuestionBank();
        question.setId(10L);
        question.setCourseId(1L);
        question.setQuestionType("SINGLE");
        question.setContent("{\"question\":\"1+1=?\",\"answer\":\"2\"}");
        question.setUsageCount(0);
    }

    @Nested
    @DisplayName("createPaper — 创建试卷")
    class CreatePaper {

        @Test
        @DisplayName("EX-01: 应创建试卷并关联题目")
        void shouldCreatePaperWithQuestions() {
            ExamPaperCreateDTO dto = new ExamPaperCreateDTO();
            dto.setPaperName("期中考试");
            dto.setCourseId(1L);
            ExamPaperCreateDTO.QuestionItem qi = new ExamPaperCreateDTO.QuestionItem();
            qi.setQuestionId(10L); qi.setQuestionNo(1); qi.setScore(new BigDecimal("5"));
            dto.setQuestions(List.of(qi));

            Teacher teacher = new Teacher();
            teacher.setId(1L);
            teacher.setUserId(1L);
            when(teacherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(teacher);
            when(questionBankMapper.selectById(10L)).thenReturn(question);

            ExamPaper result = examService.createPaper(1L, dto);

            assertNotNull(result);
            verify(examPaperMapper).insert(any(ExamPaper.class));
            verify(examPaperQuestionMapper).insert(any(ExamPaperQuestion.class));
            verify(questionBankMapper).updateById(any(QuestionBank.class));
        }
    }

    @Nested
    @DisplayName("publish/close — 发布与关闭")
    class PublishClose {

        @Test
        @DisplayName("EX-02: 应发布考试")
        void shouldPublish() {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);
            examService.publishPaper(1L);
            verify(examPaperMapper).updateById(any(ExamPaper.class));
        }

        @Test
        @DisplayName("EX-03: 应关闭考试")
        void shouldClose() {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);
            examService.closePaper(1L);
            verify(examPaperMapper).updateById(any(ExamPaper.class));
        }

        @Test
        @DisplayName("EX-04: 试卷不存在应抛出异常")
        void shouldThrowException_WhenPaperNotFound() {
            when(examPaperMapper.selectById(999L)).thenReturn(null);
            assertThrows(BusinessException.class, () -> examService.publishPaper(999L));
        }
    }

    @Nested
    @DisplayName("getPendingExams — 学生待考列表")
    class GetPendingExams {

        @Test
        @DisplayName("EX-05: 应返回学生课程的已发布考试")
        void shouldReturnPublishedExams() {
            when(courseStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(new CourseStudent() {{
                        setCourseId(1L); setStudentId(100L);
                    }}));
            paper.setStatus("PUBLISHED");
            when(examPaperMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(paper));

            List<ExamPaper> result = examService.getPendingExams(100L);

            assertEquals(1, result.size());
            assertEquals("PUBLISHED", result.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("submitExam — 学生交卷")
    class SubmitExam {

        @Test
        @DisplayName("EX-06: 客观题全对应得满分")
        void shouldGetFullScore_WhenAllCorrect() {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);

            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setQuestionId(10L); epq.setQuestionNo(1); epq.setScore(new BigDecimal("10"));
            when(examPaperQuestionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(epq));
            when(questionBankMapper.selectById(10L)).thenReturn(question);

            examService.submitExam(1L, 100L, Map.of(10L, "2"));

            verify(assessmentMapper).insert(any(Assessment.class));
            verify(assessmentRecordMapper).insert(any(AssessmentRecord.class));
        }

        @Test
        @DisplayName("EX-07: 答案错误应得零分")
        void shouldGetZeroScore_WhenAllWrong() throws Exception {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);

            ExamPaperQuestion epq = new ExamPaperQuestion();
            epq.setQuestionId(10L); epq.setQuestionNo(1); epq.setScore(new BigDecimal("10"));
            when(examPaperQuestionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(epq));
            when(questionBankMapper.selectById(10L)).thenReturn(question);

            // Submit wrong answer
            examService.submitExam(1L, 100L, Map.of(10L, "wrong"));

            verify(assessmentRecordMapper).insert(any(AssessmentRecord.class));
        }
    }

    @Nested
    @DisplayName("getExamResults — 考试结果统计")
    class GetExamResults {

        @Test
        @DisplayName("EX-08: 应计算正确的统计值")
        void shouldCalculateStatistics() {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);

            Assessment assessment = new Assessment();
            assessment.setId(1L);
            when(assessmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(assessment);

            AssessmentRecord r1 = new AssessmentRecord();
            r1.setStudentId(100L); r1.setTotalScore(new BigDecimal("90"));
            AssessmentRecord r2 = new AssessmentRecord();
            r2.setStudentId(101L); r2.setTotalScore(new BigDecimal("70"));
            when(assessmentRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(r1, r2));
            when(courseStudentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            when(studentMapper.selectBatchIds(anyList())).thenReturn(List.of());

            ExamResultDTO result = examService.getExamResults(1L);

            assertEquals(new BigDecimal("80.00"), result.getAverageScore());
            assertTrue(result.getMaxScore().compareTo(new BigDecimal("90.00")) == 0);
            assertTrue(result.getMinScore().compareTo(new BigDecimal("70.00")) == 0);
        }

        @Test
        @DisplayName("EX-09: 无考核记录时应返回空统计")
        void shouldReturnEmptyStats_WhenNoAssessment() {
            when(examPaperMapper.selectById(1L)).thenReturn(paper);
            when(assessmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            ExamResultDTO result = examService.getExamResults(1L);

            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getTotalStudents());
        }
    }

    @Nested
    @DisplayName("submitGrade — 批阅主观题")
    class SubmitGrade {

        @Test
        @DisplayName("EX-10: 应更新分数和评语")
        void shouldUpdateScoreAndComment() {
            AssessmentRecord record = new AssessmentRecord();
            record.setId(1L);
            record.setTotalScore(BigDecimal.ZERO);
            when(assessmentRecordMapper.selectById(1L)).thenReturn(record);

            examService.submitGrade(1L, new BigDecimal("15"), "回答完整，逻辑清晰");

            verify(assessmentRecordMapper).updateById(any(AssessmentRecord.class));
        }
    }
}
