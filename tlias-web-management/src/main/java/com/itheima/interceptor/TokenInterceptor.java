package com.itheima.interceptor;

import com.itheima.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取请求路径
        String requestURI = request.getRequestURI(); // /login
        log.info("拦截到请求: {}", requestURI);

//        //2. 判断是否是登录请求(包含 login, 是登录请求), 直接放行 . 记录日志
//        if (requestURI.contains("login")){
//            log.info("登录请求, 放行");
//            return true;
//        }

        //3. 获取请求头中的token
        String token = request.getHeader("token");
        //4. 判断token是否存在, 如果不存在, 记录日志, 响应401 状态码
        if (token == null || token.isEmpty()){
            log.info("token不存在, 拦截请求: {}", requestURI);
            response.setStatus(401); //设置响应状态码
            return false;
        }

        //5. 解析token , 解析失败, 记录日志, 响应401 状态码
        try {
            JwtUtils.parseJWT(token);
        } catch (Exception e){
            log.info("token解析失败, 令牌非法");
            response.setStatus(401); //设置响应状态码
            return false;
        }

        //6. 放行
        log.info("令牌合法, 放行");
        return true;
    }
}
