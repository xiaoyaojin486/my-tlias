package com.itheima.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    /**
     * HS256 要求的最小密钥长度: 256 bit = 32 字节
     */
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey signKey;
    private final Long expire;

    public JwtUtils(JwtProperties jwtProperties) {
        this.signKey = buildSignKey(jwtProperties.getSignKey());
        this.expire = jwtProperties.getExpire();
    }

    /**
     * 把配置里的 Base64 密钥解码成 HmacSHA256 用的 Key, 并校验长度
     * 密钥不足 256 bit 直接让应用启动失败 —— 弱密钥可以被离线暴力破解,
     * 一旦破解出来就能伪造任意用户的令牌, 这种问题不该留到线上才暴露
     */
    private static SecretKey buildSignKey(String base64SignKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64SignKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT 签名密钥不是合法的 Base64 字符串, 生成方式见仓库根目录 .env.example", e);
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(String.format(
                    "JWT 签名密钥长度不足: HS256 要求至少 %d 字节(%d bit), 当前只有 %d 字节(%d bit), 生成方式见仓库根目录 .env.example",
                    MIN_KEY_BYTES, MIN_KEY_BYTES * 8, keyBytes.length, keyBytes.length * 8));
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * 生成JWT令牌
     * @return
     */
    public String generateJwt(Map<String,Object> claims){
        String jwt = Jwts.builder()
                .addClaims(claims) //添加自定义属性
                .signWith(SignatureAlgorithm.HS256, signKey)
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .compact();
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public Claims parseJWT(String jwt){
        Claims claims = Jwts.parser()
                .setSigningKey(signKey)
                .parseClaimsJws(jwt)
                .getBody();
        return claims;
    }
}
