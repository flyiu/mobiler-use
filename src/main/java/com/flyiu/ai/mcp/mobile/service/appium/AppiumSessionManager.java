package com.flyiu.ai.mcp.mobile.service.appium;

import com.flyiu.ai.mcp.mobile.config.AppiumConfig;
import com.flyiu.ai.mcp.mobile.config.DeviceConfig;
import com.flyiu.ai.mcp.mobile.model.DeviceSession;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.Setting;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appium会话管理服务
 */
@Slf4j
@Service
public class AppiumSessionManager {

    private final AppiumConfig appiumConfig;
    private final DeviceConfig deviceConfig;
    private final AppiumServerService appiumServerService;

    // 存储已创建的设备会话
    private final Map<String, DeviceSession> deviceSessions = new ConcurrentHashMap<>();

    @Autowired
    public AppiumSessionManager(AppiumConfig appiumConfig, DeviceConfig deviceConfig,
            AppiumServerService appiumServerService) {
        this.appiumConfig = appiumConfig;
        this.deviceConfig = deviceConfig;
        this.appiumServerService = appiumServerService;
    }

    /**
     * 检查Appium服务器状态，必要时启动服务器
     * 
     * @return 服务器是否可用
     */
    private boolean ensureServerRunning() {
        if (appiumServerService.isServerRunning()) {
            return true;
        }

        if (appiumConfig.getServer().isAutoStart()) {
            log.info("Appium服务器未运行，根据配置正在尝试自动启动");
            return appiumServerService.startServer();
        } else {
            log.warn("Appium服务器未运行，且未配置自动启动");
            return false;
        }
    }

    /**
     * 创建Android设备会话
     * 
     * @param capabilities 设备配置
     * @return 创建结果
     */
    public Map<String, Object> createAndroidSession(DeviceConfig.DeviceCapabilities capabilities) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查服务器状态，必要时启动
            if (!ensureServerRunning()) {
                throw new RuntimeException("Appium服务器未运行，无法创建会话");
            }

            // 使用UiAutomator2Options替代DesiredCapabilities
            UiAutomator2Options options = new UiAutomator2Options();
            options.setPlatformName("Android");
            options.setAutomationName("UiAutomator2");
            options.setNoReset(capabilities.isNoReset());
            options.setNewCommandTimeout(Duration.ofSeconds(capabilities.getNewCommandTimeout()));

            // 如果udid不是"auto"，则设置具体的udid
            if (!"auto".equals(capabilities.getUdid())) {
                options.setUdid(capabilities.getUdid());
            }

            if (capabilities.getName() != null && !capabilities.getName().isEmpty()) {
                options.setDeviceName(capabilities.getName());
            }

            URL serverUrl = new URL(appiumConfig.getServer().getUrl());
            log.info("创建Android设备会话，设备名称: {}, URL: {}", capabilities.getName(), serverUrl);

