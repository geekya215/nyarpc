package io.geekya215.nyarpc.annotation;

import io.geekya215.nyarpc.protocal.Compress;
import io.geekya215.nyarpc.protocal.Serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    Serialization serializer() default Serialization.JDK;

    Compress compress() default Compress.NO_COMPRESS;

    int timeout() default 3;
}
