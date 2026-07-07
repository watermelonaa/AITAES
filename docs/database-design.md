# AITAES 数据库设计文档

> **项目名称**：基于AI的数智化教学分析评价系统  
> **数据库名**：`aitaes_db`  
> **字符集**：utf8mb4 / utf8mb4_unicode_ci  
> **存储引擎**：InnoDB  
> **版本**：v2.0 | 最后更新：2026-07-07

---

## 一、数据库 ER 图

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│   t_teacher  │       │  t_course_student │       │   t_student  │
│──────────────│       │──────────────────│       │──────────────│
│ PK id        │──┐    │ PK id            │    ┌──│ PK id        │
│    teacher_no│  │    │ FK course_id ────│──┐ │  │    student_no│
│    name      │  │    │ FK student_id ───│──│──┘  │    name      │
│    college   │  │    │    semester      │  │    │    college   │
│    title     │  │    │    deleted       │  │    │    major     │
│    deleted   │  │    └──────────────────┘  │    │    deleted   │
└──────────────┘  │           │ CASCADE      │    └──────────────┘
       │          │           │ CASCADE             │ CASCADE
       │          │    ┌──────────────┐              │
       │          └───→│   t_course   │←─────────────┘
       │    SET NULL   │──────────────│
       │               │ PK id        │
       │          ┌────│    course_no │
       │          │    │    course_name│
       │          │    │ FK teacher_id│──→ t_teacher.id
       │          │    │    semester  │
       │          │    │    deleted   │
       │          │    └──────────────┘
       │          │           │ CASCADE
       │          │           ▼
       │          │    ┌────────────────────┐       ┌─────────────────────────┐
       │          │    │ t_evaluation_score │       │ t_evaluation_indicator  │
       │          │    │────────────────────│       │─────────────────────────│
       │          │    │ PK id              │       │ PK id                   │
       │          │    │ FK course_id       │──→ t_course.id    │ indicator_no    │
       │          │    │ FK student_id      │──→ t_student.id   │ indicator_name  │
       │          │    │ FK indicator_id ───│──────│ FK indicator_id (RESTRICT)│
       │          │    │    score (0-100)   │       │    category            │
       │          │    │    comment         │       │    weight              │
       │          │    │    deleted         │       │ FK parent_id ──◎ self  │
       │          │    └────────────────────┘       │    level (1级/2级)      │
       │          │                                 │    deleted              │
       │          │                                 └─────────────────────────┘
       │          │
       │          │    ┌──────────────────┐
       │          │    │    t_report      │
       │          │    │──────────────────│
       │          │    │ PK id            │
       │          └───→│ FK course_id     │──→ t_course.id   (SET NULL)
       │    SET NULL   │ FK teacher_id ───│──→ t_teacher.id (SET NULL)
       │               │    report_data   │ (JSON)
       │               │    ai_analysis   │
       │               │    deleted       │
       │               └──────────────────┘
       │
       │    ┌──────────────────┐       ┌──────────────────────┐
       └───→│ t_ai_analysis_   │       │  t_data_source       │
            │     result       │       │──────────────────────│
            │──────────────────│       │ PK id                │
            │ PK id            │       │    source_name       │
            │    target_type   │       │    source_type       │
            │    target_id ────│──→ 多态关联                  │
            │    result_data   │       │    file_path          │
            │    model_name    │       │    status             │
            │    deleted       │       │    deleted            │
            └──────────────────┘       └──────────┬───────────┘
                                                   │ SET NULL
                                                   ▼
                                          ┌──────────────────────┐
                                          │  t_data_import_log   │
                                          │──────────────────────│
                                          │ PK id                │
                                          │ FK source_id         │
                                          │    file_name         │
                                          │    import_type       │
                                          │    total/success/fail│
                                          │    deleted           │
                                          └──────────────────────┘
