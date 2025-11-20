package com.xiaochen.mianshiya.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.exception.BusinessException;
import com.xiaochen.mianshiya.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;

import static com.xiaochen.mianshiya.constant.UserConstant.USER_LOGIN_STATE;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Autowired
    private TellKicked tellKicked;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//         注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                           .check(r -> tellKicked.isConflicted());
            SaRouter.match("/**")
                        .check(r -> {
                            User user = (User) StpUtil.getSession().get(USER_LOGIN_STATE);
                            if ("Ban".equals(user.getUserRole()))
                                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您已被封");
//                            .check(r -> StpUtil.checkLogin());
                        });
        })).addPathPatterns("/**")
                .excludePathPatterns("/user/login");
    }
}