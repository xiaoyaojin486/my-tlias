package com.itheima.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
//@WebFilter(urlPatterns = "/*")
public class AbcFilter implements Filter {

    //拦截请求, 每次请求被处理之前执行, 每拦截到一次请求, 执行一次
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.info(" AbcFilter .... doFilter .... 放行前 ....");

        //放行
        filterChain.doFilter(servletRequest, servletResponse);

        log.info(" AbcFilter .... doFilter .... 放行后 ....");
    }

}
