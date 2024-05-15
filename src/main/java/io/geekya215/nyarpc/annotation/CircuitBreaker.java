package io.geekya215.nyarpc.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    int failureThreshold() default 3;

    int successThreshold() default 2;

    long delay() default 5;

    @NotNull ChronoUnit unit() default ChronoUnit.SECONDS;
}
