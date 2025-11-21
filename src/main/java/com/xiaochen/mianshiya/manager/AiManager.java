package com.xiaochen.mianshiya.manager;

import cn.hutool.core.collection.CollUtil;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用的 Ai 调用类
 */
@Component
public class AiManager {
    @Resource
    private ArkService service;

    private String DEFAULT_AIMODEL = "deepseek-v3-250324";

    public String doChat(String userPrompt){
        return doChat("", userPrompt, DEFAULT_AIMODEL);
    }

    public String doChat(String systemPrompt, String userPrompt){
        return doChat(systemPrompt, userPrompt, DEFAULT_AIMODEL);
    }
    public String doChat(String systemPrompt, String userPrompt, String model){
        System.out.println("\n----- standard request -----");
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemPrompt).build();
        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userPrompt).build();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                // 指定您创建的方舟推理接入点 ID，此处已帮您修改为您的推理接入点 ID
//                .model("deepseek-v3-250324")
                .model(model)
                .messages(messages)
                .build();

        List<ChatCompletionChoice> choices = service.createChatCompletion(chatCompletionRequest).getChoices();
        try {
            if (CollUtil.isNotEmpty(choices)) {
                return (String) choices.get(0).getMessage().getContent();
            }
        }catch (Exception ex){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Ai 操作失败，无返回结果");
        }
//        service.shutdownExecutor();
        return null;
    }
}
