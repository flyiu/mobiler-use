package com.flyiu.ai.mcp.mobile.service.appium;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flyiu.ai.mcp.mobile.ai.AiService;
import com.flyiu.ai.mcp.mobile.model.DeviceSession;
import com.flyiu.ai.mcp.mobile.service.screenshot.RecordService;
import com.flyiu.ai.mcp.mobile.util.AndroidPageUtils;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Appium设备操作服务，封装常用操作
 */
@Slf4j
@Service
public class AppiumOperationService {

    private final AppiumSessionManager sessionManager;

    @Value("${spring.ai.openai.api-key:xxxxxxxxxxxx}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen-vl-plus}")
    private String model;

    @Autowired
    private RecordService recordService;

    @Autowired
    private AiService aiService;
    @Autowired
    public AppiumOperationService(AppiumSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 获取设备当前屏幕截图
     * 
     * @param deviceName 设备名称
     * @return 截图文件
     */
    public File takeScreenshot(String deviceName) {
        log.info("获取设备屏幕截图: {}", deviceName);
        return getDriverOrThrow(deviceName).getScreenshotAs(OutputType.FILE);
    }

    /**
     * 获取指定元素的屏幕截图
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     * @return 截图文件
     */
    public File takeElementScreenshot(String deviceName, By locator) {
        log.info("获取元素屏幕截图: {}, 定位器: {}", deviceName, locator);
        WebElement element = findElement(deviceName, locator);
        return element.getScreenshotAs(OutputType.FILE);
    }

    /**
     * 点击指定元素
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     */
    public void clickElement(String deviceName, By locator) {
        log.info("点击元素: {}, 定位器: {}", deviceName, locator);
        findElement(deviceName, locator).click();
    }

    /**
     * 在指定元素中输入文本
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     * @param text       要输入的文本
     */
    public void sendKeys(String deviceName, By locator, String text) {
        log.info("输入文本: {}, 定位器: {}, 文本: {}", deviceName, locator, text);
        findElement(deviceName, locator).sendKeys(text);
    }

    /**
     * sendkeys 直接获取当前输入框
     * 
     * @param deviceName 设备名称
     * @param text       要输入的文本
     */
    // FIXME: 输入键盘文字 需要优化
    public void sendKeysToCurrentInput(String deviceName, String text) {
        log.info("输入键盘文字: {}, 文本: {}", deviceName, text);
        AppiumDriver driver = getDriverOrThrow(deviceName);
        // 查询当前输入框
        WebElement currentInput = driver.findElement(By.className("android.widget.EditText"));
        currentInput.sendKeys(text);
    }

    /**
     * 按指定元素输入键盘文字
     * 
     * @param deviceName 设备名称
     * @param text       要输入的文本
     */
    // FIXME: 输入键盘文字 需要优化
    public void inputKeyboardText(String deviceName, String text) {
        log.info("输入键盘文字: {}, 文本: {}", deviceName, text);
        AppiumDriver driver = getDriverOrThrow(deviceName);
        AndroidDriver androidDriver = (AndroidDriver) driver;
        String inputs = text.replaceAll("\\s+", "");
        for (char c : inputs.toCharArray()) {
            log.info("输入键盘文字: {}, 字符: {}", deviceName, c);
            // 将字符转换为AndroidKey
            androidDriver.pressKey(new KeyEvent(AndroidKey.valueOf(String.valueOf(c))));
        }
    }

    /**
     * 清除指定元素中的文本
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     */
    public void clearElement(String deviceName, By locator) {
        log.info("清除元素文本: {}, 定位器: {}", deviceName, locator);
        findElement(deviceName, locator).clear();
    }

    /**
     * 获取元素文本
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     * @return 元素文本
     */
    public String getElementText(String deviceName, By locator) {
        log.info("获取元素文本: {}, 定位器: {}", deviceName, locator);
        return findElement(deviceName, locator).getText();
    }

    /**
     * 检查元素是否存在
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     * @return 是否存在
     */
    public boolean isElementPresent(String deviceName, By locator) {
        log.info("检查元素是否存在: {}, 定位器: {}", deviceName, locator);
        try {
            return !findElements(deviceName, locator).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 等待元素出现
     * 
     * @param deviceName       设备名称
     * @param locator          元素定位器
     * @param timeoutInSeconds 超时时间（秒）
     * @return 是否成功等到
     */
    public boolean waitForElement(String deviceName, By locator, int timeoutInSeconds) {
        log.info("等待元素出现: {}, 定位器: {}, 超时时间: {}秒", deviceName, locator, timeoutInSeconds);
        long endTime = System.currentTimeMillis() + (timeoutInSeconds * 1000L);
        while (System.currentTimeMillis() < endTime) {
            if (isElementPresent(deviceName, locator)) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Back / Home / Menu
     * 
     * @param deviceName 设备名称
     * @param action     操作类型，支持back（返回）、home（主页）、menu（菜单）、recent（最近任务）
     */
    public void backHomeMenu(String deviceName, String action) {
        log.info("执行Back / Home / Menu: {}, {}", deviceName, action);
        AppiumDriver driver = getDriverOrThrow(deviceName);

        if (driver instanceof AndroidDriver) {
            AndroidDriver androidDriver = (AndroidDriver) driver;

            switch (action.toLowerCase()) {
                case "back":
                    // 返回键
                    driver.navigate().back();
                    break;
                case "home":
                    // 主页键
                    androidDriver.pressKey(new KeyEvent(AndroidKey.HOME));
                    break;
                case "menu":
                    // 菜单键
                    androidDriver.pressKey(new KeyEvent(AndroidKey.MENU));
                    break;
                case "recent":
                    // 最近任务键（多任务键）
                    androidDriver.pressKey(new KeyEvent(AndroidKey.APP_SWITCH));
                    break;
                default:
                    log.warn("未知的操作类型: {}", action);
                    throw new IllegalArgumentException("不支持的操作: " + action + "，支持的操作: back, home, menu, recent");
            }
        } else {
            log.warn("当前设备驱动不是AndroidDriver，无法执行系统按键操作: {}", deviceName);
            throw new UnsupportedOperationException("当前设备不支持系统按键操作");
        }
    }

    /**
     * 获取所有可见元素通过截图识别
     * 
     * @param deviceName 设备名称
     * @param purpose    目的
     * @param useLLM     是否使用大模型进行判断，如果不使用大模型，本地可能无法对于图片的含义进行判断
     * @return 包含元素信息的列表，每个元素包含其类型、文本、属性等信息
     */
    public List<Map<String, Object>> getAllVisibleElements(String deviceName, String purpose, boolean useLLM) {
        log.info("获取所有可见元素通过截图识别: {}, 是否使用大模型: {}", deviceName, useLLM);
        List<Map<String, Object>> visibleElements = new ArrayList<>();

        try {
            // 获取屏幕截图
            File screenshotFile = takeScreenshot(deviceName);
            log.info("已获取屏幕截图: {}", screenshotFile.getAbsolutePath());

            // 获取设备屏幕尺寸
            AppiumDriver driver = getDriverOrThrow(deviceName);
            int screenWidth = driver.manage().window().getSize().getWidth();
            int screenHeight = driver.manage().window().getSize().getHeight();
            log.info("设备屏幕尺寸: {}x{}", screenWidth, screenHeight);

            // 如果不使用大模型，尝试使用本地UI元素识别方法
            if (!useLLM) {
                log.info("不使用大模型，尝试使用本地UI元素识别方法");
                // 这里可以添加本地UI元素识别的逻辑
                // 例如使用UIAutomator获取元素
                visibleElements = AndroidPageUtils.getLocalElements(driver);
                return visibleElements;
            }

            // 使用大模型识别UI元素
            // 将截图转换为Base64
            String base64Image = "";
            try {
                byte[] fileContent = Files.readAllBytes(screenshotFile.toPath());
                base64Image = Base64.getEncoder().encodeToString(fileContent);
                log.info("已将截图转换为Base64格式");
            } catch (Exception e) {
                log.error("转换截图为Base64失败: {}", e.getMessage());
                return visibleElements;
            }

            // 构建提示信息，要求提供更精确的坐标信息
            String prompt = "请分析这张手机屏幕截图，识别并列出所有可见的UI元素。\n" +
                    "屏幕尺寸为: " + screenWidth + "x" + screenHeight + "像素,如果你进行了缩放，请根据缩放比例进行计算。\n" +
                    "对于每个元素，请提供以下信息：\n" +
                    "1. 元素类型（按钮、文本框、图片等）\n ,图片和按钮如果文字为空，请返回的text中简单描述" +
                    "2. 元素文本内容（如果有）\n" +
                    "3. 元素的准确边界框坐标（x坐标, y坐标, 宽度, 高度），坐标是指元素左上角在屏幕上的位置\n" +
                    "4. 元素的中心点坐标（centerX, centerY），用于点击操作\n" +
                    "5. 元素是否可能是可交互的（可点击、可输入等）\n\n" +
                    "6. 本次操作的预期目的,要注意键盘和页面实际功能的区别，优先实际功能 \n" + purpose + "\n" +
                    "请以JSON格式返回，每个元素包含上述属性。格式示例：\n" +
                    "[{\"type\": \"按钮\", \"text\": \"确定\", \"bounds\": {\"x\": 100, \"y\": 200, \"width\": 80, \"height\": 40}, \"center\": {\"x\": 140, \"y\": 220}, \"interactive\": true}]";

            prompt = AndroidPageUtils.getLocalString(driver, prompt);

            System.out.println(prompt);

            // 使用Spring AI的ChatModel发送请求
            try {
                // 由于Spring AI当前版本限制，使用文本方式发送请求


                String result = aiService.imageRecognition(base64Image, prompt, ResponseFormat.JSON);
                log.info("大模型分析结果: {}", result);


                // List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
                // contents.add(TextContent.from(prompt + "\n\n注意：这个Base64字符串代表了一张截图，请分析其中的UI元素。"));
                // contents.add(ImageContent.from("data:image/png;base64," + base64Image));
                // ChatMessage userMessage = UserMessage.from(contents);

                // // OpenAiApi openAiApi =
                // // OpenAiApi.builder().apiKey(apiKey).baseUrl(baseUrl).build();
                // OpenAiChatModel chatModel = OpenAiChatModel.builder().apiKey(apiKey).baseUrl(baseUrl).modelName(model)
                //         .build();

                // ChatRequest chatRequest = ChatRequest.builder().responseFormat(ResponseFormat.JSON)
                //         .messages(List.of(userMessage)).build();
                // ChatResponse response = chatModel.chat(chatRequest);
                // log.info("大模型分析结果: {}", response.aiMessage().text());

                // 解析响应数据
                try {
                    // 提取JSON部分，可能需要根据实际响应格式调整
                    String responseText = extractJsonFromText(result);

                    // 将JSON转换为对象
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> elements = mapper.readValue(responseText,
                            new TypeReference<List<Map<String, Object>>>() {
                            });

                    // 处理每个元素，转换为与原方法返回格式兼容的格式
                    int id = 0;
                    for (Map<String, Object> element : elements) {
                        Map<String, Object> elementInfo = new HashMap<>();

                        // 设置基本信息
                        elementInfo.put("text", element.getOrDefault("text", ""));
                        elementInfo.put("tagName", element.getOrDefault("type", ""));
                        elementInfo.put("className", element.getOrDefault("type", ""));
                        // elementInfo.put("resourceId", "ai_detected_" + id);
                        elementInfo.put("contentDesc", element.getOrDefault("description", ""));

                        // 获取元素边界框和中心点坐标
                        Map<String, Object> bounds = null;
                        Map<String, Object> center = null;

                        // 优先使用模型返回的精确坐标
                        if (element.containsKey("bounds")) {
                            bounds = (Map<String, Object>) element.get("bounds");
                        }

                        if (element.containsKey("center")) {
                            center = (Map<String, Object>) element.get("center");
                        }

                        // 如果没有精确坐标，使用替代方法
                        if (bounds == null) {
                            bounds = new HashMap<>();
                            // 使用字符串位置描述来估计坐标
                            String position = (String) element.getOrDefault("position", "center");

                            int x = screenWidth / 2;
                            int y = screenHeight / 2;
                            int width = 100;
                            int height = 50;

                            // 根据位置描述调整坐标
                            if (position.contains("左上")) {
                                x = screenWidth / 4;
                                y = screenHeight / 4;
                            } else if (position.contains("右上")) {
                                x = screenWidth * 3 / 4;
                                y = screenHeight / 4;
                            } else if (position.contains("左下")) {
                                x = screenWidth / 4;
                                y = screenHeight * 3 / 4;
                            } else if (position.contains("右下")) {
                                x = screenWidth * 3 / 4;
                                y = screenHeight * 3 / 4;
                            } else if (position.contains("左")) {
                                x = screenWidth / 4;
                            } else if (position.contains("右")) {
                                x = screenWidth * 3 / 4;
                            } else if (position.contains("上")) {
                                y = screenHeight / 4;
                            } else if (position.contains("下")) {
                                y = screenHeight * 3 / 4;
                            }

                            bounds.put("x", x - width / 2);
                            bounds.put("y", y - height / 2);
                            bounds.put("width", width);
                            bounds.put("height", height);
                        }

                        // 如果没有中心点坐标，则根据边界框计算
                        if (center == null && bounds != null) {
                            center = new HashMap<>();
                            int boundX = ((Number) bounds.get("x")).intValue();
                            int boundY = ((Number) bounds.get("y")).intValue();
                            int boundWidth = ((Number) bounds.get("width")).intValue();
                            int boundHeight = ((Number) bounds.get("height")).intValue();

                            center.put("x", boundX + boundWidth / 2);
                            center.put("y", boundY + boundHeight / 2);
                        }

                        // 设置位置信息
                        Map<String, Integer> locationMap = new HashMap<>();
                        locationMap.put("x", ((Number) bounds.get("x")).intValue());
                        locationMap.put("y", ((Number) bounds.get("y")).intValue());
                        elementInfo.put("location", locationMap);

                        // 设置中心点（用于点击）
                        Map<String, Integer> centerMap = new HashMap<>();
                        centerMap.put("x", ((Number) center.get("x")).intValue());
                        centerMap.put("y", ((Number) center.get("y")).intValue());
                        elementInfo.put("center", centerMap);

                        // 设置大小信息
                        Map<String, Integer> sizeMap = new HashMap<>();
                        sizeMap.put("width", ((Number) bounds.get("width")).intValue());
                        sizeMap.put("height", ((Number) bounds.get("height")).intValue());
                        elementInfo.put("size", sizeMap);

                        // 设置元素状态
                        boolean interactive = Boolean.TRUE.equals(element.getOrDefault("interactive", false));
                        elementInfo.put("enabled", interactive);
                        // elementInfo.put("selected", false);
                        // elementInfo.put("displayed", true);

                        // 设置唯一ID
                        // elementInfo.put("elementId", id++);

                        visibleElements.add(elementInfo);
                    }

                    log.info("成功解析大模型识别的 {} 个元素", visibleElements.size());
                } catch (Exception e) {
                    log.error("解析大模型返回数据失败: {}", e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                log.error("请求大模型分析截图失败: {}", e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            log.error("获取设备截图失败: {}", e.getMessage());
            e.printStackTrace();
        }

        return visibleElements;
    }

    /**
     * 获取所有可见元素通过截图识别 (兼容旧接口)
     * 
     * @param deviceName 设备名称
     * @param purpose    目的
     * @return 包含元素信息的列表，每个元素包含其类型、文本、属性等信息
     */
    public List<Map<String, Object>> getAllVisibleElements(String deviceName, String purpose) {
        // 默认使用大模型
        return getAllVisibleElements(deviceName, purpose, true);
    }

    /**
     * 从文本中提取JSON部分
     */
    private String extractJsonFromText(String text) {
        // 尝试找到JSON开始和结束的位置
        int startIdx = text.indexOf('[');
        int endIdx = text.lastIndexOf(']') + 1;

        if (startIdx >= 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx);
        }

        // 尝试查找JSON对象
        startIdx = text.indexOf('{');
        endIdx = text.lastIndexOf('}') + 1;

        if (startIdx >= 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx);
        }

        // 如果没有找到JSON格式，返回原文本
        log.warn("未能从文本中提取JSON格式，返回原始文本");
        return text;
    }

    /**
     * 获取指定条件的所有元素
     * 
     * @param deviceName 设备名称
     * @param locator    元素定位器
     * @return 包含元素信息的列表，每个元素包含其类型、文本、属性等信息
     */
    public List<Map<String, Object>> getElements(String deviceName, By locator) {
        log.info("获取指定条件的所有元素: {}, 定位器: {}", deviceName, locator);
        List<WebElement> elements = findElements(deviceName, locator);

        AppiumDriver driver = getDriverOrThrow(deviceName);

        // 将WebElement转换为包含元素属性的Map列表
        return elements.stream()
                .map(element -> {
                    Map<String, Object> elementInfo = new HashMap<>();
                    try {
                        // 检查元素是否真的可见
                        boolean isDisplayed = false;
                        try {
                            isDisplayed = element.isDisplayed();
                        } catch (Exception e) {
                            log.debug("检查元素可见性失败: {}", e.getMessage());
                        }

                        // 获取元素位置和大小
                        org.openqa.selenium.Point location = element.getLocation();
                        org.openqa.selenium.Dimension size = element.getSize();

                        // 只处理有尺寸的元素
                        if (size.getWidth() <= 0 || size.getHeight() <= 0) {
                            return null;
                        }

                        // 获取元素基本信息
                        String text = element.getText();
                        elementInfo.put("text", text != null && !text.isEmpty() ? text : "");

                        String tagName = element.getTagName();
                        elementInfo.put("tagName", tagName != null ? tagName : "");

                        String className = element.getAttribute("className");
                        elementInfo.put("className", className != null ? className : "");

                        String resourceId = element.getAttribute("resource-id");
                        elementInfo.put("resourceId", resourceId != null ? resourceId : "");

                        String contentDesc = element.getAttribute("content-desc");
                        elementInfo.put("contentDesc", contentDesc != null ? contentDesc : "");

                        // 添加位置和大小
                        Map<String, Integer> locationMap = new HashMap<>();
                        locationMap.put("x", location.getX());
                        locationMap.put("y", location.getY());
                        elementInfo.put("location", locationMap);

                        Map<String, Integer> sizeMap = new HashMap<>();
                        sizeMap.put("width", size.getWidth());
                        sizeMap.put("height", size.getHeight());
                        elementInfo.put("size", sizeMap);

                        // 获取元素状态
                        elementInfo.put("displayed", isDisplayed);
                        elementInfo.put("enabled", element.isEnabled());
                        elementInfo.put("selected", element.isSelected());

                        // 尝试获取元素的XPath
                        try {
                            String xpath = generateXPath(driver, element);
                            if (xpath != null && !xpath.isEmpty()) {
                                elementInfo.put("xpath", xpath);
                            }
                        } catch (Exception e) {
                            log.debug("生成元素XPath失败: {}", e.getMessage());
                        }

                        // 添加唯一ID以便标识元素
                        elementInfo.put("elementId", System.identityHashCode(element));
                    } catch (Exception e) {
                        log.warn("获取元素属性时出错: {}", e.getMessage());
                        return null;
                    }
                    return elementInfo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 生成元素的XPath路径
     * 
     * @param driver  Appium驱动
     * @param element 目标元素
     * @return 元素的XPath路径
     */
    private String generateXPath(AppiumDriver driver, WebElement element) {
        // 这里使用JavaScript执行器来获取元素的XPath
        return (String) driver.executeScript(
                "function getPathTo(element) {" +
                        "    if (element.id !== '')" +
                        "        return '//*[@id=\"' + element.id + '\"]';" +
                        "    if (element === document.body)" +
                        "        return '/html/body';" +
                        "    var index = 1;" +
                        "    var siblings = element.parentNode.childNodes;" +
                        "    for (var i = 0; i < siblings.length; i++) {" +
                        "        var sibling = siblings[i];" +
                        "        if (sibling === element)" +
                        "            return getPathTo(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + index + ']';"
                        +
                        "        if (sibling.nodeType === 1 && sibling.tagName === element.tagName)" +
                        "            index++;" +
                        "    }" +
                        "}" +
                        "return getPathTo(arguments[0]);",
                element);
    }

    /**
     * 获取页面源码
     * 
     * @param deviceName 设备名称
     * @return 页面源码
     */
    public String getPageSource(String deviceName) {
        log.info("获取页面源码: {}", deviceName);
        return getDriverOrThrow(deviceName).getPageSource();
    }

    /**
     * 启动应用程序
     * 
     * @param deviceName 设备名称
     * @param appPackage 应用包名
     * @param appName    应用名
     */
    public void launchApp(String deviceName, String appPackage, String appName) {
        log.info("启动应用: {}, 包名: {}, 应用名: {}", deviceName, appPackage, appName);
        AppiumDriver driver = getDriverOrThrow(deviceName);
        try {
            AndroidDriver androidDriver = (AndroidDriver) driver;
            androidDriver.activateApp(appPackage);
            System.out.println("通过activateApp方法启动应用: " + appPackage);
        } catch (Exception e) {
            System.err.println("启动应用失败: " + e.getMessage());

            log.info("尝试通过应用名称搜索并启动: {}", appName);
            try {
                AndroidDriver androidDriver = (AndroidDriver) driver;

                // 按Home键回到主屏幕
                backHomeMenu(deviceName, "home");
                Thread.sleep(500);

                // 在某些设备上可能需要打开应用抽屉
                swipe(deviceName, "down");
                Thread.sleep(500);

                log.info("没有找到应用图标，尝试在搜索框中搜索");
                // 输入应用名称
                WebElement searchInput = androidDriver.findElement(By.className("android.widget.EditText"));

                if (searchInput != null) {
                    searchInput.click();
                    Thread.sleep(500);
                    // 清空搜索框
                    searchInput.clear();

                    searchInput.sendKeys(appName);
                    Thread.sleep(1000);
                    // 点击搜索结果:预期是通过文字定位数组第一个,如果是英文需要忽视大小写
                    List<WebElement> searchResults = androidDriver.findElements(By.xpath(
                            "//*[@text='" + appName + "' or @content-desc='" + appName + "']"));

                    if (!searchResults.isEmpty() && searchResults.size() > 1) {
                        searchResults.get(1).click();
                    } else {
                        log.error("搜索结果中未找到应用: {}", appName);
                        // 直接敲enter
                        androidDriver.pressKey(new KeyEvent(AndroidKey.ENTER));
                    }
                } else {
                    // 如果找不到搜索框，尝试使用adb命令启动
                    log.info("尝试使用adb命令启动应用: {}", appPackage);
                    String command = "monkey -p " + appPackage + " -c android.intent.category.LAUNCHER 1";
                    driver.executeScript("mobile: shell", Map.of("command", command));
                }
            } catch (Exception ex) {
                log.error("所有启动应用方法均失败: {}", ex.getMessage());
                throw new RuntimeException("无法启动应用: " + appName + ", " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 滑动屏幕
     * 
     * @param deviceName 设备名称
     * @param startX     起始X坐标
     * @param startY     起始Y坐标
     * @param endX       结束X坐标
     * @param endY       结束Y坐标
     * @param duration   持续时间（毫秒）
     */
    public void swipe(String deviceName, int startX, int startY, int endX, int endY, int duration) {
        log.info("滑动屏幕: {}, 从({},{})到({},{}),方向: {}, 持续时间: {}毫秒",
                deviceName, startX, startY, endX, endY, getSwipeDirection(startX, startY, endX, endY), duration);

        AppiumDriver driver = getDriverOrThrow(deviceName);
        driver.executeScript("mobile: swipeGesture",
                Map.of(
                        "left", startX,
                        "top", startY,
                        "width", endX - startX,
                        "height", endY - startY,
                        "direction", getSwipeDirection(startX, startY, endX, endY),
                        "percent", 0.75,
                        "speed", duration));
    }

    /**
     * 向指定方向滑动
     * 
     * @param deviceName 设备名称
     * @param direction  方向（up, down, left, right）
     */
    public void swipe(String deviceName, String direction) {
        log.info("向{}方向滑动: {}", direction, deviceName);

        AppiumDriver driver = getDriverOrThrow(deviceName);

        driver.executeScript("mobile: swipeGesture",
                Map.of(
                        "left", 100,
                        "top", 200,
                        "width", 200,
                        "height", 400,
                        "direction", direction,
                        "percent", 0.75));
    }

    /**
     * 按坐标点击屏幕
     * 
     * @param deviceName 设备名称
     * @param x          X坐标
     * @param y          Y坐标
     */
    public void tapByCoordinates(String deviceName, int x, int y) {
        log.info("按坐标点击屏幕: {}, 坐标: ({},{})", deviceName, x, y);

        AppiumDriver driver = getDriverOrThrow(deviceName);
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    // 查找单个元素
    private WebElement findElement(String deviceName, By locator) {
        return getDriverOrThrow(deviceName).findElement(locator);
    }

    // 查找多个元素
    private List<WebElement> findElements(String deviceName, By locator) {
        return getDriverOrThrow(deviceName).findElements(locator);
    }

    // 获取驱动或抛出异常
    private AppiumDriver getDriverOrThrow(String deviceName) {
        return sessionManager.getSession(deviceName)
                .orElseThrow(() -> new RuntimeException("设备未连接或会话不存在: " + deviceName))
                .getDriver();
    }

    // 计算滑动方向
    private String getSwipeDirection(int startX, int startY, int endX, int endY) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            return deltaX > 0 ? "right" : "left";
        } else {
            return deltaY > 0 ? "down" : "up";
        }
    }

    /**
     * 通过元素ID点击屏幕上的元素
     * 
     * @param deviceName 设备名称
     * @param elementId  元素ID
     * @return 是否成功点击
     */
    public boolean clickElementById(String deviceName, int elementId) {
        log.info("通过ID点击元素: {}, elementId: {}", deviceName, elementId);
        try {
            List<Map<String, Object>> elements = getAllVisibleElements(deviceName, "");

            for (Map<String, Object> element : elements) {
                int id = (Integer) element.get("elementId");
                if (id == elementId) {
                    Map<String, Integer> center = (Map<String, Integer>) element.get("center");
                    int x = center.get("x");
                    int y = center.get("y");

                    log.info("找到元素ID: {}，点击坐标: ({}, {})", elementId, x, y);
                    tapByCoordinates(deviceName, x, y);
                    return true;
                }
            }

            log.error("未找到元素ID: {}", elementId);
            return false;
        } catch (Exception e) {
            log.error("通过ID点击元素失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过多次按返回键退出应用，然后重新启动应用
     * 
     * @param deviceName   设备名称
     * @param appPackage   应用包名
     * @param appName      应用名
     * @param maxBackCount 最大返回次数
     */
    public void restartApp(String deviceName, String appPackage, String appName, int maxBackCount) {
        log.info("通过返回键退出并重启应用: {}, 包名: {}, 应用名: {}, 最大返回次数: {}", deviceName, appPackage, appName, maxBackCount);
        AppiumDriver driver = getDriverOrThrow(deviceName);

        recordService.startRecording(deviceName, 600);

        // 先尝试通过多次返回键退出应用
        for (int i = 0; i < maxBackCount; i++) {
            try {
                log.info("执行返回操作 {}/{}", i + 1, maxBackCount);
                backHomeMenu(deviceName, "back");
                // 每次返回后等待一小段时间
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("执行返回操作失败: {}", e.getMessage());
                break;
            }
        }

        // 确保应用完全退出，按Home键回到主屏幕
        try {
            backHomeMenu(deviceName, "home");
            Thread.sleep(1000);
        } catch (Exception e) {
            log.warn("执行Home键操作失败: {}", e.getMessage());
        }
        // 重新启动应用
        launchApp(deviceName, appPackage, appName);
    }

    /**
     * 获取指定设备名称的Driver
     * 用于录屏等需要直接访问Driver的场景
     * 
     * @param deviceName 设备名称
     * @return AppiumDriver实例
     */
    public AppiumDriver getDriverByName(String deviceName) {
        return getDriverOrThrow(deviceName);
    }
}