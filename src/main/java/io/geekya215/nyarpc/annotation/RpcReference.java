package io.geekya215.nyarpc.annotation;

import io.geekya215.nyarpc.serializer.SerializerType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    byte serializer() default SerializerType.JDK;
}
