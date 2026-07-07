package com.example.aitaes.controller;

import com.example.aitaes.common.Result;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.service.DataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     *
     * @param file       上传的文件（.xlsx / .xls / .csv）
     * @param importType 导入类型：TEACHER / STUDENT / COURSE / SCORE / INDICATOR
     * @param sourceId   数据源ID（可选）
     * @return 导入结果
     */
    @PostMapping("/upload")
    public Result<ImportResultDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType,
            @RequestParam(value = "sourceId", required = false) Long sourceId) {

        log.info("收到导入请求: 文件={}, 类型={}, 数据源ID={}",
                file.getOriginalFilename(), importType, sourceId);

        ImportResultDTO result = dataImportService.importFile(file, importType, sourceId);

        // 根据不同状态返回不同消息
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
}
