package com.project.Blog_Management_System.Annotations;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@WithUserDetails(setupBefore = TestExecutionEvent.TEST_EXECUTION)
public @interface WithMockBlogUser {

    /**
     * Maps your dynamic value straight into Spring Security's username property.
     */
    @AliasFor(annotation = WithUserDetails.class, attribute = "value")
    String value() default "testuser";
}
