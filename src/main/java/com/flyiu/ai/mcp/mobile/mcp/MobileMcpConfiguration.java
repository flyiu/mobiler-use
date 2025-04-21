package com.flyiu.ai.mcp.mobile.mcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.flyiu.ai.mcp.mobile.config.DeviceConfig;
import com.flyiu.ai.mcp.mobile.model.DeviceSession;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumOperationService;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumSessionManager;
import com.flyiu.ai.mcp.mobile.service.screenshot.ScreenshotService;

import io.appium.java_client.android.AndroidDriver;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP工具配置类
 * 使用Spring AI提供的方法工具回调提供者注册移动设备控制相关的MCP工具
 */
@Slf4j
@Configuration
public class MobileMcpConfiguration {

    private final AppiumSessionManager sessionManager;
    private final AppiumOperationService operationService;
    private final ScreenshotService screenshotService;

    @Autowired
    public MobileMcpConfiguration(
            AppiumSessionManager sessionManager,
            AppiumOperationService operationService,
            ScreenshotService screenshotService) {
        this.sessionManager = sessionManager;
        this.operationService = operationService;
        this.screenshotService = screenshotService;
    }

    /**
     * 注册MCP工具回调
     */
    @Bean
    public ToolCallbackProvider mobileToolCallbackProvider() {
        // 创建工具对象
        MobileTools mobileTools = new MobileTools(sessionManager, operationService, screenshotService);

        // 注册工具对象
        return MethodToolCallbackProvider.builder()
                .toolObjects(mobileTools)
                .build();
    }

    /**
     * 内部类，包含所有移动设备控制工具方法
     */
    public static class MobileTools {

        private final AppiumSessionManager sessionManager;
        private final AppiumOperationService operationService;
        private final ScreenshotService screenshotService;
        private final Map<String, AndroidDriver> driverMap = new HashMap<>();

        public MobileTools(
                AppiumSessionManager sessionManager,
                AppiumOperationService operationService,
                ScreenshotService screenshotService) {
            this.sessionManager = sessionManager;
            this.operationService = operationService;
            this.screenshotService = screenshotService;
        }

