package com.example.aitaes.strategy;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitaes.dto.ImportResultDTO;
import com.example.aitaes.dto.excel.StudentExcelDTO;
import com.example.aitaes.entity.Student;
import com.example.aitaes.entity.SystemConfig;
import com.example.aitaes.entity.User;
import com.example.aitaes.enums.ImportStatus;
import com.example.aitaes.enums.ImportType;
import com.example.aitaes.listener.GenericExcelListener;
import com.example.aitaes.mapper.StudentMapper;
import com.example.aitaes.mapper.SystemConfigMapper;
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
 * 学生数据导入策略
 * <p>
 * 对于新学生（学号在 t_student 中不存在），自动创建 t_user 认证账号，
 * 使用系统配置的默认密码（BCrypt 加密），角色设为 STUDENT，标记首次登录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentImportStrategy implements ImportStrategy {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final SystemConfigMapper systemConfigMapper;

    /** 缓存默认密码，避免每条记录都查数据库 */
    private String cachedDefaultPassword;
    private boolean passwordLoaded;

    @Override
    public ImportType getSupportedType() {
        return ImportType.STUDENT;
    }

    @Override
    public ImportResultDTO execute(InputStream inputStream, String originalFilename) {
        GenericExcelListener<StudentExcelDTO> listener =
                new GenericExcelListener<>(500, this::saveBatch);
        EasyExcel.read(inputStream, StudentExcelDTO.class, listener)
                .excelType(getExcelType(originalFilename))
                .sheet().doRead();
        return buildResult(listener);
    }

    private void saveBatch(List<StudentExcelDTO> dtoList) {
        // 1. 预查数据库中已有学号
        List<String> studentNos = dtoList.stream()
                .map(StudentExcelDTO::getStudentNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> existingNos;
        if (!studentNos.isEmpty()) {
            existingNos = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>()
                            .in(Student::getStudentNo, studentNos)
            ).stream().map(Student::getStudentNo).collect(Collectors.toSet());
        } else {
            existingNos = Set.of();
        }

        // 2. 对每个新学生：创建 t_user → t_student
        String defaultPassword = getDefaultPassword();
        int createdCount = 0;

        for (StudentExcelDTO dto : dtoList) {
            if (dto.getStudentNo() == null || dto.getStudentNo().isBlank()) continue;
            if (existingNos.contains(dto.getStudentNo())) continue;

            try {
                // 2a. 创建 t_user 认证账号
                User user = new User();
                user.setUsername(dto.getStudentNo());
                user.setPassword(PasswordUtil.encode(defaultPassword));
                user.setRole("STUDENT");
                user.setStatus("ACTIVE");
                user.setFirstLogin(1);
                user.setCreateTime(LocalDateTime.now());
                userMapper.insert(user);

                // 2b. 创建 t_student，关联 user_id
                Student student = new Student();
                BeanUtils.copyProperties(dto, student);
                student.setUserId(user.getId());
                student.setCreateTime(LocalDateTime.now());
                studentMapper.insert(student);

                createdCount++;
            } catch (Exception e) {
                log.warn("创建学生账号失败: studentNo={}, 原因: {}", dto.getStudentNo(), e.getMessage());
            }
        }

        if (createdCount > 0) {
            log.info("批量创建学生账号 {} 条（含 t_user + t_student）", createdCount);
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

        List<String> allErrors = listener.getErrorMessages();
        result.setErrors(allErrors.size() > 100 ? allErrors.subList(0, 100) : allErrors);
        return result;
    }
}
