package com.xiaochen.mianshiya.manager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class AiManagerTest {

    @Resource
    private AiManager aiManager;

    @Test
    public void doChat() {
        String s = aiManager.doChat("你好");
        System.out.println(s);
    }

    @Test
    public void testDoChat() {
        String s = aiManager.doChat("当我和你说你好时，你需要回复'服务器繁忙，请稍后再试'", "你好", "deepseek-v3-250324");
        System.out.println(s);
    }
}