            // 使用正确的选项创建AndroidDriver
            AndroidDriver driver = new AndroidDriver(serverUrl, options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            // driver.setSettings(Map.of("fixImageFindScreenshotDims", False));
            // driver.setSetting(Setting.FIX_IMAGE_FIND_SCREENSHOT_DIMENSIONS, false);
            // driver.setSetting(Setting.FIX_IMAGE_TEMPLATE_SIZE, true);
            // driver.setSetting(Setting.UPDATE_IMAGE_ELEMENT_POSITION, true);

            // 将结果保存起来
            deviceSessions.put(capabilities.getName(), new DeviceSession(capabilities.getName(), driver));

            // 返回成功信息
            result.put("success", true);
            result.put("deviceName", capabilities.getName());
            result.put("sessionId", driver.getSessionId().toString());

            log.info("成功创建Android设备会话，设备名称: {}, 会话ID: {}",
                    capabilities.getName(), driver.getSessionId());

        } catch (Exception e) {
            log.error("创建Android设备会话失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 创建iOS设备会话
     * 
     * @param deviceName 设备名称
     * @return 设备会话
     */
    public DeviceSession createIOSSession(String deviceName) {
        try {
            // 检查服务器状态，必要时启动
            if (!ensureServerRunning()) {
                throw new RuntimeException("Appium服务器未运行，无法创建会话");
            }

            DeviceConfig.DeviceCapabilities deviceCaps = findIOSDevice(deviceName);
            if (deviceCaps == null) {
                throw new IllegalArgumentException("未找到指定的iOS设备配置: " + deviceName);
            }

            DesiredCapabilities capabilities = createCapabilities(deviceCaps);
            URL appiumServerUrl = new URL(appiumConfig.getServer().getUrl());

            IOSDriver driver = new IOSDriver(appiumServerUrl, capabilities);
            DeviceSession session = new DeviceSession(deviceCaps.getName(), driver);

            deviceSessions.put(deviceCaps.getName(), session);
            log.info("成功创建iOS设备会话: {}", deviceCaps.getName());

            return session;
        } catch (Exception e) {
            log.error("创建iOS设备会话失败", e);
            throw new RuntimeException("创建iOS设备会话失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取设备会话
     * 
     * @param deviceName 设备名称
     * @return 设备会话
     */
    public Optional<DeviceSession> getSession(String deviceName) {
        return Optional.ofNullable(deviceSessions.get(deviceName));
    }

    /**
     * 关闭设备会话
     * 
     * @param deviceName 设备名称
     */
    public void closeSession(String deviceName) {
        DeviceSession session = deviceSessions.get(deviceName);
        if (session != null) {
            try {
                session.getDriver().quit();
                deviceSessions.remove(deviceName);
                log.info("成功关闭设备会话: {}", deviceName);
            } catch (Exception e) {
                log.error("关闭设备会话失败: {}", deviceName, e);
            }
        }
    }

    /**
     * 关闭所有设备会话
     */
    public void closeAllSessions() {
        deviceSessions.forEach((name, session) -> {
            try {
                session.getDriver().quit();
                log.info("成功关闭设备会话: {}", name);
            } catch (Exception e) {
                log.error("关闭设备会话失败: {}", name, e);
            }
        });
        deviceSessions.clear();
    }

    // 查找Android设备配置
    public DeviceConfig.DeviceCapabilities findAndroidDevice(String deviceName) {
        if (deviceConfig.getAndroid() == null) {
            return null;
        }

        return deviceConfig.getAndroid().stream()
                .filter(device -> device.getName().equals(deviceName))
                .findFirst()
                .orElse(deviceConfig.getAndroid().isEmpty() ? null : deviceConfig.getAndroid().get(0));
    }

    // 查找iOS设备配置
    private DeviceConfig.DeviceCapabilities findIOSDevice(String deviceName) {
        if (deviceConfig.getIos() == null) {
            return null;
        }

        return deviceConfig.getIos().stream()
                .filter(device -> device.getName().equals(deviceName))
                .findFirst()
                .orElse(deviceConfig.getIos().isEmpty() ? null : deviceConfig.getIos().get(0));
    }

    // 创建设备能力配置
    private DesiredCapabilities createCapabilities(DeviceConfig.DeviceCapabilities deviceCaps) {
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability("platformName", deviceCaps.getPlatformName());
        capabilities.setCapability("automationName", deviceCaps.getAutomationName());

        if (!"auto".equals(deviceCaps.getUdid())) {
            capabilities.setCapability("udid", deviceCaps.getUdid());
        }

        capabilities.setCapability("noReset", deviceCaps.isNoReset());
        capabilities.setCapability("newCommandTimeout", deviceCaps.getNewCommandTimeout());

        // 添加额外的capabilities
        if (deviceCaps.getExtraCapabilities() != null) {
            deviceCaps.getExtraCapabilities().forEach(capabilities::setCapability);
        }

        return capabilities;
    }
}