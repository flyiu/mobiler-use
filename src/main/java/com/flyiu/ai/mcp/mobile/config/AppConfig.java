package com.flyiu.ai.mcp.mobile.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用主配置类
 */
@Configuration
@EnableConfigurationProperties({ AppiumConfig.class, DeviceConfig.class })
public class AppConfig {
    // 后续可添加全局Bean配置
}