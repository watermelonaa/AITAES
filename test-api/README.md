# AITAES API 自动化测试

基于 **pytest + requests** 的接口自动化回归测试。

## 快速开始

```bash
# 1. 创建虚拟环境（只需一次）
python -m venv venv

# 2. 激活虚拟环境并安装依赖（只需一次）
venv\Scripts\activate        # Windows
# source venv/bin/activate   # macOS / Linux
pip install pytest requests

# 3. 确保后端已启动（Spring Boot 监听 localhost:8080）
cd ..
./mvnw spring-boot:run

# 4. 回到 test-api 目录运行测试
cd test-api
pytest -v

# 5. 指定后端地址
AITAES_BASE_URL=http://192.168.1.100:8080 pytest -v

# 6. 只跑某个模块
pytest test_import.py -v

# 7. 用完退出虚拟环境
deactivate
```

## 文件说明

```
test-api/
├── conftest.py          # 公共 fixtures: base_url, session, 断言工具
├── pytest.ini           # pytest 配置
├── test_datasource.py   # 数据源 CRUD (6 tests)
├── test_import.py       # 数据导入 (5 tests)
└── README.md
```

## 前提条件

- 后端已启动
- MySQL 数据库已配置（可先通过 `POST /api/import/upload` 导入 test-data/ 下的 CSV 文件）

## 测试用例统计

| 模块 | 用例数 | 说明 |
|------|:---:|------|
| DataSource | 6 | 完整 CRUD 流程 |
| Import | 5 | 三种导入 + 异常 |
| **合计** | **11** | |
