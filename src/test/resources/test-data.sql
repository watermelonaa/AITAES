-- ============================================================
-- AITAES 集成测试数据
-- 每次集成测试前通过 @Sql 注解加载
-- ============================================================

-- 评价指标体系（4 个一级 + 16 个二级，同生产环境）
INSERT INTO t_evaluation_indicator (indicator_no, indicator_name, category, weight, parent_id, level, description, sort_order) VALUES
('I01', '教学态度', '教学态度', 0.2500, NULL, 1, '考察教师的教学责任心', 1),
('I02', '教学内容', '教学内容', 0.2500, NULL, 1, '考察课程内容的科学性', 2),
('I03', '教学方法', '教学方法', 0.2500, NULL, 1, '考察教学手段多样性', 3),
('I04', '教学效果', '教学效果', 0.2500, NULL, 1, '考察学生学习收获', 4);

INSERT INTO t_evaluation_indicator (indicator_no, indicator_name, category, weight, parent_id, level, description, sort_order) VALUES
('I01-01', '备课充分，授课认真',     '教学态度', 0.0625, 1, 2, NULL, 1),
('I01-02', '按时上下课',             '教学态度', 0.0625, 1, 2, NULL, 2),
('I01-03', '注重课堂纪律管理',       '教学态度', 0.0625, 1, 2, NULL, 3),
('I01-04', '耐心答疑',               '教学态度', 0.0625, 1, 2, NULL, 4),
('I02-01', '内容充实，信息量适中',   '教学内容', 0.0625, 2, 2, NULL, 1),
('I02-02', '理论联系实际',           '教学内容', 0.0625, 2, 2, NULL, 2),
('I02-03', '内容前沿',               '教学内容', 0.0625, 2, 2, NULL, 3),
('I02-04', '重点突出，条理清晰',     '教学内容', 0.0625, 2, 2, NULL, 4),
('I03-01', '教学方法灵活多样',       '教学方法', 0.0625, 3, 2, NULL, 1),
('I03-02', '启发式教学',             '教学方法', 0.0625, 3, 2, NULL, 2),
('I03-03', '课堂互动充分',           '教学方法', 0.0625, 3, 2, NULL, 3),
('I03-04', '合理运用信息技术',       '教学方法', 0.0625, 3, 2, NULL, 4),
('I04-01', '知识理解与掌握程度',     '教学效果', 0.0625, 4, 2, NULL, 1),
('I04-02', '分析解决问题能力提升',   '教学效果', 0.0625, 4, 2, NULL, 2),
('I04-03', '学习兴趣与主动性激发',   '教学效果', 0.0625, 4, 2, NULL, 3),
('I04-04', '整体满意度',             '教学效果', 0.0625, 4, 2, NULL, 4);

-- 测试用户账号
INSERT INTO t_user (id, username, password, role) VALUES
(1, 'T001', '$2a$10$dummy', 'TEACHER'),
(2, 'T002', '$2a$10$dummy', 'TEACHER'),
(3, 'S2024001', '$2a$10$dummy', 'STUDENT'),
(4, 'S2024002', '$2a$10$dummy', 'STUDENT'),
(5, 'S2024003', '$2a$10$dummy', 'STUDENT');

-- 测试教师
INSERT INTO t_teacher (id, user_id, teacher_no, name, gender, college, department, title, email) VALUES
(1, 1, 'T001', '张建国', '男', '计算机学院', '软件工程系', '教授', 'zjg@university.edu.cn'),
(2, 2, 'T002', '李美玲', '女', '计算机学院', '网络工程系', '副教授', 'lml@university.edu.cn');

-- 测试学生
INSERT INTO t_student (id, user_id, student_no, name, gender, college, major, class_name, grade) VALUES
(1, 3, 'S2024001', '赵小明', '男', '计算机学院', '软件工程', '软件2101', '2024'),
(2, 4, 'S2024002', '钱小红', '女', '计算机学院', '软件工程', '软件2101', '2024'),
(3, 5, 'S2024003', '孙大伟', '男', '计算机学院', '网络工程', '网络2101', '2024');

-- 测试课程
INSERT INTO t_course (id, course_no, course_name, teacher_id, credit, course_type, semester) VALUES
(1, 'CS101', '数据结构与算法', 1, 4.0, '必修', '2025-2026-1'),
(2, 'CS201', '计算机网络',     2, 3.5, '必修', '2025-2026-1'),
(3, 'CS301', '软件工程导论',   1, 3.0, '必修', '2025-2026-1');
