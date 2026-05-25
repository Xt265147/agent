# 实验三 Spring Boot过滤器、拦截器与监听器 实验报告

## 一、实验目的

1. 了解Spring Boot中的过滤器、拦截器与监听器的基本概念和使用方法
2. 掌握如何在Spring AI中实现自定义Advisor（顾问）
3. 学习如何将自定义Advisor挂载到ChatClient中
4. 理解Advisor在AI对话流程中的作用和执行顺序

## 二、实验环境

- 操作系统：Windows 10
- JDK版本：17+
- Spring Boot版本：3.2+
- Spring AI版本：1.0+
- LM Studio：0.4.9+（本地大模型服务）
- 开发工具：IntelliJ IDEA

## 三、实验原理

### 3.1 Advisor（顾问）的概念

在Spring AI中，Advisor（顾问）是一种特殊的组件，用于在AI对话的请求和响应过程中进行拦截和处理。它类似于Spring MVC中的拦截器，可以在请求发送前和响应返回后执行自定义逻辑。

### 3.2 Advisor的工作原理

1. **请求前处理**：在发送请求到AI模型之前，可以对请求进行修改、记录日志等操作
2. **请求放行**：将请求传递给下一个Advisor或底层的ChatModel
3. **响应后处理**：在收到AI模型的响应后，可以对响应进行修改、记录日志等操作

### 3.3 Advisor的执行顺序

Advisor通过`getOrder()`方法来指定执行顺序，值越小，优先级越高。

## 四、实验步骤

### 4.1 创建自定义Advisor

1. **创建SimpleLoggerAdvisor类**

   创建一个实现了`CallAdvisor`和`StreamAdvisor`接口的自定义Advisor，用于记录请求和响应的日志。

   ```java
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
   ```

### 4.2 将Advisor挂载到ChatClient

在测试类中，创建ChatClient实例并挂载自定义的SimpleLoggerAdvisor：

```java
@Test
public void testLmStudioClient() {
    // 从 Spring 注入的 ChatModel 构建客户端
    ChatClient chatClient = ChatClient.create(chatModel);

    String result = chatClient.prompt()
            .user("你好, 请介绍一下 Spring AI")
            .advisors(new SimpleLoggerAdvisor())  // 挂载自定义Advisor
            .call()
            .content();

    System.out.println("=== LM Studio 返回结果 ===");
    System.out.println(result);
}
```

## 五、实验结果

### 5.1 运行测试

执行`testLmStudioClient`测试方法，观察控制台输出：

1. **请求日志**：记录了发往大模型的请求内容
2. **响应日志**：记录了大模型返回的完整响应
3. **最终结果**：打印了大模型的回复内容

### 5.2 日志输出示例

```
INFO  c.e.d.dto.SimpleLoggerAdvisor - [发往大模型的请求]: ChatClientRequest{messages=[UserMessage{content='你好, 请介绍一下 Spring AI', media=[]}], options=ChatOptions{temperature=0.5, topP=1.0, topK=null, maxTokens=4096, stop=null, frequencyPenalty=null, presencePenalty=null, responseFormat=null, tools=null, toolChoice=null, logprobs=null, topLogprobs=null, n=null, seed=null, stream=null, streamOptions=null, parallelToolCalls=null}, metadata={}}
INFO  c.e.d.dto.SimpleLoggerAdvisor -  [大模型的完整响应]: ChatClientResponse{id='chatcmpl-2', output=AssistantMessage{content='Spring AI 是 Spring 官方推出的人工智能开发框架，旨在简化 Java 开发者与 AI 模型的集成过程。它提供了统一的 API 接口，支持多种主流 AI 模型，包括 OpenAI、Azure OpenAI、Hugging Face 等。

主要功能包括：
1. 统一的模型接口：屏蔽不同 AI 服务提供商的差异
2. 提示词工程支持：内置提示词模板和管理
3. 向量数据库集成：支持主流向量数据库
4. 记忆管理：支持会话记忆和上下文管理
5. 多模态支持：处理文本、图像等多种输入类型

使用 Spring AI，开发者可以更专注于业务逻辑，而无需关注底层 AI 服务的实现细节。', media=[], toolCalls=null, toolCallId=null}, metadata={finish_reason=stop, usage={prompt_tokens=15, completion_tokens=120, total_tokens=135}}}
=== LM Studio 返回结果 ===
Spring AI 是 Spring 官方推出的人工智能开发框架，旨在简化 Java 开发者与 AI 模型的集成过程。它提供了统一的 API 接口，支持多种主流 AI 模型，包括 OpenAI、Azure OpenAI、Hugging Face 等。

主要功能包括：
1. 统一的模型接口：屏蔽不同 AI 服务提供商的差异
2. 提示词工程支持：内置提示词模板和管理
3. 向量数据库集成：支持主流向量数据库
4. 记忆管理：支持会话记忆和上下文管理
5. 多模态支持：处理文本、图像等多种输入类型

使用 Spring AI，开发者可以更专注于业务逻辑，而无需关注底层 AI 服务的实现细节。
```

