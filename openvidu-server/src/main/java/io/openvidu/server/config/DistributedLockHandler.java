package io.openvidu.server.config;

import io.openvidu.server.annotation.DistributedLock;
import io.openvidu.server.common.constants.CacheKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @program: prepaid-platform
 * @description:
 * @author: WuBing
 * @create: 2021-07-12 11:13
 **/

@Aspect
@Component
@Slf4j
public class DistributedLockHandler {

    @Resource
    protected RedissonClient redissonClient;

    @Pointcut("@annotation(io.openvidu.server.annotation.DistributedLock) ")
    public void tryRunDistributedLock() {

    }

    @Around("tryRunDistributedLock()")
    public void checkAndLock(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        //获取锁key值
        String key = annotation.key();
        RLock lock = redissonClient.getLock(CacheKeyConstants.REDISSON_LOCK_PREFIX_KEY + key);
        try {
            if (lock.tryLock()) {
                log.debug("通过redisson 执行方法{},锁key值{}", ((MethodSignature) joinPoint.getSignature()).getMethod().getName(), key);
                try {
                    joinPoint.proceed();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }


    }


}
