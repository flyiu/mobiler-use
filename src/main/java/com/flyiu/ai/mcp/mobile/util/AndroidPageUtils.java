package com.flyiu.ai.mcp.mobile.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.google.gson.Gson;

import io.appium.java_client.AppiumDriver;

public class AndroidPageUtils {

    public static Gson gson = new Gson();

    public static String getPrompt(List<WebElement> localelements, int length) {
        StringBuffer prompt = new StringBuffer();
        for (int i = 0; i < localelements.size(); i++) {
            try {
                WebElement element = localelements.get(i);

                // 根据元素类型处理
                String type = element.getAttribute("class");
                if (type.contains("android.widget.TextView")) {
                    prompt.append("\n {text: " + element.getText() + ", type: " + type + ", bounds: "
                            + gson.toJson(element.getRect()) + "}");
                } else if (type.contains("android.widget.EditText")) {
                    prompt.append("\n {text: " + element.getText() + ", type: " + type + ", bounds: "
                            + gson.toJson(element.getRect()) + "}");
                } else if (type.contains("android.widget.ImageView")) {
                    prompt.append("\n {text: " + element.getText() + ", type: " + type + ", bounds: "
                            + gson.toJson(element.getRect()) + ", resourceId: " + element.getAttribute("resource-id")
                            + "}");
                }
            } catch (Exception e) {
                // log.error("获取元素信息失败: {}", e.getMessage());
            }
            length--;
            if (length <= 0) {
                break;
            }
        }
        return prompt.toString();
    }

    private static List<Map<String, Object>> getLocalElementsByPageSource(AppiumDriver driver) {
        String pageSource = driver.getPageSource();
        List<Map<String, Object>> elements = new ArrayList<>();

        try {
            org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(pageSource)));

            // 常见Android UI元素类型列表
            String[][] elementTypes = {
                    { "android.widget.TextView", "text", "text" },
                    { "android.widget.EditText", "text", "editText" },
                    { "android.widget.ImageView", "content-desc", "image" },
                    { "android.widget.Button", "text", "button" },
                    { "android.widget.ImageButton", "content-desc", "imageButton" },
                    { "android.widget.CheckBox", "text", "checkbox" },
                    { "android.widget.RadioButton", "text", "radioButton" },
                    { "android.widget.Switch", "text", "switch" },
                    { "android.widget.ToggleButton", "text", "toggleButton" },
                    { "android.widget.Spinner", "text", "spinner" },
//                    { "android.widget.ProgressBar", "content-desc", "progressBar" },
                    { "android.widget.SeekBar", "content-desc", "seekBar" },
//                    { "android.widget.ListView", "content-desc", "listView" },
//                    { "android.widget.GridView", "content-desc", "gridView" },
                    { "android.widget.RecyclerView", "content-desc", "recyclerView" },
//                    { "android.widget.ScrollView", "content-desc", "scrollView" },
//                    { "android.widget.HorizontalScrollView", "content-desc", "horizontalScrollView" },
//                    { "android.widget.WebView", "content-desc", "webView" },
//                    { "android.view.View", "content-desc", "view" },
//                    { "android.view.ViewGroup", "content-desc", "viewGroup" }
            };

            // 处理所有元素类型
            for (String[] elementType : elementTypes) {
                String tagName = elementType[0];
                String textAttribute = elementType[1];
                String typeName = elementType[2];

                // 默认只有ImageView和ImageButton需要resourceId
                boolean needResourceId = tagName.contains("Image");
                processElements(doc, tagName, textAttribute, typeName, elements, needResourceId);
            }

