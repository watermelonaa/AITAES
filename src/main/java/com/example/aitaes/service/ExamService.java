package com.example.aitaes.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.dto.ExamPaperCreateDTO;
import com.example.aitaes.dto.ExamResultDTO;
import com.example.aitaes.entity.ExamPaper;
import com.example.aitaes.entity.QuestionBank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 考试服务接口
 */
public interface ExamService {

    // ===== 试卷管理 =====

    ExamPaper createPaper(Long teacherId, ExamPaperCreateDTO dto);
    IPage<ExamPaper> listPapers(int pageNum, int pageSize, Long courseId, Long teacherId);
    ExamPaper getPaperById(Long id);
    ExamPaper updatePaper(Long id, ExamPaperCreateDTO dto);
    void deletePaper(Long id);
    void publishPaper(Long id);
    void closePaper(Long id);

    // ===== 学生考试 =====

    List<ExamPaper> getPendingExams(Long studentId);
    ExamPaper getExamForStudent(Long paperId, Long studentId);
    void submitExam(Long paperId, Long studentId, Map<Long, String> answers);

    // ===== 考试结果 =====

    ExamResultDTO getExamResults(Long paperId);

    // ===== 主观题批阅 =====

    List<Map<String, Object>> getGradingList(Long courseId);
    void submitGrade(Long recordId, BigDecimal score, String comment);

    // ===== AI 预留接口 =====

    default String generateWrongAnswerAnalysis(Long questionId) { return null; }
    default List<QuestionBank> generateSimilarQuestions(Long questionId, int count) { return null; }
    default String suggestScore(String questionContent, String studentAnswer) { return null; }
}
