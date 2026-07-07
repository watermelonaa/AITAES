package com.example.aitaes.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 通用 EasyExcel 读取监听器
 * <p>
 * 通过回调模式 {@link Consumer} 实现与业务逻辑的完全解耦。
 * 支持批量处理、行级容错、统计计数。
 *
 * @param <T> Excel 行数据对应的 DTO 类型
 */
@Slf4j
public class GenericExcelListener<T> implements ReadListener<T> {

    private final int batchSize;
    private final List<T> batch;
    private final Consumer<List<T>> batchProcessor;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final List<String> errorMessages = new ArrayList<>();
    private int totalRows = 0;

    /**
     * @param batchSize      每批处理的行数
     * @param batchProcessor 批处理回调，接收当前批次数据列表
     */
    public GenericExcelListener(int batchSize, Consumer<List<T>> batchProcessor) {
        this.batchSize = batchSize;
        this.batchProcessor = batchProcessor;
        this.batch = new ArrayList<>(batchSize);
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        totalRows++;
        batch.add(data);
        if (batch.size() >= batchSize) {
            flushBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!batch.isEmpty()) {
            flushBatch();
        }
        log.info("Excel 解析完成: 共{}行, 成功{}, 失败{}", totalRows, successCount.get(), failCount.get());
    }

    /**
     * EasyExcel 解析级别异常处理（数据类型转换失败等）
     * 不抛出异常则 EasyExcel 继续处理后续行
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        if (exception instanceof ExcelDataConvertException ex) {
            failCount.incrementAndGet();
            errorMessages.add(String.format("第%d行第%d列: 数据格式错误 - %s",
                    ex.getRowIndex() + 1, ex.getColumnIndex() + 1, ex.getMessage()));
            log.warn("第{}行第{}列数据格式错误", ex.getRowIndex() + 1, ex.getColumnIndex() + 1);
        } else {
            failCount.incrementAndGet();
            Integer rowIndex = context.readRowHolder() != null
                    ? context.readRowHolder().getRowIndex() + 1 : null;
            errorMessages.add(String.format("%s: 解析错误 - %s",
                    rowIndex != null ? "第" + rowIndex + "行" : "未知行", exception.getMessage()));
            log.warn("Excel 解析错误", exception);
        }
        // 不抛出异常，继续处理后续行
    }

    private void flushBatch() {
        try {
            batchProcessor.accept(new ArrayList<>(batch));
            successCount.addAndGet(batch.size());
        } catch (Exception e) {
            failCount.addAndGet(batch.size());
            errorMessages.add("批次保存失败: " + e.getMessage());
            log.error("批次保存失败，共{}行", batch.size(), e);
        }
        batch.clear();
    }

    // ===== 统计信息获取 =====

    public int getTotalRows() {
        return totalRows;
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailCount() {
        return failCount.get();
    }

    public List<String> getErrorMessages() {
        return new ArrayList<>(errorMessages);
    }
}
