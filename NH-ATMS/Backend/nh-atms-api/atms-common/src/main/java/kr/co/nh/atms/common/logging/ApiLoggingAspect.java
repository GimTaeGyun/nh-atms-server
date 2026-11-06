package kr.co.nh.atms.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Around("execution(* kr.co.nh.atms..controller..*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String method = request != null ? request.getMethod() : "";
        String uri = request != null ? request.getRequestURI() : "";
        String userId = (request != null && request.getUserPrincipal() != null)
                ? request.getUserPrincipal().getName() : "anonymous";

        long start = System.currentTimeMillis();
        log.info("[API 요청] {} {} | user={} | method={}", method, uri, userId, joinPoint.getSignature().getName());

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[API 응답] {} {} | {}ms | 성공", method, uri, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[API 오류] {} {} | {}ms | {}", method, uri, elapsed, e.getMessage());
            throw e;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
