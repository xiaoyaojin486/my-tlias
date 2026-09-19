package com.itheima.filter;

import com.itheima.utils.CurrentHolder;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        //1. 获取请求路径
        String requestURI = request.getRequestURI(); // /login
        log.info("拦截到请求: {}", requestURI);

        //2. 判断是否是登录请求(包含 login, 是登录请求), 直接放行 . 记录日志
        if (requestURI.contains("login")){
            log.info("登录请求, 放行");
            filterChain.doFilter(req, resp); //放行
            return;
        }

        //3. 获取请求头中的token
        String token = request.getHeader("token");
        //4. 判断token是否存在, 如果不存在, 记录日志, 响应401 状态码
        if (token == null || token.isEmpty()){
            log.info("token不存在, 拦截请求: {}", requestURI);
            response.setStatus(401); //设置响应状态码
            return;
        }

        //5. 解析token , 解析失败, 记录日志, 响应401 状态码
        try {
            Claims claims = jwtUtils.parseJWT(token);
            log.info("解析到用户信息: {}", claims);
            Integer id = (Integer) claims.get("id");
            log.info("当前用户id: {}", id);

            //往ThreadLocal中存入当前用户id
            CurrentHolder.setCurrentId(id);
        } catch (Exception e){
            log.info("token解析失败, 令牌非法");
            response.setStatus(401); //设置响应状态码
            return;
        }

        //6. 放行
        log.info("令牌合法, 放行");
        filterChain.doFilter(req, resp);

        //7. 移除ThreadLocal中的数据
        CurrentHolder.remove();
    }
}