```

---

## 二、表结构详细设计

> 所有表均包含 `deleted TINYINT DEFAULT 0` 字段，由 MyBatis-Plus 全局 `logic-delete-field` 自动管理。

### 表 1：`t_teacher` — 教师表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `teacher_no` | VARCHAR(32) | NOT NULL, **UNIQUE** | 工号 |
| `name` | VARCHAR(64) | NOT NULL | 姓名 |
| `gender` | VARCHAR(8) | — | 性别 |
| `college` | VARCHAR(128) | — | 学院 |
| `department` | VARCHAR(128) | — | 系/部门 |
| `title` | VARCHAR(64) | — | 职称（讲师/副教授/教授等） |
| `email` | VARCHAR(128) | — | 邮箱 |
| `phone` | VARCHAR(32) | — | 联系电话 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**索引**：`uk_teacher_no`(UNIQUE), `idx_college`, `idx_name`

**被引用**：
| 子表 | 外键 | 删除策略 |
|------|------|---------|
| t_course | teacher_id | SET NULL — 删除教师后课程保留 |
| t_report | teacher_id | SET NULL — 删除教师后报告保留 |

---

### 表 2：`t_student` — 学生表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `student_no` | VARCHAR(32) | NOT NULL, **UNIQUE** | 学号 |
| `name` | VARCHAR(64) | NOT NULL | 姓名 |
| `gender` | VARCHAR(8) | — | 性别 |
| `college` | VARCHAR(128) | — | 学院 |
| `major` | VARCHAR(128) | — | 专业 |
| `class_name` | VARCHAR(128) | — | 班级 |
| `grade` | VARCHAR(16) | — | 年级（如 2025） |
| `email` | VARCHAR(128) | — | 邮箱 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**索引**：`uk_student_no`(UNIQUE), `idx_college`, `idx_major`, `idx_grade`

**被引用**：
| 子表 | 外键 | 删除策略 |
|------|------|---------|
| t_course_student | student_id | CASCADE — 删除学生时退课记录级联删除 |
| t_evaluation_score | student_id | CASCADE — 删除学生时评分级联删除 |

---

### 表 3：`t_course` — 课程表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `course_no` | VARCHAR(32) | NOT NULL, **UNIQUE** | 课程编号 |
| `course_name` | VARCHAR(256) | NOT NULL | 课程名称 |
| `teacher_id` | BIGINT | **FK** → t_teacher.id, SET NULL | 授课教师 |
| `credit` | DECIMAL(4,1) | DEFAULT 0.0 | 学分 |
| `course_type` | VARCHAR(32) | — | 必修 / 选修 / 公选 |
| `semester` | VARCHAR(32) | — | 学期（格式：2025-2026-1） |
| `description` | VARCHAR(1024) | — | 课程描述 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**外键**：`fk_course_teacher` → `t_teacher(id)` **ON DELETE SET NULL ON UPDATE CASCADE**

**索引**：`uk_course_no`(UNIQUE), `idx_teacher_id`, `idx_semester`, `idx_course_type`

**被引用**：
| 子表 | 外键 | 删除策略 |
|------|------|---------|
| t_course_student | course_id | CASCADE |
| t_evaluation_score | course_id | CASCADE |
| t_report | course_id | SET NULL |

---

### 表 4：`t_course_student` — 选课关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `course_id` | BIGINT | NOT NULL, **FK** → t_course.id, CASCADE | 课程 |
| `student_id` | BIGINT | NOT NULL, **FK** → t_student.id, CASCADE | 学生 |
| `semester` | VARCHAR(32) | — | 选课学期 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除（退课标记） |

**外键**：
- `fk_cs_course` → `t_course(id)` **ON DELETE CASCADE ON UPDATE CASCADE**
- `fk_cs_student` → `t_student(id)` **ON DELETE CASCADE ON UPDATE CASCADE**

**索引**：`uk_course_student`(UNIQUE: course_id+student_id), `idx_course_id`, `idx_student_id`

---

### 表 5：`t_evaluation_indicator` — 评价指标表

支持**两级指标体系**，通过 `parent_id` 自引用实现。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `indicator_no` | VARCHAR(16) | NOT NULL, **UNIQUE** | 指标编号（如 I02-03） |
| `indicator_name` | VARCHAR(256) | NOT NULL | 指标名称 |
| `category` | VARCHAR(64) | — | 所属类别 |
| `weight` | DECIMAL(5,4) | DEFAULT 0.0000 | 权重（0.2500 = 25%） |
| `parent_id` | BIGINT | **FK→self**, SET NULL | 父级指标（NULL=一级） |
| `level` | TINYINT | DEFAULT 1 | 1=一级, 2=二级 |
| `description` | VARCHAR(512) | — | 评分标准说明 |
| `sort_order` | INT | DEFAULT 0 | 排序 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**外键**：`fk_indicator_parent` → `t_evaluation_indicator(id)` **ON DELETE SET NULL ON UPDATE CASCADE**

**索引**：`uk_indicator_no`(UNIQUE), `idx_category`, `idx_parent_id`, `idx_level`, `idx_sort_order`

**被引用**：
| 子表 | 外键 | 删除策略 |
|------|------|---------|
| t_evaluation_score | indicator_id | **RESTRICT** — 有评分数据时禁止删除指标 |

---

### 表 6：`t_evaluation_score` — 评价分数表（核心事实表）

一行 = 一个学生对一门课程的一个指标的一次评分。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `course_id` | BIGINT | NOT NULL, **FK** → t_course.id, CASCADE | 课程 |
| `student_id` | BIGINT | NOT NULL, **FK** → t_student.id, CASCADE | 评教学生 |
| `indicator_id` | BIGINT | NOT NULL, **FK** → t_evaluation_indicator.id, **RESTRICT** | 指标 |
| `score` | DECIMAL(5,2) | — | 评分（0-100） |
| `comment` | VARCHAR(1024) | — | 文字评价 / 主观意见 |
| `semester` | VARCHAR(32) | — | 学期 |
| `evaluate_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 评价时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**唯一约束**：`uk_score (course_id, student_id, indicator_id)` — 同一学生对同一课程的同一指标只能评一次

