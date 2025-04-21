package com.flyiu.ai.mcp.mobile.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;

@Service
public class AiService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private OpenAiChatModel chatModel;


    @PostConstruct
    public void init() {
        // 初始化大模型
        chatModel = OpenAiChatModel.builder().apiKey(apiKey).baseUrl(baseUrl).modelName(model).build();
    }

    public String imageRecognition(String imageBase64, String prompt, ResponseFormat responseFormat) {

        // 由于Spring AI当前版本限制，使用文本方式发送请求
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
        contents.add(TextContent.from(prompt));
        contents.add(ImageContent.from("data:image/png;base64," + imageBase64));
        ChatMessage userMessage = UserMessage.from(contents);
        ChatRequest chatRequest = ChatRequest.builder().responseFormat(responseFormat).messages(List.of(userMessage))
                .build();
        ChatResponse chatResponse = chatModel.chat(chatRequest);
        return chatResponse.aiMessage().text();
    }

}
