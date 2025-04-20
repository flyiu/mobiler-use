package com.flyiu.ai.mcp.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.flyiu.ai.mcp.mobile.service.screenshot.RecordService;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumSessionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * MCP移动设备控制应用
 * 主要功能:
 * - 封装Appium常规操作提供REST API
 * - 支持设备截图
 * - 支持Spring AI MCP协议进行AI交互
 */
// 禁用OpenAI自动配置
@SpringBootApplication
@ComponentScan(basePackages = { "com.flyiu.ai.mcp.mobile" })
@EnableConfigurationProperties
@Slf4j
public class MobileApplication {

    @Value("${app.auto-record:false}")
    private boolean autoRecord;

    @Value("${app.auto-record-device:}")
    private String autoRecordDevice;

    @Value("${app.auto-record-duration:300}")
    private int autoRecordDuration;

    public static void main(String[] args) {
        SpringApplication.run(MobileApplication.class, args);
        System.out.println("MCP移动设备控制应用启动成功");
    }

    @Bean
    public CommandLineRunner autoRecordRunner(RecordService recordService,
            AppiumSessionManager sessionManager) {
        return args -> {
            if (autoRecord && autoRecordDevice != null && !autoRecordDevice.trim().isEmpty()) {
                try {
                    log.info("系统启动时自动开始录制设备: {}, 时长: {}秒", autoRecordDevice, autoRecordDuration);

                    // 检查设备是否已连接
                    if (sessionManager.getSession(autoRecordDevice).isPresent()) {
                        // 启动录制
                        recordService.startRecording(autoRecordDevice, autoRecordDuration);
                        log.info("已成功启动自动录制");
                    } else {
                        log.warn("无法启动自动录制，设备 {} 未连接", autoRecordDevice);
                    }
                } catch (Exception e) {
                    log.error("自动录制启动失败", e);
                }
            }
        };
    }
}
