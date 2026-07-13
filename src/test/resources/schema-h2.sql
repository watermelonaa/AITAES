-- ============================================================
-- AITAES 测试数据库初始化（H2 兼容语法）
-- 版本：v3.0 | 每次集成测试前自动执行
-- ============================================================

-- 先 DROP 所有表（子表优先，避免外键约束阻止删除）
DROP TABLE IF EXISTS t_record_kp_deduction;
DROP TABLE IF EXISTS t_assessment_record;
DROP TABLE IF EXISTS t_assessment;
DROP TABLE IF EXISTS t_student_wrong_question;
DROP TABLE IF EXISTS t_student_kp_mastery;
DROP TABLE IF EXISTS t_knowledge_point;
DROP TABLE IF EXISTS t_attendance;
DROP TABLE IF EXISTS t_experiment;
DROP TABLE IF EXISTS t_question_bank;
DROP TABLE IF EXISTS t_warning_record;
DROP TABLE IF EXISTS t_warning_rule;
DROP TABLE IF EXISTS t_notification;
DROP TABLE IF EXISTS t_data_import_log;
DROP TABLE IF EXISTS t_evaluation_score;
DROP TABLE IF EXISTS t_course_student;
DROP TABLE IF EXISTS t_report;
DROP TABLE IF EXISTS t_course;
DROP TABLE IF EXISTS t_evaluation_indicator;
DROP TABLE IF EXISTS t_data_source;
DROP TABLE IF EXISTS t_ai_analysis_result;
DROP TABLE IF EXISTS t_student;
DROP TABLE IF EXISTS t_teacher;
DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_system_config;

-- ============================================================
-- 0. 统一用户认证表
-- ============================================================
CREATE TABLE t_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(32)  NOT NULL UNIQUE,
    password        VARCHAR(256) NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    status          VARCHAR(16)  DEFAULT 'ACTIVE',
    first_login     TINYINT      DEFAULT 1,
    last_login_time TIMESTAMP    DEFAULT NULL,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
);

