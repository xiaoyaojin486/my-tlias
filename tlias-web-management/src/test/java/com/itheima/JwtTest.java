package com.itheima;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    /**
     * 生成JWT
     */
    @Test
    public void testGenJwt(){
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("username", "java422");
        dataMap.put("id", 10010);

        // 生成JWT
        String jwt = Jwts.builder()
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60)) //设置过期时间为 10 分钟
                .signWith(SignatureAlgorithm.HS256, "itheima".getBytes()) // 签名算法及秘钥
                .addClaims(dataMap) // 添加自定义数据
                .compact();// 返回紧凑的JWT字符串
        System.out.println(jwt);
    }

    /**
     * 解析JWT ---> 如果令牌被篡改, 解析将会报错; 令牌过期, 解析将会报错;
     */
    @Test
    public void testParser(){
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MjQzODYxNjIsImV4cCI6MTcyNDM4NjIyMiwiaWQiOjEwMDEwLCJ1c2VybmFtZSI6ImphdmE0MjIifQ._2j_PZiCYdHrGpsaPKo1k7tCz3zAxOhHe7CzYhwC3Cc";
        Map<String, Object> map = Jwts.parser()
                .setSigningKey("itheima".getBytes()) // 设置签名秘钥 - 与生成JWT时使用的秘钥一致
                .parseClaimsJws(jwt) // 解析JWT
                .getBody(); // 获取JWT中的数据(第二部分)
        System.out.println(map);
    }

}
