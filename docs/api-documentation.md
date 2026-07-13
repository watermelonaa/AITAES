# AITAES API 接口文档

> **项目**：基于AI的数智化教学分析评价系统  
> **Base URL**：`http://localhost:8080`  
> **版本**：v3.0 | 最后更新：2026-07-10

---

## 目录

- [一、通用规范](#一通用规范)
- [二、数据采集模块](#二数据采集模块)
- [三、数据源管理](#三数据源管理)
- [四、评价计算引擎](#四评价计算引擎)
- [五、报告管理](#五报告管理)
- [六、仪表盘](#六仪表盘)
- [七、Excel 导出](#七excel-导出)
- [八、全局异常处理](#八全局异常处理)
- [附录 A：接口速查表](#附录-a接口速查表)
- [附录 B：枚举值一览](#附录-b枚举值一览)

---

## 一、通用规范

### 1.1 统一响应格式

所有接口返回统一 JSON 结构，由 `Result<T>` 包装：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `int` | 业务状态码，与 HTTP 状态码保持一致 |
| `message` | `string` | 人类可读的提示信息 |
| `data` | `object` / `null` / `array` | 响应负载，无数据时为 `null` |

### 1.2 状态码定义

| code | 枚举常量 | 含义 |
|------|----------|------|
| `200` | `SUCCESS` | 操作成功 |
| `206` | `IMPORT_PARTIAL` | 部分成功（导入含失败行） |
| `400` | `BAD_REQUEST` | 请求参数错误 |
| `401` | `UNAUTHORIZED` | 未授权（预留） |
| `403` | `FORBIDDEN` | 无访问权限（预留） |
| `404` | `NOT_FOUND` | 资源不存在 |
| `500` | `INTERNAL_ERROR` | 服务器内部错误 |

> 所有异常由 `GlobalExceptionHandler` 统一拦截，保证前端始终收到上述 JSON 结构，不会出现 HTML 错误页或裸堆栈。

### 1.3 学期格式

统一使用 `YYYY-YYYY-N` 格式：

- `2025-2026-1` — 2025-2026 学年第一学期
- `2025-2026-2` — 2025-2026 学年第二学期

### 1.4 分页规范

分页接口统一使用 MyBatis-Plus `IPage` 分页对象，响应 `data` 结构如下：

```json
{
  "records": [ ... ],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | `array` | 当前页数据列表 |
| `total` | `long` | 总记录数 |
| `size` | `long` | 每页条数 |
| `current` | `long` | 当前页码 |
| `pages` | `long` | 总页数 |

---

## 二、数据采集模块

### 2.1 上传文件导入数据

```
POST /api/import/upload
Content-Type: multipart/form-data
```

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `File` | ✅ | 上传文件，支持 `.xlsx` / `.xls` / `.csv` |
| `importType` | `String` | ✅ | 导入类型，见下方枚举 |
| `sourceId` | `Long` | ❌ | 关联的数据源 ID，用于追踪数据来源 |

**importType 枚举**

| 值 | 导入目标表 | 唯一键 |
|----|-----------|--------|
| `TEACHER` | 教师表 | 工号 |
| `STUDENT` | 学生表 | 学号 |
| `COURSE` | 课程表 | 课程编号 |
| `SCORE` | 评价分数表（遗留） | 课程编号 + 学号 + 指标编号 |
| `INDICATOR` | 评价指标表（遗留） | 指标编号 |
| `CLASS_STUDENT` | 学生表 + 选课关联表 | 学号 + 课程编号 |
| `HOMEWORK` | 考核表 + 成绩记录 + 扣分明细 | 考核名称 + 学号 |
| `QUIZ` | 考核表 + 成绩记录 + 扣分明细 | 考核名称 + 学号 |
| `ATTENDANCE` | 考勤记录表 | 课程编号 + 学号 + 日期 |
| `EXPERIMENT` | 实验报告表 | — |
| `EXAM_SCORE` | 考核表 + 成绩记录 + 扣分明细（期中/期末） | 考核名称 + 学号 |
| `KNOWLEDGE_POINT` | 知识点库表 | 课程编号 + 知识点名称 |

**文件名约定**：所有导入类型的文件名格式为 `{课程编号}_{类型}_{描述}.xlsx`，系统从文件名自动解析课程关联。例如 `CS-NET-001_HOMEWORK_第1次作业.xlsx`。

**Excel 列模板**

| importType | 表头列（第一行必须与中文完全匹配） |
|------------|----------------------------------|
| `TEACHER` | 工号、姓名、性别、学院、系/部门、职称、邮箱、联系电话 |
| `STUDENT` | 学号、姓名、性别、学院、专业、班级、年级、邮箱 |
| `COURSE` | 课程编号、课程名称、授课教师工号、学分、课程类型、学期、课程描述 |
| `SCORE` | 课程编号、学号、指标编号、评分、评价意见、学期、评价时间 |
| `INDICATOR` | 指标编号、指标名称、指标类别、权重、父级指标编号、指标层级、指标说明、排序号 |
| `CLASS_STUDENT` | 学号、姓名、性别、学院、专业、班级、年级、邮箱 |
| `HOMEWORK` | 序号、学号、姓名、第1题得分、扣分知识点、…(动态题目列)、总成绩、最薄弱知识点 |
| `QUIZ` | 同 `HOMEWORK`（序号、学号、姓名、第1题得分、扣分知识点、…、总成绩、最薄弱知识点） |
| `ATTENDANCE` | 学号、姓名、日期、状态、第几周、节次、备注 |
| `EXPERIMENT` | 学号、姓名、实验名称、实验次数、分数、提交时间、备注 |
| `EXAM_SCORE` | 同 `HOMEWORK`（序号、学号、姓名、第1题得分、扣分知识点、…、总成绩、最薄弱知识点） |

**响应 — 全部成功** `200`

```json
{
  "code": 200,
  "message": "导入成功: 共100行",
  "data": {
    "totalRows": 100,
    "successRows": 100,
    "failRows": 0,
    "status": "SUCCESS",
    "errors": []
  }
}
```

**响应 — 部分成功** `200`（code=206）

```json
{
  "code": 206,
  "message": "部分导入成功: 成功80行, 失败20行",
  "data": {
    "totalRows": 100,
    "successRows": 80,
    "failRows": 20,
    "status": "PARTIAL",
    "errors": [
      "第3行: 工号已存在，已跳过",
      "第15行第2列: 数据格式错误 - 无法转换为数字"
    ]
  }
}
```

**响应 — 全部失败** `200`（code=500）

```json
{
  "code": 500,
  "message": "导入全部失败",
  "data": null
}
```

**ImportResultDTO 字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalRows` | `int` | 数据总行数 |
| `successRows` | `int` | 成功导入行数 |
| `failRows` | `int` | 失败行数 |
| `status` | `String` | `SUCCESS` / `PARTIAL` / `FAILED` |
| `errors` | `List<String>` | 错误详情，最多返回前 100 条 |

**业务规则**

- 每 500 行一个批次，批次独立提交事务，某批失败不影响其他批次
- 基于唯一键自动去重：重复行静默跳过，计入 `successRows`
- `errors` 最多返回 100 条，超出部分截断
- 导入成功后自动写入 `t_data_import_log` 日志表

---

## 三、数据源管理

### 3.1 分页查询数据源

```
GET /api/datasource?pageNum=1&pageSize=10&sourceType=EXCEL
```

| 参数 | 类型 | 默认值 | 必填 | 说明 |
|------|------|--------|------|------|
| `pageNum` | `int` | `1` | ❌ | 页码 |
| `pageSize` | `int` | `10` | ❌ | 每页条数 |
| `sourceType` | `String` | — | ❌ | 类型过滤，不传则查全部 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "sourceName": "教师信息Excel",
        "sourceType": "EXCEL",
        "filePath": "/uploads/teachers.xlsx",
        "description": "2025秋季学期教师数据",
        "status": "ACTIVE",
        "createTime": "2025-09-01T10:00:00",
        "updateTime": "2025-09-01T10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 3.2 查询数据源详情

```
GET /api/datasource/{id}
```

| 参数 | 位置 | 类型 | 说明 |
|------|------|------|------|
| `id` | Path | `Long` | 数据源 ID |

**响应** `data` 为单个 `DataSource` 对象，结构同 3.1 中 `records` 元素。

**异常**：资源不存在时返回 `404`。

### 3.3 新增数据源

```
POST /api/datasource
Content-Type: application/json
```

**请求体**

```json
{
  "sourceName": "教师信息Excel",
  "sourceType": "EXCEL",
  "filePath": "/uploads/teachers.xlsx",
  "description": "2025秋季学期教师数据",
  "status": "ACTIVE"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sourceName` | `String` | ✅ | 数据源名称 |
| `sourceType` | `String` | ✅ | `EXCEL` / `TXT` / `CSV` / `DB` / `API` |
| `filePath` | `String` | ❌ | 文件路径 |
| `description` | `String` | ❌ | 备注说明 |
| `status` | `String` | ❌ | `ACTIVE`（默认）/ `INACTIVE` |

**响应** `data` 为创建后的完整 `DataSource` 对象（含自增 ID 和时间戳）。

### 3.4 更新数据源

```
PUT /api/datasource/{id}
Content-Type: application/json
```

请求体字段同 3.3 新增，仅需传入需要更新的字段（但 `id` 由路径参数传入，无需在 body 中重复）。

### 3.5 删除数据源

```
DELETE /api/datasource/{id}
```

**响应** `data` 为 `null`，`code=200` 表示删除成功。

> 删除为逻辑删除（`deleted` 字段置 1），数据不会物理清除。

### 3.6 查询激活的数据源（下拉列表）

```
GET /api/datasource/active
```

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "sourceName": "教师信息Excel",
      "sourceType": "EXCEL",
      "status": "ACTIVE"
    }
  ]
}
```

> 仅返回 `status = 'ACTIVE'` 且未被逻辑删除的数据源。

---

## 四、评价计算引擎

> 所有评价接口在请求时实时计算，指标体系已缓存以提高性能。

### 4.1 课程综合评价

```
GET /api/evaluation/course/{courseId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `courseId` | Path | `Long` | ✅ | 课程 ID |
| `semester` | Query | `String` | ❌ | 学期过滤，不传则查所有学期 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "courseId": 1,
    "courseNo": "CS101",
    "courseName": "数据结构",
    "teacherId": 5,
    "teacherName": "张教授",
    "semester": "2025-2026-1",
    "totalStudents": 45,
    "overallScore": 87.35,
    "categoryScores": [
      {
        "category": "教学态度",
        "weight": 0.2500,
        "score": 90.12,
        "weightedScore": 22.5300
      },
      {
        "category": "教学内容",
        "weight": 0.2500,
        "score": 85.60,
        "weightedScore": 21.4000
      },
      {
        "category": "教学方法",
        "weight": 0.2500,
        "score": 86.80,
        "weightedScore": 21.7000
      },
      {
        "category": "教学效果",
        "weight": 0.2500,
        "score": 86.88,
        "weightedScore": 21.7200
      }
    ],
    "indicatorScores": [
      {
        "indicatorId": 5,
        "indicatorNo": "I01-01",
        "indicatorName": "备课充分，授课认真",
        "category": "教学态度",
        "level": 2,
        "parentId": 1,
        "avgScore": 92.10,
        "weight": 0.0625,
        "weightedScore": 5.7563,
        "evaluationCount": 45
      }
    ]
  }
}
```

**CourseEvaluationDTO 字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `courseId` | `Long` | 课程 ID |
| `courseNo` | `String` | 课程编号 |
| `courseName` | `String` | 课程名称 |
| `teacherId` | `Long` | 授课教师 ID |
| `teacherName` | `String` | 授课教师姓名 |
| `semester` | `String` | 学期 |
| `totalStudents` | `Integer` | 参评学生人数 |
| `overallScore` | `BigDecimal` | 综合得分（0-100），所有二级指标加权分之和 |
| `categoryScores` | `List<CategoryScoreDTO>` | 四个一级维度得分明细 |
| `indicatorScores` | `List<IndicatorScoreDTO>` | 全部二级指标得分明细 |

**CategoryScoreDTO 字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `category` | `String` | 维度名称（教学态度/教学内容/教学方法/教学效果） |
| `weight` | `BigDecimal` | 维度权重 |
| `score` | `BigDecimal` | 维度标准化得分（0-100）= 维度加权分之和 ÷ 维度权重 |
| `weightedScore` | `BigDecimal` | 维度加权分 = Σ(指标均分 × 指标权重) |

**IndicatorScoreDTO 字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `indicatorId` | `Long` | 指标 ID |
| `indicatorNo` | `String` | 指标编号 |
| `indicatorName` | `String` | 指标名称 |
| `category` | `String` | 所属维度 |
| `level` | `Integer` | 指标层级（1=一级, 2=二级） |
| `parentId` | `Long` | 父级指标 ID |
| `avgScore` | `BigDecimal` | 该指标所有学生评分的算术平均值 |
| `weight` | `BigDecimal` | 指标在体系中的全局权重 |
| `weightedScore` | `BigDecimal` | 加权得分 = avgScore × weight |
| `evaluationCount` | `Integer` | 参与评价的学生人数 |

### 4.2 教师综合评价

```
GET /api/evaluation/teacher/{teacherId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `teacherId` | Path | `Long` | ✅ | 教师 ID |
| `semester` | Query | `String` | ❌ | 学期过滤 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "teacherId": 5,
    "teacherNo": "T2021001",
    "name": "张教授",
    "college": "计算机学院",
    "title": "教授",
    "courseCount": 2,
    "averageOverallScore": 88.67,
    "categoryAverages": [
      { "category": "教学态度", "weight": 0.2500, "score": 91.20, "weightedScore": 22.8000 },
      { "category": "教学内容", "weight": 0.2500, "score": 87.50, "weightedScore": 21.8750 },
      { "category": "教学方法", "weight": 0.2500, "score": 88.30, "weightedScore": 22.0750 },
      { "category": "教学效果", "weight": 0.2500, "score": 87.68, "weightedScore": 21.9200 }
    ],
    "courseDetails": [
      {
        "courseId": 1,
        "courseNo": "CS101",
        "courseName": "数据结构",
        "teacherId": 5,
        "teacherName": "张教授",
        "overallScore": 87.35,
        ...
      },
      {
        "courseId": 2,
        "courseNo": "CS201",
        "courseName": "操作系统",
        "teacherId": 5,
        "teacherName": "张教授",
        "overallScore": 89.99,
        ...
      }
    ]
  }
}
```

**TeacherEvaluationDTO 字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| `teacherId` | `Long` | 教师 ID |
| `teacherNo` | `String` | 工号 |
| `name` | `String` | 教师姓名 |
| `college` | `String` | 所属学院 |
| `title` | `String` | 职称 |
| `courseCount` | `Integer` | 教授课程数 |
| `averageOverallScore` | `BigDecimal` | 所有课程综合分的算术平均值 |
| `categoryAverages` | `List<CategoryScoreDTO>` | 四维度在全部课程间的平均得分 |
| `courseDetails` | `List<CourseEvaluationDTO>` | 每门课的完整评价明细（结构同课程评价） |

### 4.3 学院排名

```
GET /api/evaluation/college?college=计算机学院&semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `college` | `String` | ❌ | 学院名称，不传则返回全校所有教师排名 |
| `semester` | `String` | ❌ | 学期过滤 |

**响应** `data` 为 `List<TeacherEvaluationDTO>` 数组，按 `averageOverallScore` 降序排列。

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "teacherId": 7,
      "teacherNo": "T2021003",
      "name": "李教授",
      "college": "计算机学院",
      "courseCount": 3,
      "averageOverallScore": 92.50,
      "categoryAverages": [ ... ],
      "courseDetails": [ ... ]
    },
    {
      "teacherId": 5,
      "teacherNo": "T2021001",
      "name": "张教授",
      "college": "计算机学院",
      "courseCount": 2,
      "averageOverallScore": 88.67,
      ...
    }
  ]
}
```

> 无评教数据的教师自动过滤，不会出现在排名中。

### 4.4 学期全系统概览

```
GET /api/evaluation/semester/{semester}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `semester` | Path | `String` | ✅ | 学期，如 `2025-2026-1` |

**响应** `data` 为 `List<TeacherEvaluationDTO>` 数组，包含该学期所有有评教数据的教师，按综合分降序。

---

## 五、报告管理

### 5.1 生成课程评价报告

```
POST /api/report/generate/course/{courseId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `courseId` | Path | `Long` | ✅ | 课程 ID |
| `semester` | Query | `String` | ❌ | 学期过滤 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 101,
    "reportName": "数据结构 - 评价报告",
    "reportType": "COURSE",
    "semester": "2025-2026-1",
    "courseId": 1,
    "teacherId": 5,
    "summary": "数据结构 综合得分 87.35 分（参评学生 45 人）",
    "reportData": "{ ... 完整评价 JSON ... }",
    "aiAnalysis": null,
    "generateTime": "2026-07-10T17:30:00"
  }
}
```

**Report 实体字段**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `Long` | 报告 ID（自增） |
| `reportName` | `String` | 报告名称 |
| `reportType` | `String` | `TEACHER` / `COURSE` / `COLLEGE` / `SEMESTER` |
| `semester` | `String` | 学期 |
| `courseId` | `Long` | 关联课程 ID（课程报告时填充） |
| `teacherId` | `Long` | 关联教师 ID（教师报告时填充） |
| `summary` | `String` | 报告摘要文本 |
| `reportData` | `String` | 报告详细数据（JSON 字符串） |
| `aiAnalysis` | `String` | AI 分析结果（可空） |
| `generateTime` | `LocalDateTime` | 报告生成时间 |

### 5.2 生成教师评价报告

```
POST /api/report/generate/teacher/{teacherId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `teacherId` | Path | `Long` | ✅ | 教师 ID |
| `semester` | Query | `String` | ❌ | 学期过滤 |

`reportType` = `TEACHER`，`teacherId` 字段填充，响应结构与 5.1 一致。

### 5.3 生成学院 / 全校报告

```
POST /api/report/generate/college?college=计算机学院&semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `college` | `String` | ❌ | 学院名称，不传则生成全校报告 |
| `semester` | `String` | ❌ | 学期过滤 |

`reportType` = `COLLEGE`，响应结构与 5.1 一致。

### 5.4 分页查询报告列表

```
GET /api/report?pageNum=1&pageSize=10&reportType=COURSE&semester=2025-2026-1
```

| 参数 | 类型 | 默认值 | 必填 | 说明 |
|------|------|--------|------|------|
| `pageNum` | `int` | `1` | ❌ | 页码 |
| `pageSize` | `int` | `10` | ❌ | 每页条数 |
| `reportType` | `String` | — | ❌ | `TEACHER` / `COURSE` / `COLLEGE` / `SEMESTER` |
| `semester` | `String` | — | ❌ | 学期过滤 |

**响应** `data` 为 `IPage<Report>` 分页对象。

### 5.5 查看报告详情

```
GET /api/report/{id}
```

**响应** `data` 为单个 `Report` 对象。

### 5.6 删除报告

```
DELETE /api/report/{id}
```

**响应** `data` 为 `null`。删除为逻辑删除。

---

## 六、仪表盘

### 6.1 仪表盘完整数据

```
GET /api/dashboard?semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `semester` | `String` | ❌ | 学期过滤 |

**响应**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "overview": {
      "totalTeachers": 120,
      "totalStudents": 3500,
      "totalCourses": 280,
      "totalEvaluations": 45000,
      "averageOverallScore": 84.56,
      "evaluatedCourseCount": 180
    },
    "scoreDistribution": [
      { "label": "<60",   "count": 2,  "percentage": 1.11 },
      { "label": "60-69", "count": 8,  "percentage": 4.44 },
      { "label": "70-79", "count": 35, "percentage": 19.44 },
      { "label": "80-89", "count": 95, "percentage": 52.78 },
      { "label": "≥90",   "count": 40, "percentage": 22.22 }
    ],
    "collegeRanking": [
      {
        "college": "计算机学院",
        "teacherCount": 25,
        "evaluatedCourseCount": 42,
        "avgScore": 88.32
      },
      {
        "college": "数学学院",
        "teacherCount": 20,
        "evaluatedCourseCount": 35,
        "avgScore": 86.50
      }
    ],
    "trend": {
      "semesters": ["2023-2024-1", "2023-2024-2", "2024-2025-1", "2024-2025-2"],
      "overallScores": [82.30, 83.10, 84.00, 84.56],
      "categoryTrends": [
        { "category": "教学态度", "scores": [85.00, 85.50, 86.00, 86.80] },
        { "category": "教学内容", "scores": [82.00, 82.80, 83.50, 84.10] },
        { "category": "教学方法", "scores": [81.50, 82.30, 83.00, 83.60] },
        { "category": "教学效果", "scores": [80.70, 81.80, 83.50, 83.74] }
      ]
    }
  }
}
```

**DashboardDTO 字段说明**

| 区块 | 字段 | 类型 | 说明 |
|------|------|------|------|
| `overview` | | `OverviewStatsDTO` | 统计卡片数据 |
| | `totalTeachers` | `Long` | 教师总数 |
| | `totalStudents` | `Long` | 学生总数 |
| | `totalCourses` | `Long` | 课程总数 |
| | `totalEvaluations` | `Long` | 评价记录总数 |
| | `averageOverallScore` | `BigDecimal` | 全校课程综合平均分 |
| | `evaluatedCourseCount` | `Long` | 有评教数据的课程数 |
| `scoreDistribution` | | `List<ScoreDistributionDTO>` | 课程评分五段分布，供柱状图 |
| | `label` | `String` | 分数段标签 |
| | `count` | `Long` | 该分数段课程数 |
| | `percentage` | `Double` | 百分比 |
| `collegeRanking` | | `List<CollegeRankItem>` | 学院排名 Top 10，按 `avgScore` 降序 |
| | `college` | `String` | 学院名称 |
| | `teacherCount` | `Long` | 教师数 |
| | `evaluatedCourseCount` | `Long` | 参评课程数 |
| | `avgScore` | `BigDecimal` | 学院平均综合分 |
| `trend` | | `TrendDTO` | 学期趋势数据，供折线图 |
| | `semesters` | `List<String>` | 学期列表（X 轴） |
| | `overallScores` | `List<BigDecimal>` | 各学期综合均分 |
| | `categoryTrends` | `List<CategoryTrend>` | 四个维度各学期均分（四条折线） |

### 6.2 概览统计

```
GET /api/dashboard/overview?semester=2025-2026-1
```

只返回 `OverviewStatsDTO`，结构参考 6.1 中 `overview` 字段。

### 6.3 评分分布

```
GET /api/dashboard/distribution?semester=2025-2026-1
```

只返回 `List<ScoreDistributionDTO>` 数组，结构参考 6.1 中 `scoreDistribution` 字段。

### 6.4 学期趋势

```
GET /api/dashboard/trend
```

只返回 `TrendDTO` 对象，无需 `semester` 参数（自动对全部学期数据计算）。

---

## 七、Excel 导出

> 导出接口直接返回 `.xlsx` 二进制文件流（`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`），浏览器自动触发下载。
> 接口返回值类型为 `void`，不走 `Result` 包装。

### 7.1 导出课程评价

```
GET /api/export/course/{courseId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `courseId` | Path | `Long` | ✅ | 课程 ID |
| `semester` | Query | `String` | ❌ | 学期过滤 |

**下载文件名**：`课程评价_数据结构.xlsx`

**Excel 列**：课程编号、课程名称、授课教师、学期、参评人数、综合得分、教学态度、教学内容、教学方法、教学效果

### 7.2 导出教师全部课程评价

```
GET /api/export/teacher/{teacherId}?semester=2025-2026-1
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `teacherId` | Path | `Long` | ✅ | 教师 ID |
| `semester` | Query | `String` | ❌ | 学期过滤 |

**下载文件名**：`教师评价_张教授.xlsx`

**Excel 列**：每行一门课，列同课程评价导出。

### 7.3 导出学院排名

```
GET /api/export/college?college=计算机学院&semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `college` | `String` | ❌ | 学院名称，不传则导出全校 |
| `semester` | `String` | ❌ | 学期过滤 |

**下载文件名**：`计算机学院_教师排名.xlsx`

**Excel 列**：工号、姓名、学院、职称、课程数、综合平均分、教学态度、教学内容、教学方法、教学效果

### 7.4 导出学期全课程评价

```
GET /api/export/semester/{semester}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `semester` | Path | `String` | ✅ | 学期，如 `2025-2026-1` |

**下载文件名**：`学期评价_2025-2026-1.xlsx`

导出指定学期下所有课程的评教汇总。

---

## 八、全局异常处理

所有异常由 `GlobalExceptionHandler` 统一拦截，保证前端始终收到一致的 JSON 响应。

| 异常类型 | 响应 code | message 示例 | 触发场景 |
|----------|-----------|-------------|---------|
| `BusinessException` | 自定义 | 按业务抛出内容 | 业务规则不满足时 |
| `MethodArgumentNotValidException` | `400` | `sourceName: 不能为空` | `@Valid` 校验失败 |
| `MaxUploadSizeExceededException` | `400` | 上传文件大小超出限制（最大10MB） | 文件超过 `spring.servlet.multipart.max-file-size` |
| `MissingServletRequestParameterException` | `400` | `缺少必填参数: importType` | 缺少 `@RequestParam(required=true)` 参数 |
| `Exception`（兜底） | `500` | 服务器内部错误 | 未预期的运行时异常 |

**常见业务异常示例**

| HTTP 状态 | 响应 code | message | 触发条件 |
|-----------|-----------|---------|---------|
| 200 | 400 | 不支持的文件格式，仅支持: .xlsx, .xls, .csv | 上传非表格文件 |
| 200 | 400 | 上传文件不能为空 | 未选择文件 |
| 200 | 400 | 不支持的导入类型: XXX | `importType` 不在枚举中 |
| 200 | 404 | 课程不存在: id=999 | 查询不存在的资源 |

---

## 附录 A：接口速查表

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 导入 | `POST` | `/api/import/upload` | 上传文件导入数据 |
| 数据源 | `GET` | `/api/datasource` | 分页查询列表 |
| 数据源 | `GET` | `/api/datasource/{id}` | 查询详情 |
| 数据源 | `POST` | `/api/datasource` | 新增 |
| 数据源 | `PUT` | `/api/datasource/{id}` | 更新 |
| 数据源 | `DELETE` | `/api/datasource/{id}` | 删除（逻辑删除） |
| 数据源 | `GET` | `/api/datasource/active` | 激活列表（下拉用） |
| 评价 | `GET` | `/api/evaluation/course/{courseId}` | 课程综合评价 |
| 评价 | `GET` | `/api/evaluation/teacher/{teacherId}` | 教师综合评价 |
| 评价 | `GET` | `/api/evaluation/college` | 学院排名 |
| 评价 | `GET` | `/api/evaluation/semester/{semester}` | 学期全系统概览 |
| 报告 | `POST` | `/api/report/generate/course/{courseId}` | 生成课程报告 |
| 报告 | `POST` | `/api/report/generate/teacher/{teacherId}` | 生成教师报告 |
| 报告 | `POST` | `/api/report/generate/college` | 生成学院/全校报告 |
| 报告 | `GET` | `/api/report` | 分页查询报告列表 |
| 报告 | `GET` | `/api/report/{id}` | 查看报告详情 |
| 报告 | `DELETE` | `/api/report/{id}` | 删除报告 |
| 仪表盘 | `GET` | `/api/dashboard` | 完整仪表盘数据 |
| 仪表盘 | `GET` | `/api/dashboard/overview` | 概览统计卡片 |
| 仪表盘 | `GET` | `/api/dashboard/distribution` | 评分分布 |
| 仪表盘 | `GET` | `/api/dashboard/trend` | 学期趋势 |
| 导出 | `GET` | `/api/export/course/{courseId}` | ⬇ 课程评价 Excel |
| 导出 | `GET` | `/api/export/teacher/{teacherId}` | ⬇ 教师评价 Excel |
| 导出 | `GET` | `/api/export/college` | ⬇ 学院排名 Excel |
| 导出 | `GET` | `/api/export/semester/{semester}` | ⬇ 学期全课程 Excel |

**共计 24 个接口**，覆盖 6 大模块。

## 附录 B：枚举值一览

### sourceType（数据源类型）

| 值 | 说明 |
|----|------|
| `EXCEL` | Excel 文件 |
| `TXT` | 文本文件 |
| `CSV` | CSV 文件 |
| `DB` | 数据库直连 |
| `API` | 外部 API |

### importType（导入类型）

| 值 | 说明 |
|----|------|
| `TEACHER` | 教师信息 |
| `STUDENT` | 学生信息 |
| `COURSE` | 课程信息 |
| `SCORE` | 评价分数 |
| `INDICATOR` | 评价指标 |

### reportType（报告类型）

| 值 | 说明 |
|----|------|
| `COURSE` | 课程评价报告 |
| `TEACHER` | 教师评价报告 |
| `COLLEGE` | 学院评价报告 |
| `SEMESTER` | 学期评价报告 |

### status（数据源状态 / 导入状态）

| 值 | 适用场景 | 说明 |
|----|---------|------|
| `ACTIVE` | 数据源 | 激活可用 |
| `INACTIVE` | 数据源 | 已停用 |
| `SUCCESS` | 导入结果 | 全部成功 |
| `PARTIAL` | 导入结果 | 部分成功 |
| `FAILED` | 导入结果 | 全部失败 |
