-- ============================================================================
-- P0-4 密码明文存储 -> BCrypt 加密存储  迁移脚本
--
-- 执行环境: MySQL 8.0, 数据库 tlias
-- 执行方式: mysql -uroot -p tlias < sql/migrate-p0-4-password-bcrypt.sql
-- 可重复执行, 第二次运行不会产生任何变化
-- 执行前请先备份: mysqldump -uroot -p tlias > tlias-backup.sql
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 1. 加宽 password 列, 并去掉明文默认值
--
-- BCrypt 密文固定 60 个字符, 原来的 varchar(50) 装不下。
-- 不改列宽直接写入会失败:
--     ERROR 1406 (22001): Data too long for column 'password' at row 1
--
-- 同时把 DEFAULT '123456' 去掉并设为 NOT NULL:
-- 默认值意味着"只要忘了设密码, 数据库就悄悄给你一个 123456 的明文",
-- 这种"沉默的兜底"正是密码问题的温床。现在新增员工时如果没设密码, 会直接报错,
-- 强制代码里显式调用 passwordEncoder.encode() 加密后再插入。
-- 列宽留到 100 而不是刚好 60, 是为了将来换算法(如 Argon2 密文更长)时不用再改表。
-- ----------------------------------------------------------------------------
ALTER TABLE emp
    MODIFY COLUMN password VARCHAR(100) NOT NULL COMMENT '密码, BCrypt 密文';


-- ----------------------------------------------------------------------------
-- 2. 把存量明文密码替换成对应的 BCrypt 密文
--
-- 密文由 BCryptPasswordEncoder(默认 cost=10) 生成, 已逐一验证 matches() 返回 true。
-- 线上有 3 种不同的明文密码, 所以要分 3 条更新; 不能无脑把所有行刷成同一个哈希,
-- 否则密码本来是 1234567 / 12345678 的账号会被改掉密码, 直接登不上去。
--
-- 注意: BCrypt 的盐是随机的, 所以同一个明文每次加密得到的密文都不同。
-- 这里对同一个明文只生成一个密文、所有同密码的账号共用, 是可以接受的 ——
-- 因为盐已经存在于密文中, 破解单个密文仍需要独立付出代价。
-- ----------------------------------------------------------------------------
UPDATE emp SET password = '$2a$10$4Gf9H5hkfr2rGnooSdA8F.r/Ys7YisOG1DMrzZYID8KjB9bilC9Ji' WHERE password = '123456';
UPDATE emp SET password = '$2a$10$NC0ZsOvCSuLQloA0UqN.xuwUiafmz9RFE.MzkRxbiRgFqqPh9tYKK' WHERE password = '1234567';
UPDATE emp SET password = '$2a$10$TEgDp1ZoTtMdPwL1YNlN2emerCFbA5QAVve0WWln6W9/aiU220Df2' WHERE password = '12345678';


-- ----------------------------------------------------------------------------
-- 3. 校验: 这条查询必须返回 0 行
--
-- 只要有结果, 就说明库中还存在不是 BCrypt 格式的密码(明文, 或脚本没覆盖到的其它明文),
-- 这些账号登录时会失败, 需要按第 2 步的方式补一条 UPDATE。
-- ----------------------------------------------------------------------------
SELECT id, username, password
FROM emp
WHERE password NOT LIKE '$2a$%';
