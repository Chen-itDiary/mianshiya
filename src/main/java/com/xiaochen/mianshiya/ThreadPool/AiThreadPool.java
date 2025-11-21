package com.xiaochen.mianshiya.ThreadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AiThreadPool {
    public static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            20,  //核心线程数
            50,             //最大线程数
            60L,            //线程空闲时间
            TimeUnit.SECONDS,   //存活时间单位
            new LinkedBlockingQueue<>(10000),  //阻塞容量
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
}
