package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.entity.*;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考核成绩导入策略（作业/测验/考试）
 * <p>
 * 支持多Sheet Excel（每个Sheet=一个班级），动态题目数。
 * 模板格式：序号, 学号, 姓名, 第1题得分, 扣分主要知识点, 第2题得分, 扣分主要知识点, ..., 总成绩, 最薄弱知识点
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentImportStrategy implements ImportStrategy {

    private final AssessmentMapper assessmentMapper;
    private final AssessmentRecordMapper recordMapper;
    private final RecordKpDeductionMapper deductionMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final StudentKpMasteryMapper masteryMapper;

    /** 导入上下文 */
    private Long courseId;
    private String assessmentName;
    private String assessmentType;
    private String semester;
    private List<String> errors = new ArrayList<>();
    private int totalRows;
    private int successRows;
    private int failRows;

    @Override
    public ImportType getSupportedType() {
        return ImportType.HOMEWORK;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        errors.clear();
        totalRows = 0;
        successRows = 0;
        failRows = 0;

        // 解析文件名获取考核信息：格式为 "课程编号_考核类型_考核名称.xlsx"
        // 如：CS201_HOMEWORK_第1次作业.xlsx
        parseFileName(originalFilename);

        // 读取所有Sheet的数据
        List<Map<Integer, String>> allRows = new ArrayList<>();
        List<String> headerRow = new ArrayList<>();

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            private boolean isHeader = true;

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                if (isHeader) {
                    // 第一行是表头
                    headerRow.clear();
                    for (int i = 0; i < data.size(); i++) {
                        headerRow.add(data.getOrDefault(i, ""));
                    }
                    isHeader = false;
                    return;
                }

                // 跳过空行和知识点汇总行
                String firstCell = data.getOrDefault(0, "");
                if (firstCell == null || firstCell.isBlank()) {
                    return; // 跳过空行和底部知识点列表
                }

                // 跳过非学生数据行（序号不能解析为数字的行）
                try {
                    Integer.parseInt(firstCell.trim());
                } catch (NumberFormatException e) {
                    return;
                }

                allRows.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Sheet解析完成: {}行数据", allRows.size());
            }
        }).sheet().doRead();

        // 解析题目数：从表头中检测"第X题得分"的最大X值
        int questionCount = detectQuestionCount(headerRow);
        log.info("检测到题目数: {}", questionCount);

        // 查找或创建考核记录
        Assessment assessment = findOrCreateAssessment(questionCount);

        // 逐行处理学生数据
        for (Map<Integer, String> row : allRows) {
            try {
                processStudentRow(row, assessment, questionCount);
                successRows++;
            } catch (Exception e) {
                failRows++;
                errors.add(String.format("第%d行处理失败: %s", totalRows + 1, e.getMessage()));
                log.warn("处理学生行失败", e);
            }
            totalRows++;
        }

        // 导入完成后重新计算知识点掌握度
        recalculateKpMastery(assessment.getId());

        return buildResult();
    }

    /**
     * 从文件名解析考核信息
     */
    private void parseFileName(String filename) {
        if (filename == null) return;
        String name = filename.replace(".xlsx", "").replace(".xls", "");
        String[] parts = name.split("_");
        // 格式：courseNo_type_name，如 CS201_HOMEWORK_第1次作业
        if (parts.length >= 3 && parts[1].equals("HOMEWORK")) {
            // 查找课程
            Course course = courseMapper.selectOne(
                    new LambdaQueryWrapper<Course>().eq(Course::getCourseNo, parts[0]));
            if (course != null) {
                this.courseId = course.getId();
                this.semester = course.getSemester();
            }
            this.assessmentType = parts[1];
            this.assessmentName = parts[2];
        }
    }

    /**
     * 从表头中检测题目数量
     */
    private int detectQuestionCount(List<String> header) {
        int maxQ = 0;
        for (String col : header) {
            // 匹配"第X题得分"模式
            if (col != null && col.matches("第\\d+题得分")) {
                int qno = Integer.parseInt(col.replaceAll("[^0-9]", ""));
                maxQ = Math.max(maxQ, qno);
            }
        }
        return maxQ > 0 ? maxQ : 5; // 默认5题
    }

    /**
     * 查找或创建考核
     */
    private Assessment findOrCreateAssessment(int questionCount) {
        if (courseId == null || assessmentName == null) {
            throw new IllegalStateException("无法确定课程或考核名称，请检查文件名格式: COURSENO_TYPE_NAME.xlsx");
        }

        Assessment existing = assessmentMapper.selectOne(
                new LambdaQueryWrapper<Assessment>()
                        .eq(Assessment::getCourseId, courseId)
                        .eq(Assessment::getAssessmentName, assessmentName));

        if (existing != null) {
            return existing;
        }

        Assessment assessment = new Assessment();
        assessment.setCourseId(courseId);
        assessment.setAssessmentName(assessmentName);
        assessment.setAssessmentType(assessmentType != null ? assessmentType : "HOMEWORK");
        assessment.setTotalScore(new BigDecimal("100.00"));
        assessment.setQuestionCount(questionCount);
        assessment.setSemester(semester);
        assessment.setAssessmentDate(LocalDate.now());
        assessmentMapper.insert(assessment);
        return assessment;
    }

    /**
     * 处理单个学生行
     */
    private void processStudentRow(Map<Integer, String> row, Assessment assessment, int questionCount) {
        String studentNo = row.getOrDefault(1, "").trim(); // 第2列(索引1) = 学号
        String studentName = row.getOrDefault(2, "").trim(); // 第3列(索引2) = 姓名

        if (studentNo.isEmpty()) return;

        // 查找学生
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo));
        if (student == null) {
            throw new RuntimeException("学生不存在: " + studentNo);
        }

        // 总成绩在第 N*2+3 列（序号0 + 学号1 + 姓名2 + N题×(得分+知识点) = 3+2N）
        int totalScoreCol = 3 + questionCount * 2;
        String totalScoreStr = row.getOrDefault(totalScoreCol, "0").trim();
        BigDecimal totalScore = parseScore(totalScoreStr);

        // 最薄弱知识点在总成绩后一列
        String weakestKp = row.getOrDefault(totalScoreCol + 1, "").trim();

        // 创建考核记录
        AssessmentRecord record = new AssessmentRecord();
        record.setAssessmentId(assessment.getId());
        record.setStudentId(student.getId());
        record.setTotalScore(totalScore);
        record.setWeakestKp(weakestKp.isEmpty() ? null : weakestKp);
        record.setSubmitTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 逐题创建扣分知识点明细
        for (int q = 1; q <= questionCount; q++) {
            int scoreCol = 3 + (q - 1) * 2;     // 第q题得分列
            int deductionCol = scoreCol + 1;      // 第q题扣分知识点列

            String scoreStr = row.getOrDefault(scoreCol, "");
            String deductionKp = row.getOrDefault(deductionCol, "");

            if (scoreStr == null || scoreStr.trim().isEmpty()) {
                continue; // 该题无数据，跳过
            }

            BigDecimal questionScore = parseScore(scoreStr.trim());

            // 只有扣了分才记录（得分 < 满分）
            if (questionScore.compareTo(new BigDecimal("20")) < 0 || (deductionKp != null && !deductionKp.trim().isEmpty())) {
                RecordKpDeduction deduction = new RecordKpDeduction();
                deduction.setRecordId(record.getId());
                deduction.setQuestionNo(q);
                deduction.setQuestionScore(questionScore);
                deduction.setMaxScore(new BigDecimal("20")); // 每题默认20分
                deduction.setDeductionKp(deductionKp != null ? deductionKp.trim() : "");
                deductionMapper.insert(deduction);
            }
        }
    }

    /**
     * 重算学生知识点掌握度
     */
    private void recalculateKpMastery(Long assessmentId) {
        // 查询该考核的所有扣分明细
        List<AssessmentRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AssessmentRecord>()
                        .eq(AssessmentRecord::getAssessmentId, assessmentId));

        for (AssessmentRecord record : records) {
            List<RecordKpDeduction> deductions = deductionMapper.selectList(
                    new LambdaQueryWrapper<RecordKpDeduction>()
                            .eq(RecordKpDeduction::getRecordId, record.getId()));

            // 按知识点汇总扣分
            Map<String, BigDecimal[]> kpStats = new LinkedHashMap<>(); // kpName -> [总扣分, 总满分]
            for (RecordKpDeduction d : deductions) {
                if (d.getDeductionKp() == null || d.getDeductionKp().isBlank()) continue;

                String[] kps = d.getDeductionKp().split("[,，/]");
                BigDecimal lostScore = d.getMaxScore().subtract(
                        d.getQuestionScore() != null ? d.getQuestionScore() : BigDecimal.ZERO);

                for (String kp : kps) {
                    String kpName = kp.trim();
                    if (kpName.isEmpty()) continue;

                    BigDecimal[] stats = kpStats.computeIfAbsent(kpName, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    stats[0] = stats[0].add(lostScore.max(BigDecimal.ZERO)); // 累积扣分
                    stats[1] = stats[1].add(d.getMaxScore());                // 累积满分
                }
            }

            // UPSERT 到掌握度表
            for (Map.Entry<String, BigDecimal[]> entry : kpStats.entrySet()) {
                String kpName = entry.getKey();
                BigDecimal totalLost = entry.getValue()[0];
                BigDecimal totalMax = entry.getValue()[1];

                // mastery_rate = MAX(0, 100 - (总扣分/总分)*100)
                BigDecimal masteryRate = BigDecimal.valueOf(100);
                if (totalMax.compareTo(BigDecimal.ZERO) > 0) {
                    masteryRate = new BigDecimal("100").subtract(
                            totalLost.divide(totalMax, 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100")));
                }
                masteryRate = masteryRate.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);

                // 查找已有记录
                StudentKpMastery existing = masteryMapper.selectOne(
                        new LambdaQueryWrapper<StudentKpMastery>()
                                .eq(StudentKpMastery::getStudentId, record.getStudentId())
                                .eq(StudentKpMastery::getCourseId, courseId)
                                .eq(StudentKpMastery::getKpName, kpName));

                if (existing != null) {
                    // 更新：重新计算综合掌握率
                    existing.setMasteryRate(masteryRate);
                    existing.setLoseCount(existing.getLoseCount() + 1);
                    existing.setTotalQuestionCount(existing.getTotalQuestionCount() + 1);
                    existing.setLastAssessmentId(assessmentId);
                    existing.setLastUpdated(LocalDateTime.now());
                    masteryMapper.updateById(existing);
                } else {
                    // 新增
                    StudentKpMastery mastery = new StudentKpMastery();
                    mastery.setStudentId(record.getStudentId());
                    mastery.setCourseId(courseId);
                    mastery.setKpName(kpName);
                    mastery.setMasteryRate(masteryRate);
                    mastery.setLoseCount(totalLost.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                    mastery.setTotalQuestionCount(1);
                    mastery.setLastAssessmentId(assessmentId);
                    mastery.setLastUpdated(LocalDateTime.now());
                    masteryMapper.insert(mastery);
                }
            }
        }
    }

    private BigDecimal parseScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(scoreStr.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private ImportResultDTO buildResult() {
        ImportResultDTO result = new ImportResultDTO();
        result.setTotalRows(totalRows);
        result.setSuccessRows(successRows);
        result.setFailRows(failRows);

        if (failRows == 0) {
            result.setStatus(ImportStatus.SUCCESS.getCode());
        } else if (successRows == 0) {
            result.setStatus(ImportStatus.FAILED.getCode());
        } else {
            result.setStatus(ImportStatus.PARTIAL.getCode());
        }

        result.setErrors(errors.size() > 100 ? errors.subList(0, 100) : errors);
        return result;
    }
}
