package com.sdt.web_app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark methods for automated AOP Audit Logging
 * in compliance with RA 10173 (Data Privacy Act of 2012).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /**
     * Action performed (e.g., 'CREATE', 'READ', 'UPDATE', 'DELETE', 'EXPORT', 'LOGIN', 'PASSWORD_RESET')
     */
    String action();

    /**
     * Target entity name (e.g., 'User', 'Student', 'Grade', 'FeeMatrix')
     */
    String entityName() default "";

    /**
     * SpEL expression to dynamically resolve entity ID (e.g. "#id", "#request.username", "#result.id")
     */
    String entityId() default "";

    /**
     * Whether to capture method argument values in the log payload
     */
    boolean includeArgs() default true;
}
