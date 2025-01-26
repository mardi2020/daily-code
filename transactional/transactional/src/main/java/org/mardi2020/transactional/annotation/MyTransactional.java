package org.mardi2020.transactional.annotation;

import org.mardi2020.transactional.config.Propagation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyTransactional {

    Propagation propagation() default Propagation.REQUIRED;

    boolean readOnly() default false;

    String name() default "";
}
