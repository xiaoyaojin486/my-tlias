package com.itheima.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
//@WebFilter(urlPatterns = "/emps/*") //拦截所有请求
//@WebFilter(urlPatterns = "/*")
public class DemoFilter implements Filter {
    //初始化, Web服务器启动时执行 , 只会执行一次
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("init ....");
    }
    //拦截请求, 每次请求被处理之前执行, 每拦截到一次请求, 执行一次
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.info(" DemoFilter .... doFilter .... 放行前 ....");

        //放行
        filterChain.doFilter(servletRequest, servletResponse);

        log.info(" DemoFilter .... doFilter .... 放行后 ....");
    }
    //销毁, Web服务器关闭时执行, 只会执行一次
    @Override
    public void destroy() {
        log.info("destroy ....");
    }
}
