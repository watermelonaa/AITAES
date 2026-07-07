package com.example.aitaes.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.entity.Report;

/**
 * 评价报告服务接口
 */
public interface ReportService {

    /** 生成课程评价报告并持久化 */
    Report generateCourseReport(Long courseId, String semester);

    /** 生成教师评价报告并持久化 */
    Report generateTeacherReport(Long teacherId, String semester);

    /** 生成学院报告 */
    Report generateCollegeReport(String college, String semester);

    /** 分页查询报告列表 */
    IPage<Report> list(int pageNum, int pageSize, String reportType, String semester);

    /** 获取报告详情 */
    Report getById(Long id);

    /** 删除报告 */
    void delete(Long id);
}
