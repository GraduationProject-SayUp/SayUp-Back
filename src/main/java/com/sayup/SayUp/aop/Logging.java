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

    @Around("@within(org.springframework.web.bind.annotation.RestController)")  // @RestController가 붙은 모든 클래스의 메서드가 실행될 때마다
    public Object logHttpRequests(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();

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
}

