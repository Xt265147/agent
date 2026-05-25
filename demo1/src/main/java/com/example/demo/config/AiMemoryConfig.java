package com.example.demo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * JDBC 存储记忆
 *
 */
@Configuration
public class AiMemoryConfig {
/*
    @Bean
    public ChatMemory chatMemory(JdbcTemplate jdbcTemplate) {
        // 1. 构建基于 MySQL 语法的 JDBC 记忆库，手动创建repository，也可以通过@Bean 覆盖
        JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                // 设置mysql方言
                .dialect(new MysqlChatMemoryRepositoryDialect())
                .build();

        // 2. 将此 Repository 注入到滑动窗口策略中
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .build();
    }*/
}
