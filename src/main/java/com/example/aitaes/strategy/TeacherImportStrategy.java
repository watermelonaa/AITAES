package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.TeacherExcelDTO;
import com.example.aitaes.entity.SystemConfig;
import com.example.aitaes.entity.Teacher;
import com.example.aitaes.entity.User;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.SystemConfigMapper;
import com.example.aitaes.mapper.TeacherMapper;
import com.example.aitaes.mapper.UserMapper;
import com.example.aitaes.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教师数据导入策略
 * <p>
 * 对于新教师（工号在 t_teacher 中不存在），自动创建 t_user 认证账号，
 * 使用系统配置的默认密码（BCrypt 加密），角色设为 TEACHER。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherImportStrategy implements ImportStrategy {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;

    /** 缓存默认密码，避免每条记录都查数据库 */
    private String cachedDefaultPassword;
    private boolean passwordLoaded;

    @Override
    public ImportType getSupportedType() {
        return ImportType.TEACHER;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<TeacherExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, TeacherExcelDTO.class, listener)
                .excelType(getExcelType(originalFilename))
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<TeacherExcelDTO> dtoList) {
        // 1. 预查重复工号
        List<String> teacherNos = dtoList.stream()
                .map(TeacherExcelDTO::getTeacherNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!teacherNos.isEmpty()) {
            existingNos = teacherMapper.selectList(
                    new LambdaQueryWrapper<Teacher>()
                            .in(Teacher::getTeacherNo, teacherNos)
            ).stream().map(Teacher::getTeacherNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 对每个新教师：创建 t_user → t_teacher
        String defaultPassword = getDefaultPassword();
        int createdCount = 0;

        for (TeacherExcelDTO dto : dtoList) {
            if (dto.getTeacherNo() == null || dto.getTeacherNo().isBlank()) continue;
            if (existingNos.contains(dto.getTeacherNo())) continue;

            try {
                // 2a. 创建 t_user 认证账号
                User user = new User();
                user.setUsername(dto.getTeacherNo());
                user.setPassword(PasswordUtil.encode(defaultPassword));
                user.setRole("TEACHER");
                user.setStatus("ACTIVE");
                user.setFirstLogin(1);
                user.setCreateTime(LocalDateTime.now());
                userMapper.insert(user);

                // 2b. 创建 t_teacher，关联 user_id
                Teacher teacher = new Teacher();
                BeanUtils.copyProperties(dto, teacher);
                teacher.setUserId(user.getId());
                teacher.setCreateTime(LocalDateTime.now());
                teacherMapper.insert(teacher);

                createdCount++;
            } catch (Exception e) {
                log.warn("创建教师账号失败: teacherNo={}, 原因: {}", dto.getTeacherNo(), e.getMessage());
            }
        }

        if (createdCount > 0) {
            log.info("批量创建教师账号 {} 条（含 t_user + t_teacher）", createdCount);
        }
    }

    /**
     * 从系统配置读取默认初始密码，带缓存
     */
    private String getDefaultPassword() {
        if (!passwordLoaded) {
            try {
                SystemConfig config = systemConfigMapper.selectOne(
                        new LambdaQueryWrapper<SystemConfig>()
                                .eq(SystemConfig::getConfigKey, "default.password"));
                if (config != null && config.getConfigValue() != null
                        && !config.getConfigValue().isBlank()) {
                    cachedDefaultPassword = config.getConfigValue();
                }
            } catch (Exception e) {
                log.warn("读取默认密码配置失败，使用 fallback: 123456");
            }
            if (cachedDefaultPassword == null || cachedDefaultPassword.isBlank()) {
                cachedDefaultPassword = "123456";
            }
            passwordLoaded = true;
        }
        return cachedDefaultPassword;
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

        // 只返回前 100 条错误
        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
