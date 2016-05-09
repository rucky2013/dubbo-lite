package com.xyp260466.dubbo.annotation;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/**
 * Created by xyp on 16-5-9.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Provider {
    String value() default "";
}
