package com.flyiu.ai.mcp.mobile.service.appium;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * 微信应用的Appium测试类，提供更多微信特定的操作
 */
public class WeChatTest extends BaseTest {
    
    // 微信相关元素定位
    private static final By SEARCH_BUTTON = By.id("com.tencent.mm:id/gsl"); // 搜索按钮ID (可能需要根据实际版本调整)
    private static final By SEARCH_INPUT = By.id("com.tencent.mm:id/cd7"); // 搜索输入框ID (可能需要根据实际版本调整)
    private static final By CHAT_TAB = By.xpath("//*[@text='聊天']");
    private static final By CONTACTS_TAB = By.xpath("//*[@text='通讯录']");
    private static final By DISCOVER_TAB = By.xpath("//*[@text='发现']");
    private static final By ME_TAB = By.xpath("//*[@text='我']");
    
    // 搜索结果和聊天相关元素
    private static final By SEARCH_RESULT_ITEMS = By.id("com.tencent.mm:id/p2"); // 搜索结果列表项 (可能需要根据实际版本调整)
    private static final By MESSAGE_INPUT = By.id("com.tencent.mm:id/dn8"); // 消息输入框 (可能需要根据实际版本调整)
    private static final By SEND_BUTTON = By.id("com.tencent.mm:id/dn9"); // 发送按钮 (可能需要根据实际版本调整)
    
    /**
     * 执行搜索操作
     * @param keyword 搜索关键词
     */
    public void search(String keyword) {
        try {
            // 确保微信已经打开
            if (!isElementPresent(CHAT_TAB) && !isElementPresent(DISCOVER_TAB)) {
                openWeChat();
            }
            
            // 查找并点击搜索按钮
            if (isElementPresent(SEARCH_BUTTON)) {
                driver.findElement(SEARCH_BUTTON).click();
                System.out.println("点击了搜索按钮");
                
                // 等待搜索输入框出现
                if (waitForElement(SEARCH_INPUT, 5)) {
                    // 输入搜索关键词
                    driver.findElement(SEARCH_INPUT).sendKeys(keyword);
                    System.out.println("输入了搜索关键词: " + keyword);
                    
                    // 可以在这里添加后续操作，比如点击搜索结果等
                } else {
                    System.out.println("未找到搜索输入框");
                }
            } else {
                System.out.println("未找到搜索按钮");
            }
        } catch (Exception e) {
            System.err.println("搜索操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 搜索用户并点击进入聊天
     * @param username 用户名
     * @return 是否成功进入聊天
     */
    public boolean searchAndEnterChat(String username) {
        try {
            // 先执行搜索
            search(username);
            
            // 等待搜索结果加载（等待2秒）
            Thread.sleep(2000);
            
            // 等待搜索结果出现
            if (waitForElement(SEARCH_RESULT_ITEMS, 10)) {
                // 获取所有搜索结果
                List<WebElement> resultItems = driver.findElements(SEARCH_RESULT_ITEMS);
                
                if (!resultItems.isEmpty()) {
                    // 点击第一个搜索结果（假设这是我们要找的用户）
                    resultItems.get(0).click();
                    System.out.println("点击了搜索结果: " + username);
                    
                    // 等待聊天页面加载
                    Thread.sleep(2000);
                    
                    // 验证是否进入了聊天页面（通过检查消息输入框是否存在）
                    if (waitForElement(MESSAGE_INPUT, 5)) {
                        System.out.println("成功进入与 " + username + " 的聊天");
                        return true;
                    } else {
                        System.out.println("未能进入聊天页面，找不到消息输入框");
                    }
                } else {
                    System.out.println("未找到与 " + username + " 匹配的搜索结果");
                }
            } else {
                System.out.println("搜索结果未出现");
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("搜索并进入聊天失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 发送消息
     * @param message 要发送的消息
     * @return 是否成功发送
     */
    public boolean sendMessage(String message) {
        try {
            // 确认是否在聊天页面（检查消息输入框是否存在）
            if (isElementPresent(MESSAGE_INPUT)) {
                // 点击输入框并输入消息
                WebElement inputBox = driver.findElement(MESSAGE_INPUT);
                inputBox.click();
                inputBox.sendKeys(message);
                System.out.println("输入了消息: " + message);
                
                // 点击发送按钮
                if (isElementPresent(SEND_BUTTON)) {
                    driver.findElement(SEND_BUTTON).click();
                    System.out.println("点击了发送按钮");
                    return true;
                } else {
                    System.out.println("未找到发送按钮");
                    // 尝试使用回车键发送
                    inputBox.sendKeys(Keys.ENTER);
                    System.out.println("尝试使用回车键发送");
                    return true;
                }
            } else {
                System.out.println("未找到消息输入框，可能不在聊天页面");
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 搜索特定用户并发送消息
     * @param username 用户名
     * @param message 要发送的消息
     */
    public void searchUserAndSendMessage(String username, String message) {
        try {
            // 确保微信已经打开
            if (!isElementPresent(CHAT_TAB) && !isElementPresent(DISCOVER_TAB)) {
                openWeChat();
                // 等待微信完全加载
                Thread.sleep(3000);
            }
            
            // 切换到聊天标签页
            switchToTab("聊天");
            Thread.sleep(1000);
            
            // 搜索并进入与用户的聊天
            if (searchAndEnterChat(username)) {
                // 发送消息
                if (sendMessage(message)) {
                    System.out.println("成功向 " + username + " 发送消息: " + message);
                    
                    // 等待一会，确保消息发送成功
                    Thread.sleep(2000);
                } else {
                    System.out.println("向 " + username + " 发送消息失败");
                }
            } else {
                System.out.println("找不到用户: " + username);
            }
        } catch (Exception e) {
            System.err.println("搜索用户并发送消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 切换到微信的指定标签页
     * @param tabName 标签页名称：聊天、通讯录、发现、我
     */
    public void switchToTab(String tabName) {
        try {
            By tabLocator;
            switch (tabName) {
                case "聊天":
                    tabLocator = CHAT_TAB;
                    break;
                case "通讯录":
                    tabLocator = CONTACTS_TAB;
                    break;
                case "发现":
                    tabLocator = DISCOVER_TAB;
                    break;
                case "我":
                    tabLocator = ME_TAB;
                    break;
                default:
                    System.out.println("不支持的标签页名称: " + tabName);
                    return;
            }
            
            if (isElementPresent(tabLocator)) {
                driver.findElement(tabLocator).click();
                System.out.println("已切换到" + tabName + "标签页");
            } else {
                System.out.println("未找到" + tabName + "标签页");
            }
        } catch (Exception e) {
            System.err.println("切换标签页失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试实例，演示打开微信，搜索cvl用户并发送"你好"消息
     */
    public static void main(String[] args) {
        WeChatTest test = new WeChatTest();
        try {
            test.setup();
            
            // 搜索cvl用户并发送"你好"消息
            test.searchUserAndSendMessage("cvl", "你好");
            
            // 等待一段时间，确保可以看到结果
            Thread.sleep(5000);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.tearDown();
        }
    }
} 