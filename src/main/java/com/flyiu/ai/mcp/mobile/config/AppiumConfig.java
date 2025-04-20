package com.flyiu.ai.mcp.mobile.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Appium配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "appium")
public class AppiumConfig {

    private Server server;
    private Screenshots screenshots;
    private Recording recording;

    @Data
    public static class Server {
        private String url;
        private int connectTimeout = 5000; // 默认连接超时时间5秒
        private boolean autoStart;
        private String appiumPath = "appium"; // Appium可执行文件路径，默认为"appium"
    }

    @Data
    public static class Screenshots {
        private String storagePath;
        private String format;
        private int quality;
    }

    @Data
    public static class Recording {
        private String storagePath; // 视频存储路径
        private String format = "mp4"; // 视频格式，默认为mp4
        private String quality = "medium"; // 视频质量，可选值: low, medium, high, photo
        private int defaultDuration = 180; // 默认录制时长（秒）
        private int maxDuration = 3600; // 最大录制时长（秒）
        private String syncPath; // 视频同步路径，如果为空则使用用户主目录下的AppiumVideos
    }
}