package com.example.aitaes.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.entity.DataImportLog;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

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

    /**
     * 分页查询导入历史
     */
    IPage<DataImportLog> getHistory(int pageNum, int pageSize, String importType);

    /**
     * 生成导入模板 Excel 文件，写入 OutputStream
     */
    void generateTemplate(String importType, OutputStream outputStream);
}
