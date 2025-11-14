package com.xiaochen.mianshiya.aop;

import com.xiaochen.mianshiya.annotation.AuthCheck;
import com.xiaochen.mianshiya.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Aspect
@Component
public class DistributedInterceptor {
    ReentrantLock lock = new ReentrantLock();

    @Around("@annotation(distributedLock)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Exception {
        long haveTime = distributedLock.havaTime();
        long waitTime = distributedLock.waitTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, timeUnit);
            if (acquired) {
                return joinPoint.proceed();
            } else {
                throw new RuntimeException("获取锁失败");
            }
        } catch (Throwable e) {
            throw new Exception(e);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