        /**
         * 连接Android设备
         */
        @Tool(name = "connectAndroid", description = "连接Android设备")
        public Map<String, Object> connectAndroid(@ToolParam(description = "默认使用 default-android") String deviceName) {
            try {
                deviceName = (deviceName == null || deviceName.isEmpty()) ? "default-android" : deviceName;

                // 查找设备配置
                DeviceConfig.DeviceCapabilities deviceCaps = sessionManager.findAndroidDevice(deviceName);
                if (deviceCaps == null) {
                    return Map.of(
                            "success", false,
                            "error", "未找到指定的Android设备配置: " + deviceName);
                }

                // 调用会话管理器的方法
                Map<String, Object> result = sessionManager.createAndroidSession(deviceCaps);
                return result;

            } catch (Exception e) {
                log.error("连接Android设备失败", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 连接iOS设备
         */
        @Tool(name = "connectIOS", description = "连接iOS设备")
        public Map<String, Object> connectIOS(String deviceName) {
            try {
                deviceName = (deviceName == null || deviceName.isEmpty()) ? "default-ios" : deviceName;
                DeviceSession session = sessionManager.createIOSSession(deviceName);
                return Map.of(
                        "success", true,
                        "deviceName", session.getDeviceName(),
                        "message", "成功连接iOS设备: " + deviceName);
            } catch (Exception e) {
                log.error("连接iOS设备失败", e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 连接Android设备（兼容原方法）
         */
        // @Tool(name = "connectAndroidDevice", description = "连接Android设备")
        // public Map<String, Object> connectAndroidDevice(String deviceName) {
        // Map<String, Object> result = new HashMap<>();

        // try {
        // // 使用W3C标准的UiAutomator2Options替代DesiredCapabilities
        // UiAutomator2Options options = new UiAutomator2Options();
        // options.setPlatformName("Android");
        // options.setAutomationName("UiAutomator2");
        // options.setNoReset(true); // 不重置应用状态
        // options.setNewCommandTimeout(Duration.ofSeconds(300));

        // // 可以根据deviceName设置特定参数
        // // options.setUdid("UDID");
        // // options.setDeviceName(deviceName);

        // AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"),
        // options);
        // driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // // 保存驱动实例
        // driverMap.put(deviceName, driver);

        // result.put("success", true);
        // result.put("deviceName", deviceName);
        // result.put("sessionId", driver.getSessionId().toString());

        // } catch (Exception e) {
        // result.put("success", false);
        // result.put("error", e.getMessage());
        // }

        // return result;
        // }

        /**
         * 断开设备连接
         */
        @Tool(name = "disconnectDevice", description = "断开设备连接")
        public Map<String, Object> disconnectDevice(String deviceName) {
            try {
                sessionManager.closeSession(deviceName);
                return Map.of(
                        "success", true,
                        "message", "成功断开设备连接: " + deviceName);
            } catch (Exception e) {
                log.error("断开设备连接失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 启动应用
         */
        @Tool(name = "launchApp", description = "启动应用")
        public Map<String, Object> launchApp(String deviceName, String appPackage,
                @ToolParam(description = "应用名称,在使用appPackage启动失败时,会使用appName启动") String appName) {
            try {
                operationService.restartApp(deviceName, appPackage, appName, 5);
                // operationService.launchApp(deviceName, appPackage, appActivity);
                return Map.of(
                        "success", true,
                        "message", "成功启动应用");
            } catch (Exception e) {
                log.error("启动应用失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 等待元素出现
         */
        @Tool(name = "waitForElement", description = "等待元素出现")
        public Map<String, Object> waitForElement(String deviceName, String locatorType, String locatorValue,
                Integer timeoutInSeconds) {
            try {
                By locator = createLocator(locatorType, locatorValue);
                boolean found = operationService.waitForElement(deviceName, locator,
                        timeoutInSeconds != null ? timeoutInSeconds : 10);
                return Map.of(
                        "success", true,
                        "found", found);
            } catch (Exception e) {
                log.error("等待元素失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 点击元素
         */
        // @Tool(name = "clickElement", description = "点击元素")
        // public Map<String, Object> clickElement(String deviceName, String
        // locatorType, String locatorValue) {
        // try {
        // By locator = createLocator(locatorType, locatorValue);
        // operationService.clickElement(deviceName, locator);
        // return Map.of(
        // "success", true,
        // "message", "成功点击元素");
        // } catch (Exception e) {
        // log.error("点击元素失败: {}", deviceName, e);
        // return Map.of(
        // "success", false,
        // "error", e.getMessage());
        // }
        // }

        /**
         * 在元素中输入文本
         */
        @Tool(name = "sendKeys", description = "在元素中输入文本")
        public Map<String, Object> sendKeys(String deviceName, String text) {
            try {
                operationService.sendKeysToCurrentInput(deviceName, text);
                return Map.of(
                        "success", true,
                        "message", "成功输入文本");
            } catch (Exception e) {
                log.error("输入文本失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * Back / Home / Menu
         */
        @Tool(name = "backHomeMenu", description = "Back / Home / Menu / Recent")
        public Map<String, Object> backHomeMenu(String deviceName,
                @ToolParam(description = "back, home, menu, recent") String action) {
            try {
                operationService.backHomeMenu(deviceName, action);
                return Map.of(
                        "success", true,
                        "message", "成功执行Back / Home / Menu");
            } catch (Exception e) {
                log.error("执行Back / Home / Menu失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        /**
         * 输入键盘文字
         */
        // @Tool(name = "inputKeyboardText", description = "输入键盘文字")
        // public Map<String, Object> inputKeyboardText(String deviceName, String text)
        // {

        // return Map.of();
        // }

        /**
         * 获取设备屏幕截图
         */
        // @Tool(name = "takeScreenshot", description = "获取设备屏幕截图")
        // public Map<String, Object> takeScreenshot(String deviceName, Boolean
        // asBase64) {
        // if (deviceName == null || deviceName.isEmpty()) {
        // return Map.of(
        // "success", false,
        // "error", "设备名称不能为空");
        // }

        // asBase64 = asBase64 == null ? false : asBase64;
        // try {
        // if (asBase64) {
        // String base64Image = screenshotService.takeScreenshotAsBase64(deviceName);
        // log.info("成功获取设备屏幕截图(Base64): {}", deviceName);
        // return Map.of(
        // "success", true,
        // "imageBase64", base64Image);
        // } else {
        // String imagePath = screenshotService.takeAndSaveScreenshot(deviceName);
        // log.info("成功获取设备屏幕截图(文件): {}, 路径: {}", deviceName, imagePath);
        // return Map.of(
        // "success", true,
        // "imagePath", imagePath);
        // }
        // } catch (Exception e) {
        // log.error("获取屏幕截图失败: {}", deviceName, e);
        // return Map.of(
        // "success", false,
        // "error", e.getMessage());
        // }
        // }

        /**
         * 滑动屏幕
         */
        @Tool(name = "swipe", description = "滑动屏幕")
        public Map<String, Object> swipe(
                String deviceName,
                @ToolParam(description = "方向:up, down, left, right") String direction,
                Integer startX,
                Integer startY,
                Integer endX,
                Integer endY,
                Integer duration) {
            try {
                startX = startX == null ? 0 : startX;
                startY = startY == null ? 0 : startY;
                endX = endX == null ? 0 : endX;
                endY = endY == null ? 0 : endY;
                duration = duration == null ? 500 : duration;

                if (direction != null && !direction.isEmpty()) {
                    operationService.swipe(deviceName, direction);
                } else if (startX > 0 && startY > 0 && endX > 0 && endY > 0) {
                    operationService.swipe(deviceName, startX, startY, endX, endY, duration);
                } else {
                    return Map.of(
                            "success", false,
                            "error", "必须指定方向或坐标");
                }
                return Map.of(
                        "success", true,
                        "message", "成功滑动屏幕");
            } catch (Exception e) {
                log.error("滑动屏幕失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        // /**
        // * 滑动屏幕（兼容原方法）
        // */
        // @Tool(name = "swipeScreen", description = "滑动屏幕")
        // public Map<String, Object> swipeScreen(
        // String deviceName,
        // @ToolParam(description="滑动方向:") String direction,
        // Integer startX,
        // Integer startY,
        // Integer endX,
        // Integer endY,
        // Integer duration) {
        // return swipe(deviceName, direction, startX, startY, endX, endY, duration);
        // }

        /**
         * 获取所有可见元素,需要传入目的
         * 
         * @param deviceName
         * @param purpose    目的
         * @param useLLM     是否使用大模型进行判断，如果不使用大模型，本地可能无法对于图片的含义进行判断
         * @return
         */
        @Tool(name = "getAllVisibleElements", description = "获取可见元素，通过视觉识别获取元素的坐标，需要传入目的")
        public List<Map<String, Object>> getAllVisibleElements(String deviceName, String purpose,
                @ToolParam(description = "是否使用大模型进行判断，如果不使用大模型，本地可能无法对于图片的含义进行判断") Boolean useLLM) {
            try {
                return operationService.getAllVisibleElements(deviceName, purpose, useLLM != null ? useLLM : true);
            } catch (Exception e) {
                log.error("获取所有可见元素失败: {}", deviceName, e);
                return List.of();
            }
        }

        /**
         * 按坐标点击屏幕
         */
        @Tool(name = "tapByCoordinates", description = "按坐标点击屏幕")
        public Map<String, Object> tapByCoordinates(String deviceName, Integer x, Integer y) {
            try {
                if (x == null || y == null) {
                    return Map.of(
                            "success", false,
                            "error", "必须指定有效的x和y坐标");
                }

                operationService.tapByCoordinates(deviceName, x, y);
                return Map.of(
                        "success", true,
                        "message", "成功按坐标点击屏幕");
            } catch (Exception e) {
                log.error("按坐标点击屏幕失败: {}", deviceName, e);
                return Map.of(
                        "success", false,
                        "error", e.getMessage());
            }
        }

        // 创建定位器
        private By createLocator(String type, String value) {
            return switch (type.toLowerCase()) {
                case "id" -> By.id(value);
                case "xpath" -> By.xpath(value);
                case "css" -> By.cssSelector(value);
                case "class" -> By.className(value);
                case "name" -> By.name(value);
                case "tag" -> By.tagName(value);
                case "linktext" -> By.linkText(value);
                case "partiallinktext" -> By.partialLinkText(value);
                case "accessibility" -> By.xpath("//*[@content-desc='" + value + "']");
                default -> throw new IllegalArgumentException("不支持的定位器类型: " + type);
            };
        }
    }
}