**外键**：
- `fk_score_course` → `t_course(id)` **ON DELETE CASCADE ON UPDATE CASCADE**
- `fk_score_student` → `t_student(id)` **ON DELETE CASCADE ON UPDATE CASCADE**
- `fk_score_indicator` → `t_evaluation_indicator(id)` **ON DELETE RESTRICT ON UPDATE CASCADE**

> `RESTRICT` 策略确保有评分数据的指标不会被误删，必须先清理关联评分。

**索引**：`uk_score`(UNIQUE), `idx_course_id`, `idx_student_id`, `idx_indicator_id`, `idx_semester`, `idx_course_semester`(course_id + semester 联合索引)

---

### 表 7：`t_data_source` — 数据源配置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `source_name` | VARCHAR(128) | NOT NULL | 数据源名称 |
| `source_type` | VARCHAR(32) | NOT NULL | EXCEL / TXT / CSV / DB / API |
| `file_path` | VARCHAR(512) | — | 文件路径 |
| `description` | VARCHAR(512) | — | 说明 |
| `status` | VARCHAR(16) | DEFAULT 'ACTIVE' | ACTIVE / INACTIVE |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**索引**：`idx_source_type`, `idx_status`

**被引用**：
| 子表 | 外键 | 删除策略 |
|------|------|---------|
| t_data_import_log | source_id | SET NULL — 删除数据源后日志保留 |

---

