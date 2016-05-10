package com.xyp260466.dubbo.annotation;

import java.lang.annotation.*;

/**
 * Created by xyp on 16-5-10.
 *
 * 用于指定DUBBO接口类
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Interface {
    String value() default "";
}
