package com.itheima.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "tlias.jwt")
public class JwtProperties {

    /**
     * 签名密钥(Base64 编码字符串)
     * 要求: 解码后长度 >= 256 bit(32 字节), 不满足时应用会启动失败
     * 生成方式见仓库根目录 .env.example
     */
    private String signKey;

    /**
     * 令牌有效期(毫秒), 默认 12 小时
     */
    private Long expire = 43200000L;
}
