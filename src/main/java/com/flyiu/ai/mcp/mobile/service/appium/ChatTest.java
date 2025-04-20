package com.flyiu.ai.mcp.mobile.service.appium;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatTest {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public void test() throws IOException {
        String prompt = "请分析这张手机屏幕截图，识别并列出所有可见的UI元素。\n" +
        // "屏幕尺寸为: " + screenWidth + "x" + screenHeight + "像素。\n" +
                "对于每个元素，请提供以下信息：\n" +
                "1. 元素类型（按钮、文本框、图片等）\n ,图片和按钮如果文字为空，请返回的text中简单描述" +
                "2. 元素文本内容（如果有）\n" +
                "3. 元素的准确边界框坐标（x坐标, y坐标, 宽度, 高度），坐标是指元素左上角在屏幕上的位置\n" +
                "4. 元素的中心点坐标（centerX, centerY），用于点击操作\n" +
                "5. 元素是否可能是可交互的（可点击、可输入等）\n\n" +
                "请以JSON格式返回，每个元素包含上述属性。格式示例：\n" +
                "[{\"type\": \"按钮\", \"text\": \"确定\", \"bounds\": {\"x\": 100, \"y\": 200, \"width\": 80, \"height\": 40}, \"center\": {\"x\": 140, \"y\": 220}, \"interactive\": true}]";

        // 使用配置信息初始化模型
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .build();

        // 读取截图文件
        File screenshotFile = new File("./screenshots/default-android_latest.png");
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", screenshotFile.getAbsolutePath());
            return;
        }

        byte[] fileContent = Files.readAllBytes(screenshotFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(fileContent);
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
        contents.add(TextContent.from(prompt));
        contents.add(ImageContent.from("data:image/png;base64," + base64Image));

        ChatMessage userMessage = UserMessage.from(contents);

        ChatRequest chatRequest = ChatRequest.builder().messages(List.of(userMessage)).build();
        ChatResponse response = chatModel.chat(chatRequest);
        log.info("大模型分析结果: {}", response.aiMessage().text());
    }

    // 主方法改为私有，避免直接调用，应当通过Spring Bean使用
    private static void main(String[] args) {
        log.error("本类不应当直接通过main方法运行，请通过Spring上下文使用");
    }
}
