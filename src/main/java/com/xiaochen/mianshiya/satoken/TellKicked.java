package com.xiaochen.mianshiya.satoken;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaochen.mianshiya.DeviceUtils.DeviceUtils;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.constant.RedisConstant;
import com.xiaochen.mianshiya.exception.BusinessException;
import com.xiaochen.mianshiya.model.entity.User;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.xiaochen.mianshiya.constant.RedisConstant.conflictedKey;


@Component
public class TellKicked {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private HttpServletRequest request;

    public void isConflicted() {
        String tokenValue = StpUtil.getTokenValue();
        Object userId = StpUtil.getLoginIdByToken(tokenValue);
        RKeys keys = redissonClient.getKeys();
        if (keys.countExists(RedisConstant.conflictedKey + tokenValue) > 0) {
            keys.delete(conflictedKey + tokenValue);
            throw new BusinessException(40110, "用户已在其他设备上登录");
        }
        String device = DeviceUtils.getRequestDevice(request);
        String key = new String(RedisConstant.key + userId + ":" + device);
        RBucket<String> bucket = redissonClient.getBucket(key);
        if (keys.countExists(key) > 0) {
            if (!bucket.get().equals(tokenValue)) {
                String token = bucket.get();
                String conflictedKey = new String(RedisConstant.conflictedKey + token);
                RBucket<String> conflictedBucket = redissonClient.getBucket(conflictedKey);
                conflictedBucket.set("1");
                keys.delete(key);
            } else
                return;
        }
        bucket.set(tokenValue);
    }
}
