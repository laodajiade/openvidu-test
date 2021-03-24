package io.openvidu.server.config;

import cn.suditech.access.domain.AccessBean;
import cn.suditech.access.rule.AccessServerRule;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AccessClientAOP {

    @Pointcut("execution(* cn.suditech.access.client.AccessClient.*(..))")
    public void feignAccessClientPointcut() {
    }

    @Around("feignAccessClientPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof AccessBean) {
                    AccessServerRule.originCache.set(((AccessBean) arg).getOrigin());
                    log.info("AccessServerAOP origin={}", AccessServerRule.originCache.get());
                }
            }
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("feignClientPointcut error,origin={}", AccessServerRule.originCache.get(), e);
            throw e;
        } finally {
            AccessServerRule.originCache.remove();
        }
    }
}
