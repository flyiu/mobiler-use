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

    public static List<Map<String, Object>> getLocalElements(AppiumDriver driver) {
        List<WebElement> localelements = driver.findElements(By.className("android.widget.TextView"));
        List<WebElement> editTextElements = driver.findElements(By.className("android.widget.EditText"));
        List<WebElement> imageElements = driver.findElements(By.className("android.widget.ImageView"));

        List<Map<String, Object>> elements = new ArrayList<>();
        for (WebElement element : localelements) {
            try {
                Map<String, Object> elementMap = new HashMap<>();
                elementMap.put("text", element.getText());
                elementMap.put("type", "text");
                elementMap.put("bounds", element.getRect());
                elements.add(elementMap);
            } catch (Exception e) {
                // log.error("获取元素信息失败: {}", e.getMessage());
            }
        }
        for (WebElement element : editTextElements) {
            try {
                Map<String, Object> elementMap = new HashMap<>();
                elementMap.put("text", element.getText());
                elementMap.put("type", "editText");
                elementMap.put("bounds", element.getRect());
                elements.add(elementMap);
            } catch (Exception e) {
                // log.error("获取元素信息失败: {}", e.getMessage());
            }
        }
        for (WebElement element : imageElements) {
            try {
                Map<String, Object> elementMap = new HashMap<>();
                elementMap.put("text", element.getText());
                elementMap.put("type", "image");
                elementMap.put("bounds", element.getRect());
                elementMap.put("resourceId", element.getAttribute("resource-id"));
                elements.add(elementMap);
            } catch (Exception e) {
                // log.error("获取元素信息失败: {}", e.getMessage());
            }
        }
        return elements;
    }

    public static String getLocalString(AppiumDriver driver, String prompt) {
        List<WebElement> localelements = driver.findElements(By.className("android.widget.TextView"));
        List<WebElement> editTextElements = driver.findElements(By.className("android.widget.EditText"));
        List<WebElement> imageElements = driver.findElements(By.className("android.widget.ImageView"));

        String copilotPrompt = "";
        if (localelements.size() > 0) {
            copilotPrompt = "我通过本地识别，获取了当前文本的定位信息。你用做参考定位：\n";
        }

        copilotPrompt += getPrompt(localelements, 20);
        copilotPrompt += getPrompt(editTextElements, 20);
        copilotPrompt += getPrompt(imageElements, 50);
        if (copilotPrompt.length() > 0) {
            prompt += "\n\n" + copilotPrompt;
        }
        return prompt;
    }

}
