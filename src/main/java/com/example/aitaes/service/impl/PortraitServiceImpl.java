package com.example.aitaes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.ChartItem;
import com.example.aitaes.dto.StudentProfileVO;
import com.example.aitaes.entity.*;
import com.example.aitaes.mapper.*;
import com.example.aitaes.service.PortraitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学生画像服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortraitServiceImpl implements PortraitService {

    private final StudentMapper studentMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final AssessmentMapper assessmentMapper;
    private final AttendanceMapper attendanceMapper;
    private final ExperimentMapper experimentMapper;
    private final StudentKpMasteryMapper studentKpMasteryMapper;

    @Override
    public StudentProfileVO getProfile(Long studentId, Long courseId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "学生不存在");
        }

        CourseStudent cs = courseStudentMapper.selectOne(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, courseId)
                        .eq(CourseStudent::getStudentId, studentId));

        // 基本信息 + 重点关注
        StudentProfileVO.StudentProfileVOBuilder builder = StudentProfileVO.builder()
                .studentId(student.getId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .gender(student.getGender())
                .college(student.getCollege())
                .major(student.getMajor())
                .className(student.getClassName())
                .grade(student.getGrade())
                .avatar(student.getAvatar())
                .isFocus(cs != null && cs.getIsFocus() != null && cs.getIsFocus() == 1);

        // 成绩概览
        builder.scoreTrendList(getScoreTrend(studentId, courseId));

        // 考勤
        List<Attendance> atts = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getCourseId, courseId)
                        .eq(Attendance::getStudentId, studentId));
        builder.attendanceList(atts.stream().map(a -> StudentProfileVO.AttendanceItem.builder()
                .date(a.getAttendanceDate() != null ? a.getAttendanceDate().atStartOfDay() : null)
                .status(a.getStatus()).weekNo(a.getWeekNo()).remark(a.getRemark()).build())
                .collect(Collectors.toList()));
        long presentCount = atts.stream().filter(a -> "出勤".equals(a.getStatus())).count();
        builder.attendanceRate(atts.isEmpty() ? BigDecimal.ZERO
                : new BigDecimal(presentCount).divide(new BigDecimal(atts.size()), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100)).setScale(1, RoundingMode.HALF_UP));

        // 作业
        builder.homeworkList(getHomeworkList(studentId, courseId));

        // 实验
        List<Experiment> exps = experimentMapper.selectList(
                new LambdaQueryWrapper<Experiment>()
                        .eq(Experiment::getCourseId, courseId)
                        .eq(Experiment::getStudentId, studentId));
        builder.experimentList(exps.stream().map(e -> StudentProfileVO.ExperimentItem.builder()
                .name(e.getExperimentName()).experimentNo(e.getExperimentNo())
                .score(e.getScore()).submitTime(e.getSubmitTime()).build())
                .collect(Collectors.toList()));

        // 知识点掌握度
        builder.knowledgeRadar(getKnowledgeRadar(studentId, courseId, false));
        builder.classAvgRadar(getKnowledgeRadar(studentId, courseId, true));

        // AI 评价（预留）
        builder.aiEvaluation(generateAiEvaluation(studentId, courseId));

        return builder.build();
    }

    @Override
    public void toggleFocus(Long studentId, Long courseId, boolean focus) {
        CourseStudent cs = courseStudentMapper.selectOne(
                new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, courseId)
                        .eq(CourseStudent::getStudentId, studentId));
        if (cs == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该学生不在班级中");
        }
        cs.setIsFocus(focus ? 1 : 0);
        courseStudentMapper.updateById(cs);
        log.info("{} 重点关注学生: studentId={}, courseId={}",
                focus ? "标记" : "取消", studentId, courseId);
    }

    // ===== 私有方法 =====

    private List<StudentProfileVO.HomeworkItem> getHomeworkList(Long studentId, Long courseId) {
        List<Assessment> hwAssessments = assessmentMapper.selectList(
                new LambdaQueryWrapper<Assessment>()
                        .eq(Assessment::getCourseId, courseId)
                        .eq(Assessment::getAssessmentType, "HOMEWORK")
                        .orderByAsc(Assessment::getAssessmentNo));
        return hwAssessments.stream().map(hw -> {
            AssessmentRecord record = assessmentRecordMapper.selectOne(
                    new LambdaQueryWrapper<AssessmentRecord>()
                            .eq(AssessmentRecord::getAssessmentId, hw.getId())
                            .eq(AssessmentRecord::getStudentId, studentId));
            return StudentProfileVO.HomeworkItem.builder()
                    .name(hw.getAssessmentName())
                    .score(record != null ? record.getTotalScore() : null)
                    .submitStatus(record != null ? record.getSubmitStatus() : null)
                    .submitTime(record != null ? record.getSubmitTime() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<ChartItem> getKnowledgeRadar(Long studentId, Long courseId, boolean classAvg) {
        List<StudentKpMastery> masteryList = studentKpMasteryMapper.selectList(
                new LambdaQueryWrapper<StudentKpMastery>()
                        .eq(StudentKpMastery::getCourseId, courseId));
        if (classAvg) {
            // 按知识点聚合班级均值
            return masteryList.stream()
                    .collect(Collectors.groupingBy(StudentKpMastery::getKpName))
                    .entrySet().stream().map(e -> {
                        BigDecimal avg = e.getValue().stream()
                                .map(m -> m.getMasteryRate() != null ? m.getMasteryRate() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(new BigDecimal(e.getValue().size()), 2, RoundingMode.HALF_UP);
                        return ChartItem.builder().name(e.getKey()).value(avg).build();
                    }).collect(Collectors.toList());
        } else {
            return masteryList.stream()
                    .filter(m -> m.getStudentId().equals(studentId))
                    .map(m -> ChartItem.builder()
                            .name(m.getKpName()).value(m.getMasteryRate()).build())
                    .collect(Collectors.toList());
        }
    }

    private List<com.example.aitaes.dto.TrendDTO> getScoreTrend(Long studentId, Long courseId) {
        return Collections.emptyList(); // 复用 Dashboard 的 TrendDTO，暂简化
    }
}
