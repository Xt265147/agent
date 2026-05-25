package com.example.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自动记忆
 *
 */
@RestController
public class AutoMemoryController {

    private final ChatClient chatClient;

    // 构造器注入 Builder 和 ChatMemory
    public AutoMemoryController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                // 为 ChatClient 挂载全局记忆拦截器
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("你是一个贴心的私人助理，你需要努力记住用户的偏好。")
                .build();
    }

    @GetMapping("/api/chat")
    public String chatWithMemory(@RequestParam String msg) {
        // 由于配置了 Advisor，每次 call() 前后都会自动读写 chatMemory！
        return chatClient.prompt()
                .user(msg)
                .call()
                .content();
    }

    /**
     * @param userId 用户的唯一标识 (例如登录用户的 token 或 uuid)
     * @param msg 用户的新问题
     */
    @GetMapping("/api/chat/isolated")
    public String chatWithIsolatedMemory(
            @RequestParam String userId,
            @RequestParam String msg) {

        return chatClient.prompt()
                .user(msg)
                //在每次请求时，动态传入用户的专属会话 ID，取值见BaseChatMemoryAdvisor#getConversationId
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                // 设置值在call()时，见：DefaultChatClientUtils#toChatClientRequest
                .call()
                .content();
    }
}
