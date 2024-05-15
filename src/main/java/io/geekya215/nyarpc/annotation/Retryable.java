package io.geekya215.nyarpc.annotation;


import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {
    @NotNull Class<? extends Throwable> retryFor() default Exception.class;

    int maxRetries() default 3;

    long delay() default 1;

    @NotNull ChronoUnit unit() default ChronoUnit.SECONDS;
}
