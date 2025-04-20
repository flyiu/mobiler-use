package com.flyiu.ai.mcp.mobile.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 设备配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "devices")
public class DeviceConfig {

    private List<DeviceCapabilities> android;
    private List<DeviceCapabilities> ios;

    @Data
    public static class DeviceCapabilities {
        private String name;
        private String platformName;
        private String automationName;
        private String udid;
        private boolean noReset;
        private int newCommandTimeout;
        // 可以添加更多Appium支持的capabilities
        private Map<String, Object> extraCapabilities;
    }
}