package com.project.Blog_Management_System.Logging;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogExecution {
    boolean logArgs() default true;
    boolean logResult() default false;
}