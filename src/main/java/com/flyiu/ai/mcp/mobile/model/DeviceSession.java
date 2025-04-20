package com.flyiu.ai.mcp.mobile.model;

import io.appium.java_client.AppiumDriver;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备会话模型
 */
@Data
public class DeviceSession {
    // 设备名称
    private final String deviceName;
    // Appium驱动实例
    private final AppiumDriver driver;
    // 会话创建时间
    private final LocalDateTime createdAt;

    public DeviceSession(String deviceName, AppiumDriver driver) {
        this.deviceName = deviceName;
        this.driver = driver;
        this.createdAt = LocalDateTime.now();
    }
}