package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.EvaluationIndicatorExcelDTO;
import com.example.aitaes.entity.EvaluationIndicator;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.EvaluationIndicatorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价指标数据导入策略
 * 需要解析 parentIndicatorNo → parentId 外键（支持二级指标体系）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationIndicatorImportStrategy implements ImportStrategy {

    private final EvaluationIndicatorMapper indicatorMapper;

    @Override
    public ImportType getSupportedType() {
        return ImportType.INDICATOR;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<EvaluationIndicatorExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, EvaluationIndicatorExcelDTO.class, listener)
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<EvaluationIndicatorExcelDTO> dtoList) {
        // 1. 预查重复指标编号
        List<String> indicatorNos = dtoList.stream()
                .map(EvaluationIndicatorExcelDTO::getIndicatorNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!indicatorNos.isEmpty()) {
            existingNos = indicatorMapper.selectList(
                    new LambdaQueryWrapper<EvaluationIndicator>()
                            .in(EvaluationIndicator::getIndicatorNo, indicatorNos)
            ).stream().map(EvaluationIndicator::getIndicatorNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 解析 parentIndicatorNo → parentId
        List<String> parentNos = dtoList.stream()
                .map(EvaluationIndicatorExcelDTO::getParentIndicatorNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Long> parentNoToId = Map.of();
        if (!parentNos.isEmpty()) {
            parentNoToId = indicatorMapper.selectList(
                    new LambdaQueryWrapper<EvaluationIndicator>()
                            .in(EvaluationIndicator::getIndicatorNo, parentNos)
            ).stream().collect(Collectors.toMap(
                    EvaluationIndicator::getIndicatorNo, EvaluationIndicator::getId));
        }

        // 3. 过滤 + 转换
        Map<String, Long> finalParentNoToId = parentNoToId;
        List<EvaluationIndicator> indicators = dtoList.stream()
                .filter(dto -> dto.getIndicatorNo() != null && !dto.getIndicatorNo().isBlank())
                .filter(dto -> !existingNos.contains(dto.getIndicatorNo()))
                .map(dto -> toEntity(dto, finalParentNoToId))
                .toList();

        // 4. 批量插入
        if (!indicators.isEmpty()) {
            indicators.forEach(indicatorMapper::insert);
            log.debug("批量插入评价指标数据 {} 条", indicators.size());
        }
    }

    private EvaluationIndicator toEntity(EvaluationIndicatorExcelDTO dto,
                                          Map<String, Long> parentNoToId) {
        EvaluationIndicator indicator = new EvaluationIndicator();
        BeanUtils.copyProperties(dto, indicator, "parentIndicatorNo");
        // 解析父级外键
        if (dto.getParentIndicatorNo() != null
                && parentNoToId.containsKey(dto.getParentIndicatorNo())) {
            indicator.setParentId(parentNoToId.get(dto.getParentIndicatorNo()));
        }
        return indicator;
    }

    private ImportResultDTO buildResult(GenericExcelListener<?> listener) {
        ImportResultDTO result = new ImportResultDTO();
        result.setTotalRows(listener.getTotalRows());
        result.setSuccessRows(listener.getSuccessCount());
        result.setFailRows(listener.getFailCount());

        if (listener.getFailCount() == 0) {
            result.setStatus(ImportStatus.SUCCESS.getCode());
        } else if (listener.getSuccessCount() == 0) {
            result.setStatus(ImportStatus.FAILED.getCode());
        } else {
            result.setStatus(ImportStatus.PARTIAL.getCode());
        }

        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