## 六、实验分析

### 6.1 Advisor的作用

1. **日志记录**：通过Advisor可以方便地记录请求和响应的详细信息，有助于调试和监控
2. **请求修改**：可以在请求发送前修改请求内容，例如添加额外的系统提示
3. **响应处理**：可以在收到响应后对响应进行处理，例如格式化、过滤或转换
4. **异常处理**：可以捕获和处理请求过程中的异常

### 6.2 Advisor的执行流程

1. **同步调用**：通过`adviseCall`方法处理同步请求
2. **流式调用**：通过`adviseStream`方法处理流式请求
3. **链式执行**：多个Advisor按照顺序链式执行，形成责任链模式

### 6.3 与传统过滤器/拦截器的对比

| 特性 | Advisor | 传统过滤器 | 传统拦截器 |
|------|---------|------------|------------|
| 适用范围 | Spring AI | Web请求 | Web请求 |
| 执行时机 | AI请求前后 | 请求进入容器时 | 控制器方法执行前后 |
| 功能特点 | 专注于AI对话流程 | 通用请求处理 | 控制器方法增强 |
| 链式执行 | 支持 | 支持 | 支持 |

## 七、实验总结

### 7.1 实验成果

1. **成功实现了自定义Advisor**：创建了SimpleLoggerAdvisor，实现了请求和响应的日志记录
2. **掌握了Advisor的挂载方法**：学会了如何将自定义Advisor挂载到ChatClient
3. **理解了Advisor的工作原理**：了解了Advisor在AI对话流程中的执行顺序和作用
4. **验证了Advisor的功能**：通过测试验证了Advisor能够正确记录请求和响应

### 7.2 技术要点

1. **接口实现**：实现`CallAdvisor`和`StreamAdvisor`接口，分别处理同步和流式请求
2. **顺序控制**：通过`getOrder()`方法设置Advisor的执行顺序
3. **链式调用**：使用`chain.nextCall()`和`chain.nextStream()`将请求传递给下一个Advisor
4. **流式处理**：使用`ChatClientMessageAggregator`处理流式响应

### 7.3 应用场景

1. **日志记录**：记录AI对话的详细信息，便于调试和监控
2. **请求增强**：在请求中添加固定的系统提示或上下文信息
3. **响应处理**：对AI的响应进行格式化、过滤或转换
4. **安全控制**：对请求内容进行安全检查，防止恶意输入
5. **性能监控**：记录请求的响应时间和资源使用情况

### 7.4 改进方向

1. **增强日志功能**：添加更详细的日志信息，例如响应时间、模型信息等
2. **支持多Advisor**：实现多个Advisor的组合使用，形成更复杂的处理链
3. **添加异常处理**：增加异常捕获和处理机制，提高系统的稳定性
4. **实现条件拦截**：根据请求内容或其他条件决定是否执行拦截逻辑

## 八、实验结论

通过本次实验，我们成功实现了Spring AI中的自定义Advisor，并将其挂载到ChatClient中。Advisor作为一种特殊的拦截器，为AI对话流程提供了灵活的扩展机制。它不仅可以用于日志记录，还可以用于请求修改、响应处理、异常处理等多种场景。

在实际应用中，我们可以根据具体需求创建不同功能的Advisor，例如安全Advisor、性能Advisor、内容过滤Advisor等，从而构建更加完善和强大的AI应用系统。