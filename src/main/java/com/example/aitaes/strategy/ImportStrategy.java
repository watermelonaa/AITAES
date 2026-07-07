package com.example.aitaes.strategy;

import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.enums.ImportType;

import java.io.InputStream;

/**
 * 导入策略接口 - 每种导入数据类型对应一个实现
 */
public interface ImportStrategy {

    /** 本策略支持的导入类型 */
    ImportType getSupportedType();

    /** 执行导入，返回导入结果统计 */
    ImportResultDTO execute(InputStream inputStream, String originalFilename);
}
