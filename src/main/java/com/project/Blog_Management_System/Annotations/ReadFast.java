package com.project.Blog_Management_System.Annotations;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to indicate that a method should be executed with specific query hints for faster read operations.
 * This annotation applies a timeout of 3000 milliseconds and marks the query as read-only for Hibernate.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@QueryHints(value = {
        @QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
})
public @interface ReadFast {
}