-- ============================================================
-- 0b. 系统参数配置表
-- ============================================================
CREATE TABLE t_system_config (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key    VARCHAR(64)  NOT NULL UNIQUE,
    config_value  VARCHAR(512) NOT NULL,
    config_type   VARCHAR(32)  DEFAULT 'STRING',
    description   VARCHAR(256) DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 1. 教师表
-- ============================================================
CREATE TABLE t_teacher (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,
    teacher_no    VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(64)  NOT NULL,
    gender        VARCHAR(8)   DEFAULT NULL,
    college       VARCHAR(128) DEFAULT NULL,
    department    VARCHAR(128) DEFAULT NULL,
    title         VARCHAR(64)  DEFAULT NULL,
    email         VARCHAR(128) DEFAULT NULL,
    phone         VARCHAR(32)  DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- ============================================================
-- 2. 学生表
-- ============================================================
CREATE TABLE t_student (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,
    student_no    VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(64)  NOT NULL,
    gender        VARCHAR(8)   DEFAULT NULL,
    college       VARCHAR(128) DEFAULT NULL,
    major         VARCHAR(128) DEFAULT NULL,
    class_name    VARCHAR(128) DEFAULT NULL,
    grade         VARCHAR(16)  DEFAULT NULL,
    email         VARCHAR(128) DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- ============================================================
-- 3. 课程表 【v3.0新增 knowledge_point_list 字段】
-- ============================================================
CREATE TABLE t_course (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_no             VARCHAR(32)  NOT NULL UNIQUE,
    course_name           VARCHAR(256) NOT NULL,
    teacher_id            BIGINT       DEFAULT NULL,
    credit                DECIMAL(4,1) DEFAULT 0.0,
    course_type           VARCHAR(32)  DEFAULT NULL,
    semester              VARCHAR(32)  DEFAULT NULL,
    description           VARCHAR(1024) DEFAULT NULL,
    knowledge_point_list  TEXT         DEFAULT NULL,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted               TINYINT      DEFAULT 0,
    FOREIGN KEY (teacher_id) REFERENCES t_teacher(id)
);

-- ============================================================
-- 4. 课程-学生关联表 【v3.0新增 class_name 字段】
-- ============================================================
CREATE TABLE t_course_student (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT       NOT NULL,
    student_id    BIGINT       NOT NULL,
    class_name    VARCHAR(64)  DEFAULT NULL,
    semester      VARCHAR(32)  DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    UNIQUE (course_id, student_id),
    FOREIGN KEY (course_id)  REFERENCES t_course(id),
    FOREIGN KEY (student_id) REFERENCES t_student(id)
);

-- ============================================================
-- 5. [DEPRECATED] 评价指标表
-- ============================================================
CREATE TABLE t_evaluation_indicator (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_no    VARCHAR(16)  NOT NULL UNIQUE,
    indicator_name  VARCHAR(256) NOT NULL,
    category        VARCHAR(64)  DEFAULT NULL,
    weight          DECIMAL(5,4) DEFAULT 0.0000,
    parent_id       BIGINT       DEFAULT NULL,
    level           TINYINT      DEFAULT 1,
    description     VARCHAR(512) DEFAULT NULL,
    sort_order      INT          DEFAULT 0,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    FOREIGN KEY (parent_id) REFERENCES t_evaluation_indicator(id)
);

-- ============================================================
-- 6. [DEPRECATED] 评价分数表
-- ============================================================
CREATE TABLE t_evaluation_score (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT       NOT NULL,
    student_id      BIGINT       NOT NULL,
    indicator_id    BIGINT       NOT NULL,
    score           DECIMAL(5,2) DEFAULT NULL,
    comment         VARCHAR(1024) DEFAULT NULL,
    semester        VARCHAR(32)  DEFAULT NULL,
    evaluate_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    UNIQUE (course_id, student_id, indicator_id),
    FOREIGN KEY (course_id)    REFERENCES t_course(id),
    FOREIGN KEY (student_id)   REFERENCES t_student(id),
    FOREIGN KEY (indicator_id) REFERENCES t_evaluation_indicator(id)
);

-- ============================================================
-- 7. 数据源配置表
-- ============================================================
CREATE TABLE t_data_source (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name   VARCHAR(128) NOT NULL,
    source_type   VARCHAR(32)  NOT NULL,
    file_path     VARCHAR(512) DEFAULT NULL,
    description   VARCHAR(512) DEFAULT NULL,
    status        VARCHAR(16)  DEFAULT 'ACTIVE',
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0
);

-- ============================================================
-- 8. 数据导入日志表
-- ============================================================
CREATE TABLE t_data_import_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id     BIGINT       DEFAULT NULL,
    file_name     VARCHAR(256) DEFAULT NULL,
    import_type   VARCHAR(32)  DEFAULT NULL,
    total_rows    INT          DEFAULT 0,
    success_rows  INT          DEFAULT 0,
    fail_rows     INT          DEFAULT 0,
    status        VARCHAR(16)  DEFAULT NULL,
    error_msg     TEXT         DEFAULT NULL,
    import_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    FOREIGN KEY (source_id) REFERENCES t_data_source(id)
);

-- ============================================================
-- 9. 评价报告表
-- ============================================================
CREATE TABLE t_report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name     VARCHAR(256) NOT NULL,
    report_type     VARCHAR(32)  DEFAULT NULL,
    semester        VARCHAR(32)  DEFAULT NULL,
    course_id       BIGINT       DEFAULT NULL,
    teacher_id      BIGINT       DEFAULT NULL,
    summary         VARCHAR(2048) DEFAULT NULL,
    report_data     TEXT         DEFAULT NULL,
    ai_analysis     TEXT         DEFAULT NULL,
    generate_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    FOREIGN KEY (course_id)  REFERENCES t_course(id),
    FOREIGN KEY (teacher_id) REFERENCES t_teacher(id)
);

-- ============================================================
-- 10. AI分析结果表
-- ============================================================
CREATE TABLE t_ai_analysis_result (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_type   VARCHAR(32)  DEFAULT NULL,
    target_id       BIGINT       DEFAULT NULL,
    target_type     VARCHAR(32)  DEFAULT NULL,
    result_data     TEXT         DEFAULT NULL,
    model_name      VARCHAR(64)  DEFAULT NULL,
    analysis_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
);

-- ╔══════════════════════════════════════════════════════════╗
-- ║         v3.0 新增表（教学分析评价系统核心）                ║
-- ╚══════════════════════════════════════════════════════════╝

-- ============================================================
-- 11. 课程知识点库
-- ============================================================
CREATE TABLE t_knowledge_point (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT       NOT NULL,
    kp_name       VARCHAR(256) NOT NULL,
    kp_category   VARCHAR(128) DEFAULT NULL,
    parent_id     BIGINT       DEFAULT NULL,
    level         TINYINT      DEFAULT 1,
    difficulty    VARCHAR(16)  DEFAULT 'MEDIUM',
    description   VARCHAR(512) DEFAULT NULL,
    sort_order    INT          DEFAULT 0,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    UNIQUE (course_id, kp_name),
    FOREIGN KEY (course_id) REFERENCES t_course(id),
    FOREIGN KEY (parent_id) REFERENCES t_knowledge_point(id)
);

-- ============================================================
-- 12. 考核表
-- ============================================================
CREATE TABLE t_assessment (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id         BIGINT       NOT NULL,
    assessment_name   VARCHAR(256) NOT NULL,
    assessment_type   VARCHAR(32)  NOT NULL,
    assessment_no     INT          DEFAULT NULL,
    total_score       DECIMAL(5,2) DEFAULT 100.00,
    question_count    INT          DEFAULT 0,
    assessment_date   DATE         DEFAULT NULL,
    semester          VARCHAR(32)  DEFAULT NULL,
    description       VARCHAR(1024) DEFAULT NULL,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0,
    UNIQUE (course_id, assessment_name),
    FOREIGN KEY (course_id) REFERENCES t_course(id)
);

-- ============================================================
-- 13. 学生考核成绩记录
-- ============================================================
CREATE TABLE t_assessment_record (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id     BIGINT       NOT NULL,
    student_id        BIGINT       NOT NULL,
    total_score       DECIMAL(5,2) DEFAULT NULL,
    is_late           TINYINT      DEFAULT 0,
    is_absent         TINYINT      DEFAULT 0,
    weakest_kp        VARCHAR(512) DEFAULT NULL,
    weakest_aspect    VARCHAR(256) DEFAULT NULL,
    submit_time       TIMESTAMP    DEFAULT NULL,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0,
    UNIQUE (assessment_id, student_id),
    FOREIGN KEY (assessment_id) REFERENCES t_assessment(id),
    FOREIGN KEY (student_id)    REFERENCES t_student(id)
);

-- ============================================================
-- 14. 扣分知识点明细
-- ============================================================
CREATE TABLE t_record_kp_deduction (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id         BIGINT       NOT NULL,
    question_no       INT          NOT NULL,
    question_score    DECIMAL(5,2) DEFAULT NULL,
    max_score         DECIMAL(5,2) DEFAULT NULL,
    deduction_kp      VARCHAR(512) NOT NULL,
    deduction_aspect  VARCHAR(256) DEFAULT NULL,
    deduction_detail  VARCHAR(1024) DEFAULT NULL,
    deleted           TINYINT      DEFAULT 0,
    FOREIGN KEY (record_id) REFERENCES t_assessment_record(id)
);

-- ============================================================
-- 15. 学生知识点掌握度
-- ============================================================
CREATE TABLE t_student_kp_mastery (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id            BIGINT       NOT NULL,
    course_id             BIGINT       NOT NULL,
    kp_name               VARCHAR(256) NOT NULL,
    mastery_rate          DECIMAL(5,2) DEFAULT NULL,
    lose_count            INT          DEFAULT 0,
    total_question_count  INT          DEFAULT 0,
    last_assessment_id    BIGINT       DEFAULT NULL,
    last_updated          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted               TINYINT      DEFAULT 0,
    UNIQUE (student_id, course_id, kp_name),
    FOREIGN KEY (student_id) REFERENCES t_student(id),
    FOREIGN KEY (course_id)  REFERENCES t_course(id)
);

-- ============================================================
-- 16. 考勤记录表
-- ============================================================
CREATE TABLE t_attendance (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id         BIGINT      NOT NULL,
    student_id        BIGINT      NOT NULL,
    attendance_date   DATE        NOT NULL,
    status            VARCHAR(16) NOT NULL,
    week_no           INT         DEFAULT NULL,
    period            VARCHAR(32) DEFAULT NULL,
    semester          VARCHAR(32) DEFAULT NULL,
    remark            VARCHAR(256) DEFAULT NULL,
    create_time       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT     DEFAULT 0,
    UNIQUE (course_id, student_id, attendance_date),
    FOREIGN KEY (course_id)  REFERENCES t_course(id),
    FOREIGN KEY (student_id) REFERENCES t_student(id)
);

-- ============================================================
-- 17. 实验报告表
-- ============================================================
CREATE TABLE t_experiment (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id             BIGINT       NOT NULL,
    student_id            BIGINT       NOT NULL,
    experiment_name       VARCHAR(256) NOT NULL,
    experiment_no         INT          DEFAULT NULL,
    score                 DECIMAL(5,2) DEFAULT NULL,
    submit_time           TIMESTAMP    DEFAULT NULL,
    knowledge_point_ids   VARCHAR(1024) DEFAULT NULL,
    semester              VARCHAR(32)  DEFAULT NULL,
    remark                VARCHAR(512) DEFAULT NULL,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted               TINYINT      DEFAULT 0,
    FOREIGN KEY (course_id)  REFERENCES t_course(id),
    FOREIGN KEY (student_id) REFERENCES t_student(id)
);

-- ============================================================
-- 18. 预警规则配置表
-- ============================================================
CREATE TABLE t_warning_rule (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name     VARCHAR(128) NOT NULL,
    rule_type     VARCHAR(32)  NOT NULL,
    threshold     VARCHAR(128) NOT NULL,
    is_active     TINYINT      DEFAULT 1,
    description   VARCHAR(256) DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0
);

-- ============================================================
-- 19. 预警记录表
-- ============================================================
CREATE TABLE t_warning_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT       NOT NULL,
    course_id     BIGINT       NOT NULL,
    rule_id       BIGINT       DEFAULT NULL,
    warning_type  VARCHAR(32)  NOT NULL,
    severity      VARCHAR(8)   NOT NULL,
    warning_msg   VARCHAR(512) NOT NULL,
    is_resolved   TINYINT      DEFAULT 0,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES t_student(id),
    FOREIGN KEY (course_id)  REFERENCES t_course(id)
);

