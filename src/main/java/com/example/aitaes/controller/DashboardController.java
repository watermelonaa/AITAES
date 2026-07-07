package com.example.aitaes.controller;

import com.example.aitaes.common.Result;
import com.example.aitaes.dto.DashboardDTO;
import com.example.aitaes.dto.OverviewStatsDTO;
import com.example.aitaes.dto.ScoreDistributionDTO;
import com.example.aitaes.dto.TrendDTO;
import com.example.aitaes.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表盘控制器 — 供前端大屏/图表使用
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 仪表盘完整数据 */
    @GetMapping
    public Result<DashboardDTO> getDashboard(
            @RequestParam(required = false) String semester) {
        return Result.success(dashboardService.getDashboard(semester));
    }

    /** 概览统计卡片 */
    @GetMapping("/overview")
    public Result<OverviewStatsDTO> getOverview(
            @RequestParam(required = false) String semester) {
        return Result.success(dashboardService.getOverview(semester));
    }

    /** 评分分布（柱状图） */
    @GetMapping("/distribution")
    public Result<List<ScoreDistributionDTO>> getDistribution(
            @RequestParam(required = false) String semester) {
        return Result.success(dashboardService.getScoreDistribution(semester));
    }

    /** 学期趋势（折线图） */
    @GetMapping("/trend")
    public Result<TrendDTO> getTrend() {
        return Result.success(dashboardService.getTrend());
    }
}
