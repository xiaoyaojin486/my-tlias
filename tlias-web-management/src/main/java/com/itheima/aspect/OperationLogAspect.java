package com.itheima.aspect;

import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {
    @Autowired
    private OperateLogMapper operateLogMapper;

    // 环绕通知
    @Around("@annotation(com.itheima.anno.LogOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 执行目标方法
        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;

        // 构建日志对象
        OperateLog log = new OperateLog();
        log.setOperateEmpId(CurrentHolder.getCurrentId()); // 假设有一个方法可以获取当前用户ID
        log.setOperateTime(LocalDateTime.now());
        log.setClassName(joinPoint.getTarget().getClass().getName()); // 获取类名
        log.setMethodName(joinPoint.getSignature().getName()); // 获取方法名

        Object[] args = joinPoint.getArgs(); // 获取方法运行时传入的参数
        log.setMethodParams(Arrays.toString(args));
        log.setReturnValue(result.toString());
        log.setCostTime(costTime);

        // 保存日志
        operateLogMapper.insert(log);
        return result;
    }
}