-- ============================================================
-- 20. 通知表
-- ============================================================
CREATE TABLE t_notification (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id         BIGINT       DEFAULT NULL,
    sender_type       VARCHAR(16)  DEFAULT 'TEACHER',
    title             VARCHAR(256) NOT NULL,
    content           TEXT         NOT NULL,
    recipient_type    VARCHAR(16)  NOT NULL,
    recipient_id      BIGINT       DEFAULT NULL,
    notification_type VARCHAR(32)  DEFAULT 'MANUAL',
    is_read           TINYINT      DEFAULT 0,
    read_time         TIMESTAMP    DEFAULT NULL,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0
);

-- ============================================================
-- 21. 题库表
-- ============================================================
CREATE TABLE t_question_bank (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id         BIGINT       NOT NULL,
    teacher_id        BIGINT       NOT NULL,
    question_type     VARCHAR(32)  NOT NULL,
    difficulty        VARCHAR(16)  DEFAULT 'MEDIUM',
    content           TEXT         NOT NULL,
    knowledge_points  VARCHAR(512) DEFAULT NULL,
    ai_generated      TINYINT      DEFAULT 0,
    quality_score     DECIMAL(3,2) DEFAULT NULL,
    quality_detail    VARCHAR(1024) DEFAULT NULL,
    usage_count       INT          DEFAULT 0,
    status            VARCHAR(16)  DEFAULT 'DRAFT',
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0,
    FOREIGN KEY (course_id)  REFERENCES t_course(id),
    FOREIGN KEY (teacher_id) REFERENCES t_teacher(id)
);

-- ============================================================
-- 22. 错题本
-- ============================================================
CREATE TABLE t_student_wrong_question (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id        BIGINT       NOT NULL,
    course_id         BIGINT       NOT NULL,
    question_content  TEXT         DEFAULT NULL,
    student_answer    VARCHAR(2048) DEFAULT NULL,
    correct_answer    VARCHAR(2048) DEFAULT NULL,
    knowledge_points  VARCHAR(512) DEFAULT NULL,
    analysis          TEXT         DEFAULT NULL,
    wrong_count       INT          DEFAULT 1,
    source            VARCHAR(32)  DEFAULT 'ASSESSMENT',
    source_id         BIGINT       DEFAULT NULL,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES t_student(id),
    FOREIGN KEY (course_id)  REFERENCES t_course(id)
);
