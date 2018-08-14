package com.cfs.mini.common.extension;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * default extension name
     *
     * 默认拓展名
     */
    String value() default "";

}