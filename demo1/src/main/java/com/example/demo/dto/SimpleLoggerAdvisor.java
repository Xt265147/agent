package com.example.demo.dto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 自定义日志拦截器（顾问）
 */
@Slf4j
public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public String getName(){
        return this.getClass().getSimpleName();
    }

    /**
     * 顺序控制：设为 0 表示优先级很高，最外层执行
     */
    @Override
    public int getOrder() {
        return 0;
    }
    // ================= 拦截同步调用 (.call) =================
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 请求大模型前：打印请求头和 Prompt
        logRequest(request);

        // 放行请求，交给链条中的下一个 Advisor 或底层 ChatModel
        ChatClientResponse response = chain.nextCall(request);

        // 拿到大模型结果后：打印返回信息
        logResponse(response);

        return response;
    }

    // ================= 拦截流式调用 (.stream) =================
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // [Pre-process] 请求发出前打印
        logRequest(request);

        // 放行流式请求
        Flux<ChatClientResponse> responseFlux = chain.nextStream(request);

        // 注意：流式响应是一点点回来的。
        // 我们使用 ChatClientMessageAggregator 将所有碎片段拼接完整后，再执行打印
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(responseFlux, this::logResponse);
    }

    // 内部打印方法
    private void logRequest(ChatClientRequest request) {
        log.info("[发往大模型的请求]: {}", request);
    }

    private void logResponse(ChatClientResponse response) {
        log.info(" [大模型的完整响应]: {}", response);
    }
}
