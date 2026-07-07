package com.example.aitaes.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果 DTO
 */
@Data
public class ImportResultDTO {

    /** 总行数 */
    private int totalRows;

    /** 成功行数 */
    private int successRows;

    /** 失败行数 */
    private int failRows;

    /** 状态：SUCCESS / PARTIAL / FAILED */
    private String status;

    /** 错误信息列表（最多返回前 100 条） */
    private List<String> errors = new ArrayList<>();
}
