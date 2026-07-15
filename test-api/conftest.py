"""
AITAES API 自动化测试公共配置

启动后端后运行: cd test-api && pip install pytest requests && pytest -v
"""

import pytest
import requests
import os

# 从环境变量读取，默认本地
BASE_URL = os.environ.get("AITAES_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def base_url():
    """API 基础地址"""
    return BASE_URL


@pytest.fixture(scope="session")
def session():
    """复用 HTTP 连接"""
    s = requests.Session()
    yield s
    s.close()


def check_response(resp, expected_code=200):
    """统一的响应断言"""
    assert resp.status_code == 200, f"HTTP {resp.status_code}: {resp.text[:200]}"
    data = resp.json()
    assert data["code"] == expected_code, f"code={data['code']}, msg={data.get('message')}"
    return data["data"]


def check_error_response(resp, expected_code):
    """统一的错误响应断言"""
    assert resp.status_code == 200, f"HTTP {resp.status_code}: {resp.text[:200]}"
    data = resp.json()
    assert data["code"] == expected_code, f"期望 code={expected_code}，实际 code={data['code']}"
    return data
