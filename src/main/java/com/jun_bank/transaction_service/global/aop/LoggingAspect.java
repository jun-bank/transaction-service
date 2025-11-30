package com.jun_bank.transaction_service.global.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 요청/응답 로깅 AOP
 * - Controller: INFO 레벨 로깅
 * - Service: DEBUG 레벨 로깅
 * - 실행 시간 측정
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Controller 패키지 내 모든 public 메서드
     */
    @Pointcut("execution(* com.jun_bank.transaction_service.domain..presentation..*Controller.*(..))")
    public void controllerPointcut() {}

    /**
     * Service 패키지 내 모든 public 메서드
     */
    @Pointcut("execution(* com.jun_bank.transaction_service.domain..application.service..*Service.*(..))")
    public void servicePointcut() {}

    /**
     * Controller 메서드 로깅
     */
    @Around("controllerPointcut()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("[Controller] {}.{} 호출 시작", className, methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.info("[Controller] {}.{} 완료 - 소요시간: {}ms",
                    className, methodName, stopWatch.getTotalTimeMillis());

            return result;
        } catch (Exception e) {
            stopWatch.stop();
            log.error("[Controller] {}.{} 실패 - 소요시간: {}ms, 에러: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage());
            throw e;
        }
    }

    /**
     * Service 메서드 로깅 (DEBUG 레벨)
     */
    @Around("servicePointcut()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("[Service] {}.{} 호출 시작", className, methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.debug("[Service] {}.{} 완료 - 소요시간: {}ms",
                    className, methodName, stopWatch.getTotalTimeMillis());

            return result;
        } catch (Exception e) {
            stopWatch.stop();
            log.warn("[Service] {}.{} 실패 - 소요시간: {}ms, 에러: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(), e.getMessage());
            throw e;
        }
    }
}