            // 尝试获取根元素信息，例如活动名称
            try {
                org.w3c.dom.Element rootElement = doc.getDocumentElement();
                if (rootElement != null) {
                    Map<String, Object> rootInfo = new HashMap<>();
                    rootInfo.put("type", "rootElement");
                    String packageName = rootElement.getAttribute("package");
                    if (packageName != null && !packageName.isEmpty()) {
                        rootInfo.put("package", packageName);
                    }
                    String activityName = rootElement.getAttribute("activity");
                    if (activityName != null && !activityName.isEmpty()) {
                        rootInfo.put("activity", activityName);
                    }
                    if (!rootInfo.isEmpty()) {
                        elements.add(rootInfo);
                    }
                }
            } catch (Exception e) {
                // 忽略根元素处理错误
            }

        } catch (Exception e) {
            // 解析异常处理
            e.printStackTrace();
        }

        return elements;
    }

    // 处理特定类型的元素
    private static void processElements(org.w3c.dom.Document doc, String tagName, String textAttribute,
            String elementType, List<Map<String, Object>> elements) {
        processElements(doc, tagName, textAttribute, elementType, elements, false);
    }

    // 处理特定类型的元素，可选择是否添加resourceId
    private static void processElements(org.w3c.dom.Document doc, String tagName, String textAttribute,
            String elementType, List<Map<String, Object>> elements, boolean includeResourceId) {
        org.w3c.dom.NodeList nodeList = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodeList.item(i);
            Map<String, Object> elementMap = new HashMap<>();

            // 获取元素文本内容
            String text = element.getAttribute(textAttribute);
            if (text != null && !text.isEmpty()) {
                elementMap.put("text", text);
            }

            // 设置元素类型
            elementMap.put("type", elementType);

            // 解析位置信息
            String bounds = element.getAttribute("bounds");
            Map<String, Object> rect = parseBounds(bounds);
            elementMap.put("bounds", rect);

            // 添加常用属性 FIXME 根据情况未来可能会使用
//            addCommonAttributes(element, elementMap);

            // 如果需要，添加资源ID
            if (includeResourceId && element.hasAttribute("resource-id")) {
                String resourceId = element.getAttribute("resource-id");
                if (resourceId != null && !resourceId.isEmpty()) {
                    elementMap.put("resourceId", resourceId);
                }
            }

            // 计算中心点（用于点击）
            try {
                if (rect.containsKey("x") && rect.containsKey("y") &&
                        rect.containsKey("width") && rect.containsKey("height")) {
                    Map<String, Object> center = new HashMap<>();
                    int x = ((Number) rect.get("x")).intValue();
                    int y = ((Number) rect.get("y")).intValue();
                    int width = ((Number) rect.get("width")).intValue();
                    int height = ((Number) rect.get("height")).intValue();

                    center.put("x", x + width / 2);
                    center.put("y", y + height / 2);
                    elementMap.put("center", center);
                }
            } catch (Exception e) {
                // 忽略中心点计算错误
            }

            elements.add(elementMap);
        }
    }

    // 添加元素的常用属性
    private static void addCommonAttributes(org.w3c.dom.Element element, Map<String, Object> elementMap) {
        // 检查并添加常用属性
        String[] commonAttributes = {
                "clickable", "checkable", "checked", "enabled", "focusable", "focused",
                "scrollable", "long-clickable", "password", "selected", "visible"
        };

        for (String attr : commonAttributes) {
            String value = element.getAttribute(attr);
            if (value != null && !value.isEmpty()) {
                try {
                    // 尝试转换为布尔值
                    elementMap.put(attr, Boolean.parseBoolean(value));
                } catch (Exception e) {
                    // 如果转换失败，保留原始字符串
                    elementMap.put(attr, value);
                }
            }
        }

        // 包名和类名
        String packageName = element.getAttribute("package");
        if (packageName != null && !packageName.isEmpty()) {
            elementMap.put("package", packageName);
        }

        String className = element.getAttribute("class");
        if (className != null && !className.isEmpty()) {
            elementMap.put("className", className);
        }
    }

    // 解析bounds字符串为rect对象
    private static Map<String, Object> parseBounds(String bounds) {
        Map<String, Object> rect = new HashMap<>();
        try {
            // bounds格式通常为"[x,y][x2,y2]"
            if (bounds != null && !bounds.isEmpty()) {
                // 提取坐标值
                bounds = bounds.replace("][", ",").replace("[", "").replace("]", "");
                String[] coordinates = bounds.split(",");
                if (coordinates.length == 4) {
                    int x = Integer.parseInt(coordinates[0]);
                    int y = Integer.parseInt(coordinates[1]);
                    int x2 = Integer.parseInt(coordinates[2]);
                    int y2 = Integer.parseInt(coordinates[3]);

                    rect.put("x", x);
                    rect.put("y", y);
                    rect.put("width", x2 - x);
                    rect.put("height", y2 - y);
                }
            }
        } catch (Exception e) {
            // 解析异常处理
        }
        return rect;
    }

    public static List<Map<String, Object>> getLocalElements(AppiumDriver driver) {
        // 使用优化后的方法一次性获取所有元素
        return getLocalElementsByPageSource(driver);
    }

    public static String getLocalString(AppiumDriver driver, String prompt) {

        List<Map<String, Object>> elements = getLocalElementsByPageSource(driver);

        if (elements.size() > 0) {
            Gson gson = new Gson();
            prompt += "\n\n" + gson.toJson(elements);
        }

        return prompt;

        // List<WebElement> localelements =
        // driver.findElements(By.className("android.widget.TextView"));
        // List<WebElement> editTextElements =
        // driver.findElements(By.className("android.widget.EditText"));
        // List<WebElement> imageElements =
        // driver.findElements(By.className("android.widget.ImageView"));
        //
        // String copilotPrompt = "";
        // if (localelements.size() > 0) {
        // copilotPrompt = "我通过本地识别，获取了当前文本的定位信息。你用做参考定位：\n";
        // }
        //
        // copilotPrompt += getPrompt(localelements, 20);
        // copilotPrompt += getPrompt(editTextElements, 20);
        // copilotPrompt += getPrompt(imageElements, 50);
        // if (copilotPrompt.length() > 0) {
        // prompt += "\n\n" + copilotPrompt;
        // }
        // return prompt;
    }

}
