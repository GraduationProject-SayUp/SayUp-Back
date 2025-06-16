package com.sayup.SayUp.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Aspect
@Component
@Slf4j
public class Logging {
    private static final Logger logger = LoggerFactory.getLogger(Logging.class);

    // @RestController가 붙은 모든 메서드 실행 전후에 이 코드 실행
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logHttpRequests(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        // Query Parameter
        StringBuilder queryParams = new StringBuilder();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            queryParams.append(paramName).append("=").append(paramValue).append("&");
        }
        if (!queryParams.isEmpty()) {
            queryParams.deleteCharAt(queryParams.length() - 1); // 마지막 '&' 제거
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean isSuccess = true;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            isSuccess = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            if (isSuccess) {
                logger.info("[{}] {} {} | Method: {}.{}() | Execution time: {} ms | QueryParams: [{}] | Status: SUCCESS",
                        httpMethod, uri, queryParams.toString(), className, methodName, elapsedTime, queryParams);
            } else {
                logger.error("[{}] {} {} | Method: {}.{}() | Execution time: {} ms | QueryParams: [{}] | Status: FAILED | Error: {}",
                        httpMethod, uri, queryParams.toString(), className, methodName, elapsedTime, queryParams, errorMessage);
            }
        }
    }

    // @Service 클래스의 메서드 실행 시간과 예외 로깅
    @Around("within(@org.springframework.stereotype.Service *)")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean isSuccess = true;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            isSuccess = false;
            errorMessage = e.getMessage();
            logger.error("Service Exception in {}.{}() | Error: {}", className, methodName, e.getMessage());
            throw e;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            if (isSuccess) {
                logger.debug("Service Method: {}.{}() | Execution time: {} ms | Status: SUCCESS",
                        className, methodName, elapsedTime);
            } else {
                logger.error("Service Method: {}.{}() | Execution time: {} ms | Status: FAILED | Error: {}",
                        className, methodName, elapsedTime, errorMessage);
            }
        }
    }

    // Repository 메서드 실행 시간 모니터링
    @Around("within(@org.springframework.stereotype.Repository *)")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean isSuccess = true;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            isSuccess = false;
            logger.error("Repository Exception in {}.{}() | Error: {}", className, methodName, e.getMessage());
            throw e;
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            if (isSuccess) {
                logger.debug("Repository Method: {}.{}() | Execution time: {} ms | Status: SUCCESS",
                        className, methodName, elapsedTime);
            } else {
                logger.error("Repository Method: {}.{}() | Execution time: {} ms | Status: FAILED",
                        className, methodName, elapsedTime);
            }
        }
    }
}
