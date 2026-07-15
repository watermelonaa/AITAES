"""
数据导入 API 测试 (5 条用例)

接口:
  POST /api/import/upload  - 上传文件导入

测试文件位置: 项目根目录 test-data/
"""

import os

import pytest

from conftest import check_response, check_error_response

# 测试文件路径（相对于 test-api 目录）
TEST_DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "test-data")


def _file(name):
    """获取测试文件的绝对路径"""
    return os.path.join(TEST_DATA_DIR, name)


class TestImport:
    """数据导入"""

    def test_import_teacher_csv(self, session, base_url):
        """导入教师 CSV → successRows > 0"""
        path = _file("teacher.csv")
        if not os.path.exists(path):
            pytest.skip(f"文件不存在: {path}")

        with open(path, "rb") as f:
            resp = session.post(f"{base_url}/api/import/upload",
                                files={"file": ("teacher.csv", f,
                                                "text/csv")},
                                data={"importType": "TEACHER"})
        data = check_response(resp)

        assert "totalRows" in data
        assert data["successRows"] > 0, f"应成功导入, 实际 successRows={data.get('successRows')}"

    def test_import_student_csv(self, session, base_url):
        """导入学生 CSV"""
        path = _file("student.csv")
        if not os.path.exists(path):
            pytest.skip(f"文件不存在: {path}")

        with open(path, "rb") as f:
            resp = session.post(f"{base_url}/api/import/upload",
                                files={"file": ("student.csv", f,
                                                "text/csv")},
                                data={"importType": "STUDENT"})
        data = check_response(resp)

        assert data["successRows"] > 0

    def test_import_course_csv(self, session, base_url):
        """导入课程 CSV"""
        path = _file("course.csv")
        if not os.path.exists(path):
            pytest.skip(f"文件不存在: {path}")

        with open(path, "rb") as f:
            resp = session.post(f"{base_url}/api/import/upload",
                                files={"file": ("course.csv", f,
                                                "text/csv")},
                                data={"importType": "COURSE"})
        data = check_response(resp)

        assert data["successRows"] > 0

    def test_missing_file(self, session, base_url):
        """不传文件 → 400"""
        resp = session.post(f"{base_url}/api/import/upload",
                            data={"importType": "TEACHER"})
        check_error_response(resp, 400)

    def test_invalid_import_type(self, session, base_url):
        """不支持的导入类型 → 400"""
        path = _file("teacher.csv")
        if not os.path.exists(path):
            pytest.skip(f"文件不存在: {path}")

        with open(path, "rb") as f:
            resp = session.post(f"{base_url}/api/import/upload",
                                files={"file": ("teacher.csv", f,
                                                "text/csv")},
                                data={"importType": "INVALID_TYPE"})
        check_error_response(resp, 400)
