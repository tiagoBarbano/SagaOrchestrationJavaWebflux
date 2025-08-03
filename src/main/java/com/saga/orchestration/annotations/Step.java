package com.saga.orchestration.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Step {
    String consume();
    String produce() default "";
    Class<?> consumeDTO() default Void.class;
    Class<?> produceDTO() default Void.class;
}