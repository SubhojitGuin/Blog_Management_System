package com.project.Blog_Management_System.Annotations.uuidV7;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Custom annotation to generate UUIDv7 for entity fields.
 * This annotation uses the UuidV7Generator to produce time-ordered UUIDs.
 */
@IdGeneratorType(UuidV7Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD})
public @interface GeneratedUuidV7 {
}
