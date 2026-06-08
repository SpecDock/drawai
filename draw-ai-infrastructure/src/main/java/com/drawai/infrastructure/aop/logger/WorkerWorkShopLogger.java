package com.drawai.infrastructure.aop.logger;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author specdock
 * @Date 2026/6/7
 * @Time 16:49
 */
@Aspect
@Component
@Slf4j
public class WorkerWorkShopLogger {

    /**
     * 定义切入点表达式，精确匹配 com.drawai.domain.aiengine.service.process.workshop.WorkerWorkShop 类中的 workStream 方法
     */
    @Pointcut("execution(* com.drawai.domain.aiengine.service.process.workshop.WorkerWorkShop.workStream(..))")
    public void workerWorkShopPointcut() {
    }

    /**
     * 环绕通知处理逻辑，负责在目标方法执行前后织入日志流控制
     *
     * @param joinPoint 切入点连接点抽象，用于驱动并拦截目标方法的反射执行
     * @return 目标方法执行后的返回值
     * @throws Throwable 目标方法执行过程中抛出的任意受检或非受检异常
     */
    @Around("workerWorkShopPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 1. 织入前置横切关注点：打印启动标识
        log.info("\n[AOP-Logger] {}#{} processing: start", className, methodName);

        try {
            // 2. 驱动代理链，执行目标领域服务方法
            Object result = joinPoint.proceed();

            return result;
        } catch (Throwable throwable) {
            // 4. 异常边界处理：确保在领域服务抛出异常时，技术组件仍能正确输出异常日志并向上抛出
            log.error("[AOP-Logger] {}#{} processing failed with exception: {}", className, methodName, throwable.getMessage());
            throw throwable;
        }
    }
}