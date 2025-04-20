package com.flyiu.ai.mcp.mobile.controller;

import com.flyiu.ai.mcp.mobile.model.DeviceSession;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumOperationService;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumSessionManager;
import com.flyiu.ai.mcp.mobile.service.screenshot.ScreenshotService;
import com.flyiu.ai.mcp.mobile.service.screenshot.RecordService;
import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动设备控制器 - REST API接口
 */
@Slf4j
@Controller
@RequestMapping("/api/mobile")
public class MobileController {

    private final AppiumSessionManager sessionManager;
    private final AppiumOperationService operationService;
    private final ScreenshotService screenshotService;
    private final RecordService recordService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MobileController(
            AppiumSessionManager sessionManager,
            AppiumOperationService operationService,
            ScreenshotService screenshotService,
            RecordService recordService) {
        this.sessionManager = sessionManager;
        this.operationService = operationService;
        this.screenshotService = screenshotService;
        this.recordService = recordService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 连接Android设备
     */
    @PostMapping("/android/connect")
    @ResponseBody
    public Map<String, Object> connectAndroid(@RequestParam(required = false) String deviceName) {
        try {
            deviceName = (deviceName == null || deviceName.isEmpty()) ? "default-android" : deviceName;

            // 查找设备配置
            com.flyiu.ai.mcp.mobile.config.DeviceConfig.DeviceCapabilities deviceCaps = sessionManager
                    .findAndroidDevice(deviceName);
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
    @PostMapping("/ios/connect")
    @ResponseBody
    public Map<String, Object> connectIOS(@RequestParam(required = false) String deviceName) {
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
     * 断开设备连接
     */
    @PostMapping("/disconnect")
    @ResponseBody
    public Map<String, Object> disconnectDevice(@RequestParam String deviceName) {
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
     * 获取屏幕截图
     */
    @GetMapping(value = "/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public Map<String, Object> takeScreenshot(@RequestParam String deviceName,
            @RequestParam(required = false, defaultValue = "false") boolean asBase64) {
        try {
            if (asBase64) {
                String base64Image = screenshotService.takeScreenshotAsBase64(deviceName);
                return Map.of(
                        "success", true,
                        "imageBase64", base64Image);
            } else {
                String imagePath = screenshotService.takeAndSaveScreenshot(deviceName);
                return Map.of(
                        "success", true,
                        "imagePath", imagePath);
            }
        } catch (Exception e) {
            log.error("获取屏幕截图失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    /**
     * 点击元素
     */
    @PostMapping("/click")
    @ResponseBody
    public Map<String, Object> clickElement(@RequestParam String deviceName,
            @RequestParam String locatorType,
            @RequestParam String locatorValue) {
        try {
            By locator = createLocator(locatorType, locatorValue);
            operationService.clickElement(deviceName, locator);
            return Map.of(
                    "success", true,
                    "message", "成功点击元素");
        } catch (Exception e) {
            log.error("点击元素失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    /**
     * 输入文本
     */
    @PostMapping("/sendKeys")
    @ResponseBody
    public Map<String, Object> sendKeys(@RequestParam String deviceName,
            @RequestParam String locatorType,
            @RequestParam String locatorValue,
            @RequestParam String text) {
        try {
            By locator = createLocator(locatorType, locatorValue);
            operationService.sendKeys(deviceName, locator, text);
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
     * 等待元素
     */
    @PostMapping("/waitElement")
    @ResponseBody
    public Map<String, Object> waitElement(@RequestParam String deviceName,
            @RequestParam String locatorType,
            @RequestParam String locatorValue,
            @RequestParam(defaultValue = "10") int timeoutInSeconds) {
        try {
            By locator = createLocator(locatorType, locatorValue);
            boolean found = operationService.waitForElement(deviceName, locator, timeoutInSeconds);
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
     * 滑动屏幕
     */
    @PostMapping("/swipe")
    @ResponseBody
    public Map<String, Object> swipe(@RequestParam String deviceName,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false, defaultValue = "0") int startX,
            @RequestParam(required = false, defaultValue = "0") int startY,
            @RequestParam(required = false, defaultValue = "0") int endX,
            @RequestParam(required = false, defaultValue = "0") int endY,
            @RequestParam(required = false, defaultValue = "500") int duration) {
        try {
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

    /**
     * 启动应用
     */
    @PostMapping("/launchApp")
    @ResponseBody
    public Map<String, Object> launchApp(@RequestParam String deviceName,
            @RequestParam String appPackage,
            @RequestParam String appName) {
        try {
            operationService.launchApp(deviceName, appPackage, appName);
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
     * 按坐标点击屏幕
     */
    @PostMapping("/tapByCoordinates")
    @ResponseBody
    public Map<String, Object> tapByCoordinates(@RequestParam String deviceName,
            @RequestParam int x,
            @RequestParam int y) {
        try {
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

    /**
     * simpleTest
     */
    @PostMapping("/simpleTest")
    @ResponseBody
    public Map<String, Object> simpleTest(@RequestParam String deviceName) {

        // 连接设备
        connectAndroid(deviceName);

        // 启动应用
        // launchApp(deviceName, "com.tencent.mm",
        // "com.tencent.mm.ui.LauncherUI").get("");
        // 启动抖音 package:com.ss.android.ugc.aweme
        // 小红书 com.xingin.xhs
        // 飞书 com.ss.android.lark
        // launchApp(deviceName, "com.xingin.xhs",
        // "com.xingin.xhs.activity.MainActivity");

        launchApp(deviceName, "com.ss.android.lark", "com.ss.android.lark.base.SplashActivity");

        // 获取驱动
        AppiumDriver driver = sessionManager.getSession(deviceName).get().getDriver();

        // 点击搜索元素
        tapByCoordinates(deviceName, 1015, 194);

        tapByCoordinates(deviceName, 500, 194);

        AndroidDriver androidDriver = (AndroidDriver) driver;
        androidDriver.pressKey(new KeyEvent(AndroidKey.C));
        androidDriver.pressKey(new KeyEvent(AndroidKey.V));
        androidDriver.pressKey(new KeyEvent(AndroidKey.L));
        androidDriver.pressKey(new KeyEvent(AndroidKey.ENTER));

        // 输入键盘 cvl文字

        // [{\"type\": \"按钮\", \"text\": \"确定\", \"bounds\": {\"x\": 100, \"y\": 200,
        // \"width\": 80, \"height\": 40}, \"center\": {\"x\": 140, \"y\": 220},
        // \"interactive\": true}]
        // 一次性 获取 文本 按钮 图片 输入框 等元素
        List<WebElement> elements = driver.findElements(By.className("android.widget.TextView"));
        elements.addAll(driver.findElements(By.className("android.widget.Button")));
        elements.addAll(driver.findElements(By.className("android.widget.Image")));
        elements.addAll(driver.findElements(By.className("android.widget.EditText")));
        for (WebElement element : elements) {
            Map<String, Object> map = new HashMap<>();

            if (element.getAttribute("class").contains("android.widget.TextView")) {
                map.put("type", "文本");
            } else if (element.getAttribute("class").contains("android.widget.Button")) {
                map.put("type", "按钮");
            } else if (element.getAttribute("class").contains("android.widget.Image")) {
                map.put("type", "图片");
            } else if (element.getAttribute("class").contains("android.widget.EditText")) {
                map.put("type", "输入框");
            }
            map.put("text", element.getText());
            map.put("bounds", element.getRect());
            map.put("center", element.getLocation());
            map.put("interactive", true);
            System.out.println(new Gson().toJson(map));
        }

        // 所有文本信息
        operationService.getElements(deviceName, By.xpath("//*[]"));

        // 输入文本
        sendKeys(deviceName, "xpath", "//*[@text='搜索']", "微信");

        return Map.of(
                "success", true,
                "message", "成功执行simpleTest");
    }

    /**
     * simpleTest
     */
    @PostMapping("/simpleFeishuTest")
    @ResponseBody
    public Map<String, Object> simpleFeishuTest(@RequestParam String deviceName) {

        // 连接设备
        connectAndroid(deviceName);

        // 启动应用
        // launchApp(deviceName, "com.tencent.mm",
        // "com.tencent.mm.ui.LauncherUI").get("");
        // 启动抖音 package:com.ss.android.ugc.aweme
        // 小红书 com.xingin.xhs
        // 飞书 com.ss.android.lark
        // launchApp(deviceName, "com.xingin.xhs",
        // "com.xingin.xhs.activity.MainActivity");

        launchApp(deviceName, "com.ss.android.lark",
                "com.ss.android.lark.base.SplashActivity");

        // 获取驱动
        AppiumDriver driver = sessionManager.getSession(deviceName).get().getDriver();

        // 所有可见元素
        long startTime = System.currentTimeMillis();
        List<WebElement> allPotentialElements = driver.findElements(By.xpath("//*"));

        // List<WebElement> allPotentialElements =
        // driver.findElements(By.className("android.widget.TextView"));
        // allPotentialElements.addAll(driver.findElements(By.className("android.widget.EditText")));
        // allPotentialElements.addAll(driver.findElements(By.className("android.widget.ImageView")));

        long endTime = System.currentTimeMillis();
        log.info("所有可见元素: {}ms", endTime - startTime);
        log.info("所有可见元素: {}", allPotentialElements.size());
        Gson gson = new Gson();
        for (WebElement element : allPotentialElements) {
            // 根据类型 获取元素
            String text = "";
            String type = element.getAttribute("class");
            if (type.contains("android.widget.TextView")) {
                text += "\n {text: " + text + ", bounds: " + gson.toJson(element.getRect()) + "}";
            } else if (type.contains("android.widget.Button")) {
                text += "\n {text: " + "button" + ", bounds: " + gson.toJson(element.getRect()) + "}";
            } else if (type.contains("android.widget.Image")) {
                text += "\n {text: " + "button" + ", bounds: " + gson.toJson(element.getRect()) + "}";
            } else if (type.contains("android.widget.EditText")) {
                text += "\n {text: " + "button" + ", bounds: " + gson.toJson(element.getRect()) + "}";
            } else {
                // text += "\n {text: " + "button" + ", bounds: " +
                // gson.toJson(element.getRect()) + "}";
            }
        }
        // 分类处理时间
        long endTime2 = System.currentTimeMillis();
        log.info("处理时间: {}ms", endTime2 - endTime);

        // 点击搜索元素
        tapByCoordinates(deviceName, 942, 147);

        // tapByCoordinates(deviceName, 500, 194);

        AndroidDriver androidDriver = (AndroidDriver) driver;
        // androidDriver.pressKey(new KeyEvent(AndroidKey.C));
        // androidDriver.pressKey(new KeyEvent(AndroidKey.V));
        // androidDriver.pressKey(new KeyEvent(AndroidKey.L));
        // androidDriver.pressKey(new KeyEvent(AndroidKey.ENTER));

        // 输入键盘 cvl文字

        // [{\"type\": \"按钮\", \"text\": \"确定\", \"bounds\": {\"x\": 100, \"y\": 200,
        // \"width\": 80, \"height\": 40}, \"center\": {\"x\": 140, \"y\": 220},
        // \"interactive\": true}]
        // 一次性 获取 文本 按钮 图片 输入框 等元素
        // List<WebElement> elements =
        // driver.findElements(By.className("android.widget.TextView"));
        // elements.addAll(driver.findElements(By.className("android.widget.Button")));
        // elements.addAll(driver.findElements(By.className("android.widget.ImageView")));
        // elements.addAll(driver.findElements(By.className("android.widget.EditText")));
        // for (WebElement element : elements) {
        // Map<String, Object> map = new HashMap<>();
        //
        // if (element.getAttribute("class").contains("android.widget.TextView")) {
        // map.put("type", "文本");
        // } else if (element.getAttribute("class").contains("android.widget.Button")) {
        // map.put("type", "按钮");
        // } else if (element.getAttribute("class").contains("android.widget.Image")) {
        // map.put("type", "图片");
        // } else if (element.getAttribute("class").contains("android.widget.EditText"))
        // {
        // map.put("type", "输入框");
        // }
        // map.put("text", element.getText());
        // map.put("bounds", element.getRect());
        // map.put("center", element.getLocation());
        // map.put("interactive", true);
        // System.out.println(new Gson().toJson(map));
        // }
        //
        // // 所有文本信息
        // operationService.getElements(deviceName, By.xpath("//*[]"));
        //
        // // 输入文本
        // sendKeys(deviceName, "xpath", "//*[@text='搜索']", "微信");

        return Map.of(
                "success", true,
                "message", "成功执行simpleTest");
    }

    /**
     * 开始录制设备屏幕
     */
    @PostMapping("/record/start")
    @ResponseBody
    public Map<String, Object> startRecording(
            @RequestParam String deviceName,
            @RequestParam(required = false, defaultValue = "0") int durationSeconds) {
        try {
            String sessionId = recordService.startRecording(deviceName, durationSeconds);
            return Map.of(
                    "success", true,
                    "message", "已开始录制设备屏幕",
                    "sessionId", sessionId,
                    "deviceName", deviceName,
                    "durationSeconds", durationSeconds);
        } catch (Exception e) {
            log.error("开始录制失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    /**
     * 停止录制并保存视频
     */
    @PostMapping("/record/stop")
    @ResponseBody
    public Map<String, Object> stopRecording(@RequestParam String deviceName) {
        try {
            String videoPath = recordService.stopRecording(deviceName);
            if (videoPath == null) {
                return Map.of(
                        "success", false,
                        "message", "设备未在录制中");
            }
            return Map.of(
                    "success", true,
                    "message", "已停止录制并保存视频",
                    "videoPath", videoPath);
        } catch (Exception e) {
            log.error("停止录制失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    /**
     * 取消录制（停止录制但不保存视频）
     */
    @PostMapping("/record/cancel")
    @ResponseBody
    public Map<String, Object> cancelRecording(@RequestParam String deviceName) {
        try {
            recordService.cancelRecording(deviceName);
            return Map.of(
                    "success", true,
                    "message", "已取消录制");
        } catch (Exception e) {
            log.error("取消录制失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }

    /**
     * 获取设备录制状态
     */
    @GetMapping("/record/status")
    @ResponseBody
    public Map<String, Object> getRecordingStatus(@RequestParam String deviceName) {
        try {
            Map<String, Object> status = recordService.getRecordingStatus(deviceName);
            if (status == null) {
                return Map.of(
                        "success", true,
                        "isRecording", false,
                        "deviceName", deviceName);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isRecording", true);
            response.put("deviceName", deviceName);
            response.put("startTime", status.get("startTime"));
            response.put("duration", status.get("duration"));

            return response;
        } catch (Exception e) {
            log.error("获取录制状态失败: {}", deviceName, e);
            return Map.of(
                    "success", false,
                    "error", e.getMessage());
        }
    }
}