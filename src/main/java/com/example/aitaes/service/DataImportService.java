package com.example.aitaes.service;

import com.example.aitaes.dto.ImportResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据导入服务接口
 */
public interface DataImportService {

    /**
     * 从上传文件导入数据
     *
     * @param file       上传的文件
     * @param importType 导入类型：TEACHER/STUDENT/COURSE/SCORE/INDICATOR
     * @param sourceId   数据源ID（可选，用于追踪来源）
     * @return 导入结果
     */
    ImportResultDTO importFile(MultipartFile file, String importType, Long sourceId);
}
