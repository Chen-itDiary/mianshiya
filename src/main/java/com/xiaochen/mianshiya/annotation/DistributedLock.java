package com.xiaochen.mianshiya.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 持有锁时间
     */

    long havaTime() default 30000;

    /**
     * 等待时间
     */

    long waitTime() default 10000;

    /**
     * 时间单位
     */

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
