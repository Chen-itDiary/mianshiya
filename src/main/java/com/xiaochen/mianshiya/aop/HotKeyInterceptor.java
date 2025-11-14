package com.xiaochen.mianshiya.aop;

import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.xiaochen.mianshiya.annotation.HotKeyCheck;
import com.xiaochen.mianshiya.common.ResultUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * 权限校验 AOP
 *
 * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
 * @from <a href="https://xiaochen.icu">编程导航知识星球</a>
 */
@Aspect
@Component
public class HotKeyInterceptor {

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param hotKeyCheck
     * @return
     */
    @Around("@annotation(hotKeyCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, HotKeyCheck hotKeyCheck) throws Throwable {
        // 1. 获取注解中定义的 SpEL
        String spelExpression = hotKeyCheck.hotKey();
        // 2. 解析 SpEL 表达式，生成动态 hotKey
        String hotKey = generateHotKey(joinPoint, spelExpression);
        Object object = null;
        if(JdHotKeyStore.isHotKey(hotKey)){
           object = JdHotKeyStore.get(hotKey);
           if(object != null){
               return ResultUtils.success(object);
           }
       }
       object = joinPoint.proceed();
       JdHotKeyStore.smartSet(hotKey, object);
       return joinPoint.proceed();
    }
    /**
     * 解析 SpEL 表达式，生成动态 hotKey
     */
    private String generateHotKey(ProceedingJoinPoint joinPoint, String spelExpression) {
        try {
            // 获取方法签名和参数信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames(); // 方法参数名（依赖 -parameters 编译参数）
            Object[] paramValues = joinPoint.getArgs(); // 方法参数值

            // 构建 SpEL 上下文（将参数名和值绑定到上下文）
            StandardEvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], paramValues[i]);
            }
            // 解析表达式，生成最终 hotKey
            SpelExpressionParser spelParser = new SpelExpressionParser();
            return spelParser.parseExpression(spelExpression).getValue(context, String.class);
        } catch (Exception e) {
            // 解析失败（如参数名不匹配、空指针等），打印日志并返回 null
            e.printStackTrace();
            return null;
        }
    }
}

