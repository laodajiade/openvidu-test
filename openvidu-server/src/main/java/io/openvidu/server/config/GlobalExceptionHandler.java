package io.openvidu.server.config;

import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.kurento.jsonrpc.message.Request;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author geedow
 * @date 2019/10/25 18:17
 */

@Slf4j
@Aspect
@Component
public class GlobalExceptionHandler {

    @Resource
    RpcNotificationService notificationService;

    @Pointcut("execution(* io.openvidu.server.rpc.RpcHandler*.*(..))")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object handlerException(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable ex) {
            log.error("Exception:\n", ex);
//            Object target = proceedingJoinPoint.getTarget();
            String privateId = ((Request) ((MethodInvocationProceedingJoinPoint) proceedingJoinPoint).getArgs()[1]).getSessionId();
            if (notificationService.getRpcConnection(privateId) != null) {
                notificationService.sendErrorResponseWithDesc(privateId,
                        ((Request) ((MethodInvocationProceedingJoinPoint) proceedingJoinPoint).getArgs()[1]).getId(),
                        null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
            }
            /*((RpcHandler) target).getNotificationService().sendErrorResponseWithDesc(
                    ((Request) ((MethodInvocationProceedingJoinPoint) proceedingJoinPoint).getArgs()[1]).getSessionId(),
                    ((Request) ((MethodInvocationProceedingJoinPoint) proceedingJoinPoint).getArgs()[1]).getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);*/
        }
        return null;
    }
}
