package com.xiaochen.mianshiya.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.xiaochen.mianshiya.annotation.CrawlerCheck;
import com.xiaochen.mianshiya.annotation.HotKeyCheck;
import com.xiaochen.mianshiya.exception.BusinessException;
import com.xiaochen.mianshiya.manager.CounterManager;
import com.xiaochen.mianshiya.model.entity.User;
import com.xiaochen.mianshiya.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class CrawlerInterceptor {
    @Resource
    private UserService userService;
    @NacosValue(value = "${warn.count:10}", autoRefreshed = true)
    private Integer warnCount;
    @NacosValue(value = "${ban.count:20}", autoRefreshed = true)
    private Integer banCount;
    @Resource
    private CounterManager counterManager;
    @Around("@annotation(crawlerCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, CrawlerCheck crawlerCheck) throws Throwable {
        String userLoginId = (String) StpUtil.getLoginId();
        // 拼接key
        String key = String.format("user:access:%s", userLoginId);
        long count = counterManager.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 180);
        if (count > banCount) {
            StpUtil.kickout(userLoginId);
            User banUser = new User();
            banUser.setId(Long.valueOf(userLoginId));
            banUser.setUserRole("Ban");
            userService.updateById(banUser);
        }
        if (count == warnCount) {
            throw new BusinessException(110, "警告：请不要频繁访问");
        }
        return joinPoint.proceed();
    }
}
