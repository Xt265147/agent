package com.example.demo;

import com.example.demo.dto.SimpleLoggerAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Demo1ApplicationTests {

    // 自动注入当前激活的模型
    @Autowired
    private ChatModel chatModel;

    @Test
    void contextLoads() {
    }

    @Test
    public void testLmStudioClient() {
        // 从 Spring 注入的 ChatModel 构建客户端
        ChatClient chatClient = ChatClient.create(chatModel);

        String result = chatClient.prompt()
                .user("你好, 请介绍一下 Spring AI")
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();

        System.out.println("=== LM Studio 返回结果 ===");
        System.out.println(result);
    }
}