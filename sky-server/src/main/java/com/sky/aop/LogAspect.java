package com.sky.aop;

import com.alibaba.fastjson.JSONObject;
import com.sky.entity.OperateLog;
import com.sky.mapper.OperateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;

import static com.sky.context.BaseContext.getCurrentId;

@Slf4j
@Component
@Aspect //AOP类
public class LogAspect {
    @Autowired
    private OperateLogMapper operateLogMapper;

    @Pointcut("@within(com.sky.annotation.Log) || @annotation(com.sky.annotation.Log)")
    public void log_pt() {
    }

    @Around("log_pt()") //切入点表达式
    public Object recordTime(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("开始记录日志···");
        operateLogMapper.create_table_operate_log();

        //获取请求头中的jwt令牌
        String operateUser = String.valueOf(getCurrentId()); // 操作人ID - 当前登录员工ID
        log.info("当前操作员工ID: {}", operateUser);

        LocalDateTime operateTime = LocalDateTime.now();
        log.info("当前操作时间: {}", operateTime);

        String className = joinPoint.getTarget().getClass().getName();
        log.info("当前操作类名: {}", className);

        String methodName = joinPoint.getSignature().getName();
        log.info("当前操作方法名: {}", methodName);

        String methodParams = Arrays.toString(joinPoint.getArgs());
        log.info("当前操作方法参数: {}", methodParams);

        long begin = System.currentTimeMillis();//记录开始时间
        Object result = joinPoint.proceed();//调用原始目标方法运行
        long end = System.currentTimeMillis();//记录结束时间

        String returnValue = JSONObject.toJSONString(result);//方法返回值，转为json字符串存入数据库

        Long costTime = end - begin;
        log.info(joinPoint.getSignature()+"方法执行耗时: {}ms", costTime);//计算方法执行耗时

        //记录操作日志
        OperateLog operateLog = new OperateLog(null,operateUser,operateTime,className,methodName,methodParams,returnValue,costTime);
        operateLogMapper.insert(operateLog);
        log.info("AOP记录操作日志: {}" , operateLog);


        return result;
    }
}