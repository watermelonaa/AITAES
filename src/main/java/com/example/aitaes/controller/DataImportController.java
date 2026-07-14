package com.example.aitaes.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.common.Result;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.entity.DataImportLog;
import com.example.aitaes.service.DataImportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 数据导入控制器
 * <p>
 * 支持上传 Excel(.xlsx/.xls) 和 CSV 文件，按类型导入数据
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    /**
     * 上传文件并导入数据
     */
    @PostMapping("/upload")
    public Result<ImportResultDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType,
            @RequestParam(value = "sourceId", required = false) Long sourceId) {

        log.info("收到导入请求: 文件={}, 类型={}, 数据源ID={}",
                file.getOriginalFilename(), importType, sourceId);

        ImportResultDTO result = dataImportService.importFile(file, importType, sourceId);

        return switch (result.getStatus()) {
            case "SUCCESS" -> Result.success(
                    String.format("导入成功: 共%d行", result.getSuccessRows()), result);
            case "PARTIAL" -> Result.error(
                    com.example.aitaes.common.ResultCode.IMPORT_PARTIAL,
                    String.format("部分导入成功: 成功%d行, 失败%d行",
                            result.getSuccessRows(), result.getFailRows()));
            default -> Result.error(
                    com.example.aitaes.common.ResultCode.INTERNAL_ERROR.getCode(),
                    "导入全部失败");
        };
    }

    /**
     * 查询导入历史 (UC29)
     */
    @GetMapping("/history")
    public Result<IPage<DataImportLog>> history(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String importType) {
        return Result.success(dataImportService.getHistory(pageNum, pageSize, importType));
    }

    /**
     * 下载导入模板 (UC28)
     */
    @GetMapping("/template/{importType}")
    public void downloadTemplate(@PathVariable String importType,
                                  HttpServletResponse response) throws IOException {
        String fileName = importType.toUpperCase() + "_导入模板.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        dataImportService.generateTemplate(importType, response.getOutputStream());
    }
}
