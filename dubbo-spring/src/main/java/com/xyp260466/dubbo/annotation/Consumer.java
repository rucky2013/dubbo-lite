package com.xyp260466.dubbo.annotation;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by xyp on 16-5-9.
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
@Documented
public @interface Consumer {
    String value() default "";
}
