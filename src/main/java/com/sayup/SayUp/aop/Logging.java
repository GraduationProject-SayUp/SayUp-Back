package com.sayup.SayUp.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
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
        Object result = joinPoint.proceed();
        long elapsedTime = System.currentTimeMillis() - startTime;

        logger.info("[{}] {} {} | Method: {}.{}() | Execution time: {} ms | QueryParams: [{}]",
                httpMethod, uri, queryParams.toString(), className, methodName, elapsedTime, queryParams);

        return result;
    }

    // @RestController 또는 @Service 가 붙은 클래스만 AOP 적용
    @Around("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Service *)")
    // 예외 발생 시 로그 출력
    public Object logExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();  // 원래 메서드 실행
        } catch (Exception e) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();
            String className = signature.getDeclaringType().getSimpleName();

            logger.error("Exception in {}.{}() | Error: {}", className, methodName, e.getMessage());

            throw e;  // 예외를 다시 던져서 spring이 처리하게 함
        }
    }
}
