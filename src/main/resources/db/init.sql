-- ============================================================
-- AITAES 数智化教学分析评价系统 - 数据库初始化脚本
-- 数据库：aitaes_db
-- 字符集：utf8mb4
-- ============================================================

USE aitaes_db;

-- 关闭外键检查，允许按任意顺序 DROP TABLE
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 教师表
-- ============================================================
DROP TABLE IF EXISTS `t_teacher`;
CREATE TABLE `t_teacher` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `teacher_no`    VARCHAR(32)  NOT NULL COMMENT '工号',
    `name`          VARCHAR(64)  NOT NULL COMMENT '姓名',
    `gender`        VARCHAR(8)   DEFAULT NULL COMMENT '性别',
    `college`       VARCHAR(128) DEFAULT NULL COMMENT '学院',
    `department`    VARCHAR(128) DEFAULT NULL COMMENT '系/部门',
    `title`         VARCHAR(64)  DEFAULT NULL COMMENT '职称',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone`         VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_no` (`teacher_no`),
    KEY `idx_college` (`college`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';


-- ============================================================
-- 2. 学生表
-- ============================================================
DROP TABLE IF EXISTS `t_student`;
CREATE TABLE `t_student` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_no`    VARCHAR(32)  NOT NULL COMMENT '学号',
    `name`          VARCHAR(64)  NOT NULL COMMENT '姓名',
    `gender`        VARCHAR(8)   DEFAULT NULL COMMENT '性别',
    `college`       VARCHAR(128) DEFAULT NULL COMMENT '学院',
    `major`         VARCHAR(128) DEFAULT NULL COMMENT '专业',
    `class_name`    VARCHAR(128) DEFAULT NULL COMMENT '班级',
    `grade`         VARCHAR(16)  DEFAULT NULL COMMENT '年级',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_college` (`college`),
    KEY `idx_major` (`major`),
    KEY `idx_grade` (`grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';


-- ============================================================
-- 3. 课程表
-- ============================================================
DROP TABLE IF EXISTS `t_course`;
CREATE TABLE `t_course` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_no`     VARCHAR(32)    NOT NULL COMMENT '课程编号',
    `course_name`   VARCHAR(256)   NOT NULL COMMENT '课程名称',
    `teacher_id`    BIGINT         DEFAULT NULL COMMENT '授课教师ID',
    `credit`        DECIMAL(4,1)   DEFAULT 0.0 COMMENT '学分',
    `course_type`   VARCHAR(32)    DEFAULT NULL COMMENT '课程类型：必修/选修/公选',
    `semester`      VARCHAR(32)    DEFAULT NULL COMMENT '学期（如：2025-2026-1）',
    `description`   VARCHAR(1024)  DEFAULT NULL COMMENT '课程描述',
    `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_no` (`course_no`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_semester` (`semester`),
    KEY `idx_course_type` (`course_type`),
    CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `t_teacher` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';


-- ============================================================
-- 4. 课程-学生选课关联表
-- ============================================================
DROP TABLE IF EXISTS `t_course_student`;
CREATE TABLE `t_course_student` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_id`     BIGINT       NOT NULL COMMENT '课程ID',
    `student_id`    BIGINT       NOT NULL COMMENT '学生ID',
    `semester`      VARCHAR(32)  DEFAULT NULL COMMENT '选课学期',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除（0=正常，1=退课）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_student_id` (`student_id`),
    CONSTRAINT `fk_cs_course`  FOREIGN KEY (`course_id`)  REFERENCES `t_course` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_cs_student` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程-学生选课关联表';


-- ============================================================
-- 5. 评价指标表（支持两级指标体系，parent_id 自引用）
-- ============================================================
DROP TABLE IF EXISTS `t_evaluation_indicator`;
CREATE TABLE `t_evaluation_indicator` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `indicator_no`    VARCHAR(16)    NOT NULL COMMENT '指标编号',
    `indicator_name`  VARCHAR(256)   NOT NULL COMMENT '指标名称',
    `category`        VARCHAR(64)    DEFAULT NULL COMMENT '指标类别：教学态度/教学内容/教学方法/教学效果',
    `weight`          DECIMAL(5,4)   DEFAULT 0.0000 COMMENT '权重（如0.2500表示25%）',
    `parent_id`       BIGINT         DEFAULT NULL COMMENT '父级指标ID（NULL=一级指标）',
    `level`           TINYINT        DEFAULT 1 COMMENT '指标层级：1-一级指标，2-二级指标',
    `description`     VARCHAR(512)   DEFAULT NULL COMMENT '指标说明/评分标准',
    `sort_order`      INT            DEFAULT 0 COMMENT '排序号',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`         TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_indicator_no` (`indicator_no`),
    KEY `idx_category` (`category`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_sort_order` (`sort_order`),
    CONSTRAINT `fk_indicator_parent` FOREIGN KEY (`parent_id`) REFERENCES `t_evaluation_indicator` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标表';


-- ============================================================
-- 6. 评价分数表（核心事实表）
-- ============================================================
DROP TABLE IF EXISTS `t_evaluation_score`;
CREATE TABLE `t_evaluation_score` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_id`       BIGINT         NOT NULL COMMENT '课程ID',
    `student_id`      BIGINT         NOT NULL COMMENT '学生ID',
    `indicator_id`    BIGINT         NOT NULL COMMENT '评价指标ID',
    `score`           DECIMAL(5,2)   DEFAULT NULL COMMENT '评分（0-100）',
    `comment`         VARCHAR(1024)  DEFAULT NULL COMMENT '评价意见/文本评价',
    `semester`        VARCHAR(32)    DEFAULT NULL COMMENT '学期',
    `evaluate_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `deleted`         TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_score` (`course_id`, `student_id`, `indicator_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_indicator_id` (`indicator_id`),
    KEY `idx_semester` (`semester`),
    KEY `idx_course_semester` (`course_id`, `semester`),
    CONSTRAINT `fk_score_course`    FOREIGN KEY (`course_id`)    REFERENCES `t_course` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_score_student`   FOREIGN KEY (`student_id`)   REFERENCES `t_student` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_score_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `t_evaluation_indicator` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价分数表';


-- ============================================================
-- 7. 数据源配置表（多源数据导入）
-- ============================================================
DROP TABLE IF EXISTS `t_data_source`;
CREATE TABLE `t_data_source` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_name`   VARCHAR(128)   NOT NULL COMMENT '数据源名称',
    `source_type`   VARCHAR(32)    NOT NULL COMMENT '数据源类型：EXCEL/TXT/CSV/DB/API',
    `file_path`     VARCHAR(512)   DEFAULT NULL COMMENT '文件路径',
    `description`   VARCHAR(512)   DEFAULT NULL COMMENT '数据源说明',
    `status`        VARCHAR(16)    DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_source_type` (`source_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';


-- ============================================================
-- 8. 数据导入日志表
-- ============================================================
DROP TABLE IF EXISTS `t_data_import_log`;
CREATE TABLE `t_data_import_log` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_id`     BIGINT         DEFAULT NULL COMMENT '数据源ID',
    `file_name`     VARCHAR(256)   DEFAULT NULL COMMENT '导入文件名',
    `import_type`   VARCHAR(32)    DEFAULT NULL COMMENT '导入类型：TEACHER/STUDENT/COURSE/SCORE/INDICATOR',
    `total_rows`    INT            DEFAULT 0 COMMENT '总行数',
    `success_rows`  INT            DEFAULT 0 COMMENT '成功行数',
    `fail_rows`     INT            DEFAULT 0 COMMENT '失败行数',
    `status`        VARCHAR(16)    DEFAULT NULL COMMENT '状态：SUCCESS/PARTIAL/FAILED',
    `error_msg`     TEXT           DEFAULT NULL COMMENT '错误信息',
    `import_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_import_type` (`import_type`),
    KEY `idx_status` (`status`),
    KEY `idx_import_time` (`import_time`),
    CONSTRAINT `fk_log_source` FOREIGN KEY (`source_id`) REFERENCES `t_data_source` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导入日志表';


-- ============================================================
-- 9. 评价报告表
-- ============================================================
DROP TABLE IF EXISTS `t_report`;
CREATE TABLE `t_report` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_name`     VARCHAR(256)   NOT NULL COMMENT '报告名称',
    `report_type`     VARCHAR(32)    DEFAULT NULL COMMENT '报告类型：TEACHER/COURSE/COLLEGE/SEMESTER',
    `semester`        VARCHAR(32)    DEFAULT NULL COMMENT '学期',
    `course_id`       BIGINT         DEFAULT NULL COMMENT '关联课程ID',
    `teacher_id`      BIGINT         DEFAULT NULL COMMENT '关联教师ID',
    `summary`         VARCHAR(2048)  DEFAULT NULL COMMENT '报告摘要',
    `report_data`     JSON           DEFAULT NULL COMMENT '报告详细数据（JSON格式）',
    `ai_analysis`     TEXT           DEFAULT NULL COMMENT 'AI分析结果文本',
    `generate_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `deleted`         TINYINT        DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_semester` (`semester`),
    CONSTRAINT `fk_report_course`  FOREIGN KEY (`course_id`)  REFERENCES `t_course` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT `fk_report_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `t_teacher` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价报告表';


-- ============================================================
-- 10. AI分析结果表
-- ============================================================
DROP TABLE IF EXISTS `t_ai_analysis_result`;
CREATE TABLE `t_ai_analysis_result` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `analysis_type`   VARCHAR(32)  DEFAULT NULL COMMENT '分析类型：TEXT_ANALYSIS/SENTIMENT/TREND/SUGGESTION',
    `target_id`       BIGINT       DEFAULT NULL COMMENT '分析目标ID',
    `target_type`     VARCHAR(32)  DEFAULT NULL COMMENT '目标类型：TEACHER/COURSE/REPORT',
    `result_data`     JSON         DEFAULT NULL COMMENT 'AI分析结果（JSON格式）',
    `model_name`      VARCHAR(64)  DEFAULT NULL COMMENT 'AI模型名称',
    `analysis_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_analysis_type` (`analysis_type`),
    KEY `idx_analysis_time` (`analysis_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析结果表';


-- ============================================================
-- 初始化数据：默认评价指标体系（4个一级指标 + 各4个二级指标）
-- 权重设计：教学态度25% / 教学内容25% / 教学方法25% / 教学效果25%
-- ============================================================

-- 一级指标
INSERT INTO `t_evaluation_indicator` (`indicator_no`, `indicator_name`, `category`, `weight`, `parent_id`, `level`, `description`, `sort_order`) VALUES
('I01', '教学态度', '教学态度', 0.2500, NULL, 1, '考察教师的教学责任心、备课充分程度、课堂纪律管理等方面', 1),
('I02', '教学内容', '教学内容', 0.2500, NULL, 1, '考察课程内容的科学性、前沿性、系统性以及理论与实际结合程度', 2),
('I03', '教学方法', '教学方法', 0.2500, NULL, 1, '考察教学手段多样性、互动性、启发式教学及信息技术应用', 3),
('I04', '教学效果', '教学效果', 0.2500, NULL, 1, '考察学生学习收获、能力提升、学习兴趣激发等综合效果', 4);

-- 教学态度 二级指标
INSERT INTO `t_evaluation_indicator` (`indicator_no`, `indicator_name`, `category`, `weight`, `parent_id`, `level`, `description`, `sort_order`) VALUES
('I01-01', '备课充分，授课认真',     '教学态度', 0.0625, 1, 2, '教师课前准备充分，授课态度认真负责', 1),
('I01-02', '按时上下课，不随意调停课', '教学态度', 0.0625, 1, 2, '严格遵守教学时间安排，不随意调整或取消课程', 2),
('I01-03', '注重课堂纪律管理',       '教学态度', 0.0625, 1, 2, '有效管理课堂秩序，营造良好学习氛围', 3),
('I01-04', '耐心答疑，关注学生反馈',   '教学态度', 0.0625, 1, 2, '积极回应学生疑问，重视学生意见和建议', 4);

-- 教学内容 二级指标
INSERT INTO `t_evaluation_indicator` (`indicator_no`, `indicator_name`, `category`, `weight`, `parent_id`, `level`, `description`, `sort_order`) VALUES
('I02-01', '内容充实，信息量适中',     '教学内容', 0.0625, 2, 2, '课程内容饱满，信息密度合理，不空洞不堆砌', 1),
('I02-02', '理论联系实际，案例丰富',   '教学内容', 0.0625, 2, 2, '能将理论与实际应用相结合，案例选取恰当', 2),
('I02-03', '内容前沿，反映学科发展',   '教学内容', 0.0625, 2, 2, '教学内容能反映学科新进展、新成果和新趋势', 3),
('I02-04', '重点突出，条理清晰',      '教学内容', 0.0625, 2, 2, '重难点讲解清晰，逻辑层次分明', 4);

-- 教学方法 二级指标
INSERT INTO `t_evaluation_indicator` (`indicator_no`, `indicator_name`, `category`, `weight`, `parent_id`, `level`, `description`, `sort_order`) VALUES
('I03-01', '教学方法灵活多样',       '教学方法', 0.0625, 3, 2, '综合运用讲授、讨论、案例等多种教学方法', 1),
('I03-02', '启发式教学，引导思考',   '教学方法', 0.0625, 3, 2, '善于启发学生思维，鼓励独立思考和创新', 2),
('I03-03', '课堂互动充分',         '教学方法', 0.0625, 3, 2, '师生互动、生生互动丰富，课堂氛围活跃', 3),
('I03-04', '合理运用信息技术手段',   '教学方法', 0.0625, 3, 2, '有效利用多媒体、在线平台等信息技术辅助教学', 4);

-- 教学效果 二级指标
INSERT INTO `t_evaluation_indicator` (`indicator_no`, `indicator_name`, `category`, `weight`, `parent_id`, `level`, `description`, `sort_order`) VALUES
('I04-01', '知识理解与掌握程度',     '教学效果', 0.0625, 4, 2, '学生对课程核心知识点的理解和掌握情况', 1),
('I04-02', '分析解决问题能力提升',   '教学效果', 0.0625, 4, 2, '课程对学生分析问题和解决问题能力的培养效果', 2),
('I04-03', '学习兴趣与主动性激发',   '教学效果', 0.0625, 4, 2, '课程是否激发了学生进一步学习和探究的兴趣', 3),
('I04-04', '整体满意度',           '教学效果', 0.0625, 4, 2, '学生对课程教学的整体满意程度', 4);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
