package com.itheima.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密相关的配置
 */
@Configuration
public class PasswordConfig {

    /**
     * 密码编码器 —— 采用 BCrypt 算法
     * <p>
     * 为什么是 BCrypt:
     * 1. 单向: 只能加密, 不能解密(库里的密文即使泄漏, 也推不出原始密码)
     * 2. 自带随机盐: 每次 encode 同一个密码得到的密文都不同, 盐就存在密文里,
     *    所以无法用彩虹表/预计算哈希来批量破解
     * 3. 故意慢: 内置代价因子(cost, 默认 10, 即 2^10 轮), 单次校验约几十毫秒。
     *    对正常登录无感, 但让离线暴力破解的成本提高几个数量级
     * <p>
     * 密文固定 60 个字符, 格式 $2a$10$<22位盐><31位哈希>,
     * 所以数据库 password 列必须 >= 60 —— 见 sql/migrate-p0-4-password-bcrypt.sql
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
