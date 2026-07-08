#!/bin/bash
# ============================================================
# AITAES API 功能测试脚本
# 用法：bash test-api.sh
# 前提：应用已启动在 localhost:8080，数据库已初始化
# ============================================================

BASE="http://localhost:8080"
PASS=0
FAIL=0

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

check() {
    local desc="$1"
    local resp="$2"
    local code
    code=$(echo "$resp" | grep -oP '"code"\s*:\s*\K\d+' | head -1)
    if [ "$code" = "200" ]; then
        echo -e "${GREEN}[PASS]${NC} $desc"
        ((PASS++))
    else
        echo -e "${RED}[FAIL]${NC} $desc (code=$code)"
        echo "  response: $(echo "$resp" | head -c 200)"
        ((FAIL++))
    fi
}

echo "============================================"
echo "  AITAES API 功能测试"
echo "============================================"
echo ""

# ========================================
# 1. 数据源管理
# ========================================
echo -e "${YELLOW}--- 1. 数据源管理 ---${NC}"

# 创建数据源
resp=$(curl -s -X POST "$BASE/api/datasource" \
  -H "Content-Type: application/json" \
  -d '{"sourceName":"测试数据源","sourceType":"EXCEL","description":"用于功能测试","status":"ACTIVE"}')
check "创建数据源" "$resp"

DATASOURCE_ID=$(echo "$resp" | grep -oP '"data"\s*:\s*\{[^}]*"id"\s*:\s*\K\d+' | head -1)
echo "  数据源ID = $DATASOURCE_ID"

# 查询数据源列表
resp=$(curl -s "$BASE/api/datasource?pageNum=1&pageSize=10")
check "查询数据源列表" "$resp"

# 查询激活的数据源
resp=$(curl -s "$BASE/api/datasource/active")
check "查询激活的数据源" "$resp"

echo ""

# ========================================
# 2. 数据导入（按依赖顺序）
# ========================================
echo -e "${YELLOW}--- 2. 数据导入 ---${NC}"

# 2.1 导入教师
resp=$(curl -s -X POST "$BASE/api/import/upload" \
  -F "file=@teacher.csv" \
  -F "importType=TEACHER")
check "导入教师数据" "$resp"

# 2.2 导入学生
resp=$(curl -s -X POST "$BASE/api/import/upload" \
  -F "file=@student.csv" \
  -F "importType=STUDENT")
check "导入学生数据" "$resp"

# 2.3 导入课程
resp=$(curl -s -X POST "$BASE/api/import/upload" \
  -F "file=@course.csv" \
  -F "importType=COURSE")
check "导入课程数据" "$resp"

# 2.4 导入评价分数
resp=$(curl -s -X POST "$BASE/api/import/upload" \
  -F "file=@score.csv" \
  -F "importType=SCORE")
check "导入评价分数" "$resp"

echo ""

# ========================================
# 3. 评价计算
# ========================================
echo -e "${YELLOW}--- 3. 评价计算 ---${NC}"

# 课程评价
resp=$(curl -s "$BASE/api/evaluation/course/1?semester=2025-2026-1")
check "课程评价(课程1)" "$resp"

resp=$(curl -s "$BASE/api/evaluation/course/2?semester=2025-2026-1")
check "课程评价(课程2)" "$resp"

# 教师评价
resp=$(curl -s "$BASE/api/evaluation/teacher/1?semester=2025-2026-1")
check "教师评价(教师1-张建国)" "$resp"

# 学院排名
resp=$(curl -s "$BASE/api/evaluation/college?college=计算机学院&semester=2025-2026-1")
check "学院排名(计算机学院)" "$resp"

# 学期概览
resp=$(curl -s "$BASE/api/evaluation/semester/2025-2026-1")
check "学期概览" "$resp"

echo ""

# ========================================
# 4. 仪表盘
# ========================================
echo -e "${YELLOW}--- 4. 仪表盘 ---${NC}"

resp=$(curl -s "$BASE/api/dashboard?semester=2025-2026-1")
check "仪表盘完整数据" "$resp"

resp=$(curl -s "$BASE/api/dashboard/overview?semester=2025-2026-1")
check "概览统计卡片" "$resp"

resp=$(curl -s "$BASE/api/dashboard/distribution?semester=2025-2026-1")
check "评分分布" "$resp"

resp=$(curl -s "$BASE/api/dashboard/trend")
check "学期趋势" "$resp"

echo ""

# ========================================
# 5. 报告生成
# ========================================
echo -e "${YELLOW}--- 5. 报告生成 ---${NC}"

resp=$(curl -s -X POST "$BASE/api/report/generate/course/1?semester=2025-2026-1")
check "生成课程报告(课程1)" "$resp"

resp=$(curl -s -X POST "$BASE/api/report/generate/teacher/1?semester=2025-2026-1")
check "生成教师报告(教师1)" "$resp"

resp=$(curl -s -X POST "$BASE/api/report/generate/college?college=计算机学院&semester=2025-2026-1")
check "生成学院报告" "$resp"

# 查询报告列表
resp=$(curl -s "$BASE/api/report?pageNum=1&pageSize=10")
check "查询报告列表" "$resp"

REPORT_ID=$(echo "$resp" | grep -oP '"id"\s*:\s*\K\d+' | head -1)

# 查看报告详情
if [ -n "$REPORT_ID" ]; then
    resp=$(curl -s "$BASE/api/report/$REPORT_ID")
    check "查看报告详情(ID=$REPORT_ID)" "$resp"
fi

echo ""

# ========================================
# 6. 导出
# ========================================
echo -e "${YELLOW}--- 6. 导出 ---${NC}"

# 导出课程评价（返回文件流，检查 Content-Type）
http_code=$(curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/api/export/course/1?semester=2025-2026-1")
if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}[PASS]${NC} 导出课程评价 (HTTP $http_code)"
    ((PASS++))
else
    echo -e "${RED}[FAIL]${NC} 导出课程评价 (HTTP $http_code)"
    ((FAIL++))
fi

# 导出教师评价
http_code=$(curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/api/export/teacher/1?semester=2025-2026-1")
if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}[PASS]${NC} 导出教师评价 (HTTP $http_code)"
    ((PASS++))
else
    echo -e "${RED}[FAIL]${NC} 导出教师评价 (HTTP $http_code)"
    ((FAIL++))
fi

# 导出学院排名
http_code=$(curl -s -o /dev/null -w "%{http_code}" \
  "$BASE/api/export/college?semester=2025-2026-1")
if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}[PASS]${NC} 导出学院排名 (HTTP $http_code)"
    ((PASS++))
else
    echo -e "${RED}[FAIL]${NC} 导出学院排名 (HTTP $http_code)"
    ((FAIL++))
fi

echo ""

# ========================================
# 7. 异常场景测试
# ========================================
echo -e "${YELLOW}--- 7. 异常场景 ---${NC}"

# 不存在的课程
resp=$(curl -s "$BASE/api/evaluation/course/999?semester=2025-2026-1")
check "查询不存在的课程(应返回错误)" "$resp"

# 缺少必填字段
resp=$(curl -s -X POST "$BASE/api/datasource" \
  -H "Content-Type: application/json" \
  -d '{"sourceName":""}')
check "创建数据源-名称为空(应返回错误)" "$resp"

echo ""
echo "============================================"
echo -e "  测试结果: ${GREEN}通过 $PASS${NC} / ${RED}失败 $FAIL${NC}"
echo "============================================"
