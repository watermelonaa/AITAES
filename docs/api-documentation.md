# AITAES API 接口文档

> **项目**：基于AI的数智化教学分析评价系统  
> **Base URL**：`http://localhost:8080`  
> **版本**：v2.0 | 最后更新：2026-07-07

---

## 目录

- [一、通用规范](#一通用规范)
- [二、数据采集模块](#二数据采集模块)
- [三、数据源管理](#三数据源管理)
- [四、评价计算引擎](#四评价计算引擎)
- [五、报告管理](#五报告管理)
- [六、仪表盘](#六仪表盘)
- [七、Excel 导出](#七excel-导出)
- [八、错误码表](#八错误码表)

---

## 一、通用规范

### 1.1 统一响应格式

所有接口返回统一 JSON 结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 状态码，200=成功 |
| `message` | string | 提示信息 |
| `data` | object/null | 响应数据 |

### 1.2 状态码速查

| code | 含义 |
|------|------|
| 200 | 成功 |
| 206 | 部分成功（导入部分失败） |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.3 学期格式

统一使用 `YYYY-YYYY-N` 格式，如 `2025-2026-1`（2025-2026 学年第一学期）。

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
| `file` | File | ✅ | 上传文件（.xlsx / .xls / .csv） |
| `importType` | String | ✅ | TEACHER / STUDENT / COURSE / SCORE / INDICATOR |
| `sourceId` | Long | ❌ | 数据源ID，用于追踪来源 |

**Excel 列模板要求**

| importType | 表头列（第一行必须与以下中文完全匹配） |
|------------|--------------------------------------|
| TEACHER | 工号、姓名、性别、学院、系/部门、职称、邮箱、联系电话 |
| STUDENT | 学号、姓名、性别、学院、专业、班级、年级、邮箱 |
| COURSE | 课程编号、课程名称、授课教师工号、学分、课程类型、学期、课程描述 |
| SCORE | 课程编号、学号、指标编号、评分、评价意见、学期、评价时间 |
| INDICATOR | 指标编号、指标名称、指标类别、权重、父级指标编号、指标层级、指标说明、排序号 |

**成功响应** (200)

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

**部分成功响应** (206)

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

**错误响应** (400)

```json
{
  "code": 400,
  "message": "不支持的文件格式，仅支持: .xlsx, .xls, .csv"
}
```

**业务规则**：
- 每 500 行一批，批次独立提交，部分失败不影响其他批次
- 基于唯一键（工号/学号/课程编号等）自动跳过重复行
- 错误信息最多返回前 100 条
- 导入成功后自动写入 `t_data_import_log` 日志

---

## 三、数据源管理

### 3.1 分页查询数据源

```
GET /api/datasource?pageNum=1&pageSize=10&sourceType=EXCEL
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `pageNum` | int | 1 | 页码 |
| `pageSize` | int | 10 | 每页条数 |
| `sourceType` | String | — | 类型过滤（EXCEL/TXT/CSV/DB/API），不传则查全部 |

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

### 3.3 新增数据源

```
POST /api/datasource
Content-Type: application/json
```

```json
{
  "sourceName": "教师信息Excel",
  "sourceType": "EXCEL",
  "filePath": "/uploads/teachers.xlsx",
  "description": "2025秋季学期教师数据",
  "status": "ACTIVE"
}
```

**请求体字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sourceName` | String | ✅ | 数据源名称 |
| `sourceType` | String | ✅ | EXCEL / TXT / CSV / DB / API |
| `filePath` | String | ❌ | 文件路径 |
| `description` | String | ❌ | 说明 |
| `status` | String | ❌ | ACTIVE（默认）/ INACTIVE |

### 3.4 更新数据源

```
PUT /api/datasource/{id}
Content-Type: application/json
```

请求体同新增，仅更新传入的字段。

### 3.5 删除数据源

```
DELETE /api/datasource/{id}
```

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
    { "id": 1, "sourceName": "教师信息Excel", "sourceType": "EXCEL", "status": "ACTIVE" }
  ]
}
```

---

## 四、评价计算引擎

### 4.1 课程综合评价

```
GET /api/evaluation/course/{courseId}?semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `courseId` | Long | ✅ | 课程ID（路径参数） |
| `semester` | String | ❌ | 学期过滤，不传则查所有学期 |

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
      { "category": "教学态度", "weight": 0.2500, "score": 90.12, "weightedScore": 22.5300 },
      { "category": "教学内容", "weight": 0.2500, "score": 85.60, "weightedScore": 21.4000 },
      { "category": "教学方法", "weight": 0.2500, "score": 86.80, "weightedScore": 21.7000 },
      { "category": "教学效果", "weight": 0.2500, "score": 86.88, "weightedScore": 21.7200 }
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

**响应字段说明**

| 字段 | 说明 |
|------|------|
| `overallScore` | 综合得分（0-100），16 项加权分之和 |
| `categoryScores[].score` | 维度得分（0-100），归一化：加权和 ÷ 维度权重 |
| `indicatorScores[].avgScore` | 该指标所有学生的评分均值 |
| `indicatorScores[].weightedScore` | avgScore × weight |

### 4.2 教师综合评价

```
GET /api/evaluation/teacher/{teacherId}?semester=2025-2026-1
```

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
      { "category": "教学态度", "score": 91.20 },
      { "category": "教学内容", "score": 87.50 },
      { "category": "教学方法", "score": 88.30 },
      { "category": "教学效果", "score": 87.68 }
    ],
    "courseDetails": [
      { "courseId": 1, "courseName": "数据结构", "overallScore": 87.35, ... },
      { "courseId": 2, "courseName": "操作系统", "overallScore": 89.99, ... }
    ]
  }
}
```

**字段说明**

| 字段 | 说明 |
|------|------|
| `averageOverallScore` | 所有课程综合分的平均值 |
| `categoryAverages[].score` | 各课程该维度得分的平均值 |
| `courseDetails` | 每门课的完整评价明细（同课程评价响应结构） |

### 4.3 学院排名

```
GET /api/evaluation/college?college=计算机学院&semester=2025-2026-1
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `college` | String | ❌ | 学院名称，不传则全校排名 |
| `semester` | String | ❌ | 学期过滤 |

**响应**

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
      "categoryAverages": [...],
      "courseDetails": [...]
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

> 按 `averageOverallScore` 降序排列，无评教数据的教师自动过滤。

### 4.4 学期全系统概览

```
GET /api/evaluation/semester/2025-2026-1
```

返回该学期所有有评教数据的教师列表，按综合分降序。

---

## 五、报告管理

### 5.1 生成课程评价报告

```
POST /api/report/generate/course/{courseId}?semester=2025-2026-1
```

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
    "reportData": "{...完整评价JSON...}",
    "aiAnalysis": null,
    "generateTime": "2026-07-07T17:30:00"
  }
}
```

### 5.2 生成教师评价报告

```
POST /api/report/generate/teacher/{teacherId}?semester=2025-2026-1
```

### 5.3 生成学院报告

```
POST /api/report/generate/college?college=计算机学院&semester=2025-2026-1
```

> `college` 不传则生成全校报告。

### 5.4 分页查询报告列表

```
GET /api/report?pageNum=1&pageSize=10&reportType=COURSE&semester=2025-2026-1
```

| 参数 | 说明 |
|------|------|
| `reportType` | TEACHER / COURSE / COLLEGE / SEMESTER |
| `semester` | 学期过滤 |

### 5.5 查看报告详情

```
GET /api/report/{id}
```

### 5.6 删除报告

```
DELETE /api/report/{id}
```

---

## 六、仪表盘

### 6.1 仪表盘完整数据

```
GET /api/dashboard?semester=2025-2026-1
```

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
      { "college": "计算机学院", "teacherCount": 25, "evaluatedCourseCount": 42, "avgScore": 88.32 },
      { "college": "数学学院",   "teacherCount": 20, "evaluatedCourseCount": 35, "avgScore": 86.50 }
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

**数据说明**

| 字段 | 说明 |
|------|------|
| `overview` | 统计卡片：教师/学生/课程数量、评价总数、全校均分 |
| `scoreDistribution` | 课程评分五段分布，供柱状图使用 |
| `collegeRanking` | 学院排名 Top10，按 `avgScore` 降序 |
| `trend` | 各学期综合均分 + 四维度折线数据 |

### 6.2 概览统计

```
GET /api/dashboard/overview?semester=2025-2026-1
```

只返回 `overview` 部分。

### 6.3 评分分布

```
GET /api/dashboard/distribution?semester=2025-2026-1
```

只返回 `scoreDistribution` 数组。

### 6.4 学期趋势

```
GET /api/dashboard/trend
```

只返回 `trend` 对象（无需 semester 参数，自动对全量学期计算）。

---

## 七、Excel 导出

> 导出接口直接返回 `.xlsx` 文件流，浏览器自动触发下载。

### 7.1 导出课程评价

```
GET /api/export/course/{courseId}?semester=2025-2026-1
```

下载文件名：`课程评价_数据结构.xlsx`

**Excel 列**：课程编号、课程名称、授课教师、学期、参评人数、综合得分、教学态度、教学内容、教学方法、教学效果

### 7.2 导出教师全部课程评价

```
GET /api/export/teacher/{teacherId}?semester=2025-2026-1
```

下载文件名：`教师评价_张教授.xlsx`

Excel 包含该教师所有课程的评价行，每行一门课。

### 7.3 导出学院排名

```
GET /api/export/college?college=计算机学院&semester=2025-2026-1
```

下载文件名：`计算机学院_教师排名.xlsx`

**Excel 列**：工号、姓名、学院、职称、课程数、综合平均分、教学态度、教学内容、教学方法、教学效果

### 7.4 导出学期全课程评价

```
GET /api/export/semester/2025-2026-1
```

下载文件名：`学期评价_2025-2026-1.xlsx`

导出指定学期下所有课程的评教汇总。

---

## 八、错误码表

| HTTP 状态码 | code | message 示例 | 触发场景 |
|-------------|------|-------------|---------|
| 200 | 200 | 操作成功 | 正常响应 |
| 200 | 206 | 部分导入成功: 成功80行, 失败20行 | 导入含瑕疵数据 |
| 400 | 400 | 不支持的文件格式 | 上传了 .png / .doc 等 |
| 400 | 400 | 上传文件不能为空 | 未选择文件 |
| 400 | 400 | 缺少必填参数: importType | 参数缺失 |
| 400 | 400 | 上传文件大小超出限制（最大10MB） | 文件超限 |
| 400 | 400 | 不支持的导入类型: XXX | importType 不在枚举中 |
| 404 | 404 | 课程不存在: id=999 | 资源不存在 |
| 500 | 500 | 服务器内部错误 | 未预期的运行时异常 |

---

## 附录：接口速查表

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 导入 | `POST` | `/api/import/upload` | 上传文件导入 |
| 数据源 | `GET` | `/api/datasource` | 分页列表 |
| 数据源 | `GET` | `/api/datasource/{id}` | 详情 |
| 数据源 | `POST` | `/api/datasource` | 新增 |
| 数据源 | `PUT` | `/api/datasource/{id}` | 更新 |
| 数据源 | `DELETE` | `/api/datasource/{id}` | 删除 |
| 数据源 | `GET` | `/api/datasource/active` | 激活列表(下拉) |
| 评价 | `GET` | `/api/evaluation/course/{id}` | 课程评价 |
| 评价 | `GET` | `/api/evaluation/teacher/{id}` | 教师评价 |
| 评价 | `GET` | `/api/evaluation/college` | 学院排名 |
| 评价 | `GET` | `/api/evaluation/semester/{s}` | 学期概览 |
| 报告 | `POST` | `/api/report/generate/course/{id}` | 生成课程报告 |
| 报告 | `POST` | `/api/report/generate/teacher/{id}` | 生成教师报告 |
| 报告 | `POST` | `/api/report/generate/college` | 生成学院报告 |
| 报告 | `GET` | `/api/report` | 报告列表 |
| 报告 | `GET` | `/api/report/{id}` | 报告详情 |
| 报告 | `DELETE` | `/api/report/{id}` | 删除报告 |
| 仪表盘 | `GET` | `/api/dashboard` | 完整仪表盘 |
| 仪表盘 | `GET` | `/api/dashboard/overview` | 概览卡片 |
| 仪表盘 | `GET` | `/api/dashboard/distribution` | 分数分布 |
| 仪表盘 | `GET` | `/api/dashboard/trend` | 学期趋势 |
| 导出 | `GET` | `/api/export/course/{id}` | ⬇ 课程 Excel |
| 导出 | `GET` | `/api/export/teacher/{id}` | ⬇ 教师 Excel |
| 导出 | `GET` | `/api/export/college` | ⬇ 学院排名 Excel |
| 导出 | `GET` | `/api/export/semester/{s}` | ⬇ 学期 Excel |
