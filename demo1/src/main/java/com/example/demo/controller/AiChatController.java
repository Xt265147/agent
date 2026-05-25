package com.example.demo.controller;

import org.springframework.ai.content.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiChatController {
    private final ChatModel chatModel;

    private final ChatClient chatClient;

    // 构造注入ChatClient.Builder，自动适配LM Studio
    public AiChatController(ChatModel chatModel, ChatClient.Builder chatClientBuilder) {
        this.chatModel = chatModel;
        this.chatClient = chatClientBuilder.build();
    }
    /**
     * 使用ChatModel访问
     * @param message
     * @return
     */
    @GetMapping("/chat-model")
    public String simpleChatModel(@RequestParam String message) {
        return this.chatModel.call(message);
    }

    /**
     * 使用ChatClient访问
     * @param message
     * @return
     */
    // 极简稳定接口
    @GetMapping("/chat-client")
    public String simpleChatClient(@RequestParam String message) {
        return this.chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
    @GetMapping("/interview")
    public String mockInterview(@RequestParam String question) {
        return this.chatClient.prompt()
                // 1. 注入 System 消息，设定严厉的人设
                .system("你是一个严格的、甚至有点毒舌的高级 Java 面试官。对用户的回答要一针见血地指出缺点，不要说废话，要求十分苛刻。")
                // 2. 注入 User 消息（用户的回答）
                .user(question)
                // 3. 发送请求并获取纯文本内容
                .call()
                .content();
    }

    /**
     * 使用YML中的配置
     * @param message
     * @return
     */
    @GetMapping("/creative-writer")
    public String writePoem(@RequestParam String message) {
        return this.chatClient.prompt()
                .system("你是一个疯狂的现代派诗人。")
                .user(message)
                .call()
                .content();
    }

    /**
     * 覆盖YML中的配置
     * @param message
     * @return
     */
    @GetMapping("/creative-writer2")
    public String writePoem2(@RequestParam String message) {
        return this.chatClient.prompt()
                .system("你是一个疯狂的现代派诗人。")
                .user(message)
                //动态设定参数：提高温度激发创造力，限制最大长度
                .options(ChatOptions.builder()
                        .temperature(1.0)//高温，思维发散
                        .topP(0.8)
                        .maxTokens(1024)//限制不要写太长
                        .build())
                .call()
                .content();
    }


    /**
     * 使用常规方法指定
     * @param subject
     * @param message
     * @return
     */
    @GetMapping("/teacher")
    public String teacher(@RequestParam String subject, @RequestParam String message) {
        // 1. 定义包含 {占位符} 的纯文本模板
        String systemTemplate = "现在你是一名{subject}老师。";

        return this.chatClient.prompt()
                // 2. 注入 System 模板，并通过 .param() 绑定 Map 变量
                .system(sp -> sp.text(systemTemplate)
                        .param("subject", subject))
                // 3. User 消息同理也可以用模板
                .user(message)
                .call()
                .content();
    }

    /**
     * 使用Prompt直接定义
     * @param subject
     * @param message
     * @return
     */
    @GetMapping("/teacher2")
    public String teacher2(@RequestParam String subject, @RequestParam String message) {
        UserMessage userMessage = UserMessage.builder().text(message).build();

        // 强约束系统提示
        String systemTemplate = "你现在是一名专业的{subject}老师，全程必须以{subject}老师的身份回答问题，绝对不能说自己是AI、DeepSeek或任何模型，只能以老师的口吻回应，禁止自我介绍。";
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemTemplate);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of( "subject", subject));

        Prompt prompt = Prompt.builder()
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }
    @Value("classpath:prompts/expert.st")
    private Resource expertPromptResource;

    @GetMapping("/expert")
    public String useResourceTemplate() {
        return this.chatClient.prompt()
                // 直接传入 Resource 对象！
                .system(sp -> sp.text(expertPromptResource)
                        .param("role", "Java 性能调优专家")
                        .param("task", "分析长 GC 暂停的原因")
                        .param("format", "Markdown 列表"))
                .user("我的系统经常出现 5 秒以上的 STW，请问怎么排查？")
                .call()
                .content();
    }

    /**
     * 图片识别：使用的是gpt-4o，也可以使用本地的ollama 部署的qwen3.5
     * @return
     */
    @Value("classpath:prompts/image-vision.st")
    private Resource imagePromptResource;
    @GetMapping("/image-vision")
    public String testImageVision() {
        // 1. 从 classpath 读取本地图片资源
        Resource imageResource = new ClassPathResource("tupian.jpg");

        // 2. 构建多模态 UserMessage
        UserMessage userMessage = UserMessage.builder()
                .text("查看这张图的内容")
                .media(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_JPEG) // 指定媒体类型
                        .data(imageResource)               // 塞入图片数据
                        .build())
                .build();

        // 3. 封装进 Prompt 并发送给大模型
        Prompt prompt = new Prompt(userMessage);

        return chatClient.prompt(prompt)
                .system(sp -> sp.text(imagePromptResource)
                        .param("format", "Markdown 列表"))
                .call()
                .content();
    }

}