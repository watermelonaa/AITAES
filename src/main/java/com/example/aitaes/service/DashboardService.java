package com.example.aitaes.service;

import com.example.aitaes.dto.DashboardDTO;
import com.example.aitaes.dto.OverviewStatsDTO;
import com.example.aitaes.dto.ScoreDistributionDTO;
import com.example.aitaes.dto.TrendDTO;

import java.util.List;

/**
 * 仪表盘数据聚合服务接口
 */
public interface DashboardService {

    /** 获取概览统计数据 */
    OverviewStatsDTO getOverview(String semester);

    /** 获取全校评分分布 */
    List<ScoreDistributionDTO> getScoreDistribution(String semester);

    /** 获取学期趋势 */
    TrendDTO getTrend();

    /** 获取仪表盘完整数据 */
    DashboardDTO getDashboard(String semester);
}
