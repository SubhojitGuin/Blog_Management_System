package com.project.Blog_Management_System.Logging;

import com.project.Blog_Management_System.Annotations.LogExecution;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final String MDC_OPERATION = "operation";

    @Around("@annotation(com.project.Blog_Management_System.Annotations.LogExecution) || " +
            "@within(com.project.Blog_Management_System.Annotations.LogExecution)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        LogExecution annotation = AnnotationUtils.findAnnotation(method, LogExecution.class);

        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), LogExecution.class);
        }

        if (annotation == null) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        String operation = className + "." + methodName;

        MDC.put(MDC_OPERATION, operation);

        if (annotation.logArgs()) {
            String args = resolveArgs(signature.getParameterNames(), joinPoint.getArgs());
            log.info("Method called operation={} args={}", operation, args);
        } else {
            log.info("Method called operation={}", operation);
        }

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            if (annotation.logResult() && result != null) {
                log.info("Method succeeded operation={} duration={} ms result={}",
                        operation, duration, result);
            } else {
                log.info("Method succeeded operation={} duration={} ms", operation, duration);
            }

            return result;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("Method failed operation={} duration={} ms error={}",
                    operation, duration, ex.getMessage(), ex);
            throw ex;
        } finally {
            MDC.remove(MDC_OPERATION);
        }
    }

    private String resolveArgs(String[] paramNames, Object[] paramValues) {
        if (paramNames == null || paramNames.length == 0) return "none";

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            if (isSensitive(name)) {
                joiner.add(name + "=***");
            } else {
                joiner.add(name + "=" + paramValues[i]);
            }
        }
        return joiner.toString();
    }

    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("credential");
    }
}