### 表 8：`t_data_import_log` — 数据导入日志表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `source_id` | BIGINT | **FK** → t_data_source.id, SET NULL | 数据源 |
| `file_name` | VARCHAR(256) | — | 导入文件名 |
| `import_type` | VARCHAR(32) | — | TEACHER / STUDENT / COURSE / SCORE / INDICATOR |
| `total_rows` | INT | DEFAULT 0 | 总行数 |
| `success_rows` | INT | DEFAULT 0 | 成功行数 |
| `fail_rows` | INT | DEFAULT 0 | 失败行数 |
| `status` | VARCHAR(16) | — | SUCCESS / PARTIAL / FAILED |
| `error_msg` | TEXT | — | 错误详情（上限 65535 字符，实际截断至 60000） |
| `import_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 导入时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**外键**：`fk_log_source` → `t_data_source(id)` **ON DELETE SET NULL ON UPDATE CASCADE**

**索引**：`idx_source_id`, `idx_import_type`, `idx_status`, `idx_import_time`

---

### 表 9：`t_report` — 评价报告表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `report_name` | VARCHAR(256) | NOT NULL | 报告名称 |
| `report_type` | VARCHAR(32) | — | TEACHER / COURSE / COLLEGE / SEMESTER |
| `semester` | VARCHAR(32) | — | 学期 |
| `course_id` | BIGINT | **FK** → t_course.id, SET NULL | 关联课程 |
| `teacher_id` | BIGINT | **FK** → t_teacher.id, SET NULL | 关联教师 |
| `summary` | VARCHAR(2048) | — | 报告摘要 |
| `report_data` | **JSON** | — | 完整评价数据 |
| `ai_analysis` | TEXT | — | AI 分析结果（A方向填充） |
| `generate_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 生成时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**外键**：
- `fk_report_course` → `t_course(id)` **ON DELETE SET NULL ON UPDATE CASCADE**
- `fk_report_teacher` → `t_teacher(id)` **ON DELETE SET NULL ON UPDATE CASCADE**

**索引**：`idx_report_type`, `idx_course_id`, `idx_teacher_id`, `idx_semester`

> `report_data` JSON 结构示例：
> ```json
> {
>   "overallScore": 87.35,
>   "totalStudents": 45,
>   "categoryScores": [
>     {"category": "教学态度", "score": 90.12, "weight": 0.25},
>     {"category": "教学内容", "score": 85.60, "weight": 0.25}
>   ],
>   "indicatorScores": [
>     {"indicatorNo": "I01-01", "indicatorName": "备课充分，授课认真",
>      "avgScore": 92.10, "weight": 0.0625, "weightedScore": 5.7563,
>      "evaluationCount": 45}
>   ]
> }
> ```

---

### 表 10：`t_ai_analysis_result` — AI 分析结果表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `analysis_type` | VARCHAR(32) | — | TEXT_ANALYSIS / SENTIMENT / TREND / SUGGESTION |
| `target_id` | BIGINT | — | 分析目标 ID |
| `target_type` | VARCHAR(32) | — | TEACHER / COURSE / REPORT |
| `result_data` | **JSON** | — | AI 分析结果 |
| `model_name` | VARCHAR(64) | — | AI 模型名称 |
| `analysis_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 分析时间 |
| `deleted` | TINYINT | DEFAULT 0 | 逻辑删除 |

**索引**：`idx_target`(target_type+target_id 联合索引), `idx_analysis_type`, `idx_analysis_time`

> `target_type` + `target_id` 构成多态关联，可指向 t_teacher、t_course 或 t_report。

---

## 三、外键约束汇总

| # | 约束名 | 子表 | 父表 | 删除策略 | 更新策略 |
|---|--------|------|------|---------|---------|
| 1 | `fk_course_teacher` | t_course.teacher_id | t_teacher.id | SET NULL | CASCADE |
| 2 | `fk_cs_course` | t_course_student.course_id | t_course.id | CASCADE | CASCADE |
| 3 | `fk_cs_student` | t_course_student.student_id | t_student.id | CASCADE | CASCADE |
| 4 | `fk_indicator_parent` | t_evaluation_indicator.parent_id | t_evaluation_indicator.id | SET NULL | CASCADE |
| 5 | `fk_score_course` | t_evaluation_score.course_id | t_course.id | CASCADE | CASCADE |
| 6 | `fk_score_student` | t_evaluation_score.student_id | t_student.id | CASCADE | CASCADE |
| 7 | `fk_score_indicator` | t_evaluation_score.indicator_id | t_evaluation_indicator.id | **RESTRICT** | CASCADE |
| 8 | `fk_log_source` | t_data_import_log.source_id | t_data_source.id | SET NULL | CASCADE |
| 9 | `fk_report_course` | t_report.course_id | t_course.id | SET NULL | CASCADE |
| 10 | `fk_report_teacher` | t_report.teacher_id | t_teacher.id | SET NULL | CASCADE |

### 删除策略释义

| 策略 | 含义 | 使用场景 |
|------|------|---------|
| **CASCADE** | 删除父记录时自动删除子记录 | 课程删了，评分/选课也没意义了 |
| **SET NULL** | 删除父记录时子表外键置为 NULL | 教师走了，课程和报告保留供回溯 |
| **RESTRICT** | 有子记录时禁止删除父记录 | 有评分数据的指标不能删，防数据破坏 |

---

## 四、评价指标体系（预置数据）

### 权重总览

```
教学态度 25% ─┬─ I01-01 备课充分，授课认真          6.25%
              ├─ I01-02 按时上下课，不随意调停课      6.25%
              ├─ I01-03 注重课堂纪律管理              6.25%
              └─ I01-04 耐心答疑，关注学生反馈        6.25%

