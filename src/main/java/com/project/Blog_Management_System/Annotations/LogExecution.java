package com.project.Blog_Management_System.Annotations;

import java.lang.annotation.*;

/**
 * Custom annotation to log the execution of methods or classes.
 * It allows you to specify whether to log method arguments and/or the result.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogExecution {
    boolean logArgs() default true;
    boolean logResult() default false;
}