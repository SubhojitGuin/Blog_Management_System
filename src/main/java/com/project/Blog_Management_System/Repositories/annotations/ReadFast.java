package com.project.Blog_Management_System.Repositories.annotations;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@QueryHints(value = {
        @QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
})
public @interface ReadFast {
}