教学内容 25% ─┬─ I02-01 内容充实，信息量适中          6.25%
              ├─ I02-02 理论联系实际，案例丰富        6.25%
              ├─ I02-03 内容前沿，反映学科发展        6.25%
              └─ I02-04 重点突出，条理清晰            6.25%

教学方法 25% ─┬─ I03-01 教学方法灵活多样              6.25%
              ├─ I03-02 启发式教学，引导思考          6.25%
              ├─ I03-03 课堂互动充分                  6.25%
              └─ I03-04 合理运用信息技术手段          6.25%

教学效果 25% ─┬─ I04-01 知识理解与掌握程度            6.25%
              ├─ I04-02 分析解决问题能力提升          6.25%
              ├─ I04-03 学习兴趣与主动性激发          6.25%
              └─ I04-04 整体满意度                    6.25%
             ─────────────────────────────────────────────
              合计：4 个一级指标 × 25% = 16 个二级指标 × 6.25% = 100%
```

### 综合评分公式

```
第1步：按指标分组求平均
  avgScore_i = AVG(t_evaluation_score.score WHERE indicator_id = i)

第2步：乘权重得加权分
  weightedScore_i = avgScore_i × weight_i

第3步：求和得综合分（0-100）
  overallScore = Σ weightedScore_i  （对所有16个二级指标求和）

第4步：归一化维度分（0-100）
  categoryScore = Σ weightedScore_j / categoryWeight
  （j ∈ 该维度下的4个二级指标）
