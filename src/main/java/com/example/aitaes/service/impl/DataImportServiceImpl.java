package com.example.aitaes.service.impl;

import com.example.aitaes.common.BusinessException;
import com.example.aitaes.common.ResultCode;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.entity.DataImportLog;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.mapper.DataImportLogMapper;
import com.example.aitaes.service.DataImportService;
import com.example.aitaes.strategy.ImportStrategy;
import com.example.aitaes.strategy.ImportStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据导入服务实现
 * <p>
 * 职责：文件验证 → 类型路由 → 策略执行 → 日志记录
 * 每批次独立提交（不加 @Transactional），实现行级容错
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final ImportStrategyFactory strategyFactory;
    private final DataImportLogMapper dataImportLogMapper;

    /** 支持的导入文件扩展名 */
    private static final List<String> SUPPORTED_EXTENSIONS =
            List.of(".xlsx", ".xls", ".csv");

    @Override
    public ImportResultDTO importFile(MultipartFile file, String importType, Long sourceId) {
        // 1. 文件校验
        validateFile(file);

        // 2. 解析导入类型
        ImportType type = ImportType.fromCode(importType.toUpperCase());

        // 3. 获取策略
        ImportStrategy strategy = strategyFactory.getStrategy(type);

        // 4. 执行导入
        ImportResultDTO result;
        try (InputStream inputStream = file.getInputStream()) {
            log.info("开始导入 {}: 文件={}, 类型={}", type.getDescription(),
                    file.getOriginalFilename(), type.getCode());
            result = strategy.execute(inputStream, file.getOriginalFilename());
            log.info("导入完成 {}: 总{}行, 成功{}行, 失败{}行",
                    type.getDescription(), result.getTotalRows(),
                    result.getSuccessRows(), result.getFailRows());
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage());
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }

        // 5. 记录导入日志
        saveImportLog(file.getOriginalFilename(), type.getCode(), sourceId, result);

        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "文件名不能为空");
        }
        String lower = filename.toLowerCase();
        boolean supported = SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!supported) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "不支持的文件格式，仅支持: .xlsx, .xls, .csv");
        }
    }

    private void saveImportLog(String fileName, String importType, Long sourceId,
                                ImportResultDTO result) {
        try {
            DataImportLog logEntity = new DataImportLog();
            logEntity.setSourceId(sourceId);
            logEntity.setFileName(fileName);
            logEntity.setImportType(importType);
            logEntity.setTotalRows(result.getTotalRows());
            logEntity.setSuccessRows(result.getSuccessRows());
            logEntity.setFailRows(result.getFailRows());
            logEntity.setStatus(result.getStatus());
            logEntity.setImportTime(LocalDateTime.now());

            // 错误信息截断
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                String errorJson = result.getErrors().stream()
                        .limit(200)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("");
                if (errorJson.length() > 60000) {
                    errorJson = errorJson.substring(0, 60000) + "\n...[已截断]";
                }
                logEntity.setErrorMsg(errorJson);
            }
            dataImportLogMapper.insert(logEntity);
        } catch (Exception e) {
            // 日志记录失败不影响导入结果
            log.warn("记录导入日志失败: {}", e.getMessage());
        }
    }
}
