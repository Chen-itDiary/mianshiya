package com.xiaochen.mianshiya.constant;

public interface RedisConstant {
    /**
     * Redis 常量
     */

    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * 获取用户签到的 Redis Key
     *
     * @param year
     * @param userId
     * @return 拼接好的Redis Key
     */
    static String getUserSignInRedisKey(int year, long userId) {
        return String.format("%s:%s:%s", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }
}