```

**计算示例**（假设某课程所有16项均分均为85分）：

| 步骤 | 计算 | 结果 |
|------|------|------|
| 单项加权 | 85 × 0.0625 | 5.3125 |
| 综合得分 | 16 × 5.3125 | **85.00** |
| 教学态度维度 | (4 × 5.3125) ÷ 0.25 | **85.00** |

---

## 五、索引设计

| 表 | 索引名 | 字段 | 类型 | 用途 |
|----|--------|------|------|------|
| t_teacher | `uk_teacher_no` | teacher_no | UNIQUE | 工号唯一 |
| t_teacher | `idx_college` | college | NORMAL | 按学院查询 |
| t_teacher | `idx_name` | name | NORMAL | 按姓名搜索 |
| t_student | `uk_student_no` | student_no | UNIQUE | 学号唯一 |
| t_student | `idx_college` | college | NORMAL | 按学院查询 |
| t_student | `idx_major` | major | NORMAL | 按专业查询 |
| t_student | `idx_grade` | grade | NORMAL | 按年级查询 |
| t_course | `uk_course_no` | course_no | UNIQUE | 课程编号唯一 |
| t_course | `idx_teacher_id` | teacher_id | NORMAL | 按教师查课程 |
| t_course | `idx_semester` | semester | NORMAL | 按学期过滤 |
| t_course | `idx_course_type` | course_type | NORMAL | 按类型过滤 |
| t_course_student | `uk_course_student` | (course_id, student_id) | UNIQUE | 防重复选课 |
| t_course_student | `idx_course_id` | course_id | NORMAL | 查课程学生列表 |
| t_course_student | `idx_student_id` | student_id | NORMAL | 查学生选课列表 |
| t_evaluation_indicator | `uk_indicator_no` | indicator_no | UNIQUE | 指标编号唯一 |
| t_evaluation_indicator | `idx_category` | category | NORMAL | 按类别过滤 |
| t_evaluation_indicator | `idx_parent_id` | parent_id | NORMAL | 查子指标 |
| t_evaluation_indicator | `idx_level` | level | NORMAL | 按层级过滤 |
| t_evaluation_indicator | `idx_sort_order` | sort_order | NORMAL | 排序 |
| t_evaluation_score | `uk_score` | (course_id, student_id, indicator_id) | **UNIQUE** | 防重复评分 |
| t_evaluation_score | `idx_course_id` | course_id | NORMAL | 按课程查询 |
| t_evaluation_score | `idx_student_id` | student_id | NORMAL | 按学生查询 |
| t_evaluation_score | `idx_indicator_id` | indicator_id | NORMAL | 按指标查询 |
| t_evaluation_score | `idx_semester` | semester | NORMAL | 按学期过滤 |
| t_evaluation_score | `idx_course_semester` | **(course_id, semester)** | NORMAL | 高频组合查询 |
| t_data_source | `idx_source_type` | source_type | NORMAL | 按类型过滤 |
| t_data_source | `idx_status` | status | NORMAL | 按状态过滤 |
| t_data_import_log | `idx_source_id` | source_id | NORMAL | 按数据源查询 |
| t_data_import_log | `idx_import_type` | import_type | NORMAL | 按导入类型过滤 |
| t_data_import_log | `idx_status` | status | NORMAL | 按状态过滤 |
| t_data_import_log | `idx_import_time` | import_time | NORMAL | 按时间排序 |
| t_report | `idx_report_type` | report_type | NORMAL | 按报告类型过滤 |
| t_report | `idx_course_id` | course_id | NORMAL | 按课程查询 |
| t_report | `idx_teacher_id` | teacher_id | NORMAL | 按教师查询 |
| t_report | `idx_semester` | semester | NORMAL | 按学期过滤 |
| t_ai_analysis_result | `idx_target` | (target_type, target_id) | NORMAL | 按目标查询 |
| t_ai_analysis_result | `idx_analysis_type` | analysis_type | NORMAL | 按分析类型过滤 |
| t_ai_analysis_result | `idx_analysis_time` | analysis_time | NORMAL | 按时间排序 |

**总计：37 个索引**（9 个 UNIQUE + 28 个 NORMAL）

---

## 六、命名规范

| 规范项 | 规则 | 示例 |
|--------|------|------|
| 表名 | `t_` 前缀 + 小写下划线 | `t_evaluation_score` |
| 主键 | 统一使用 `id`，BIGINT AUTO_INCREMENT | `id` |
| 外键约束 | `fk_` + 子表简称 + `_` + 父表简称 | `fk_score_course` |
| 唯一索引 | `uk_` 前缀 | `uk_teacher_no` |
| 普通索引 | `idx_` 前缀 | `idx_semester` |
| 时间字段 | `_time` 后缀，DATETIME | `create_time`, `evaluate_time` |
| 逻辑删除 | 全部表统一 `deleted` 字段，MyBatis-Plus 全局管理 | `deleted TINYINT DEFAULT 0` |
