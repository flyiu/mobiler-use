package com.flyiu.ai.mcp.mobile.service.appium;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;

import java.io.File;

/**
 * 微信搜索测试类，继承自BaseTest，添加微信搜索CVL的功能
 */
public class WeChatSearchTest extends BaseTest {

    /**
     * 在微信中搜索指定关键词
     * @param keyword 要搜索的关键词
     */
    public void searchInWeChat(String keyword) {
        try {
            System.out.println("开始执行微信搜索 '" + keyword + "' 的测试...");
            
            // 确保微信已经打开
            openWeChat();
            
            System.out.println("等待微信主页面加载完成...");
            Thread.sleep(2000);
            
            // 点击右上角搜索按钮 (资源ID可能需要根据实际情况调整)
            // 首先尝试通过资源ID查找搜索按钮
            try {
                System.out.println("尝试点击搜索按钮...");
                // 备选的搜索按钮ID列表，以应对不同版本的微信
                String[] searchButtonIds = {
                    "com.tencent.mm:id/f_s",  // 原始测试中使用的ID
                    "com.tencent.mm:id/cd7",  // 另一个可能的ID
                    "com.tencent.mm:id/j3v",  // 其他可能的ID
                    "com.tencent.mm:id/search_bar", // 通用名称
                    "com.tencent.mm:id/search" // 通用名称
                };
                
                boolean buttonFound = false;
                for (String id : searchButtonIds) {
                    if (isElementPresent(By.id(id))) {
                        driver.findElement(By.id(id)).click();
                        System.out.println("成功点击搜索按钮 (ID: " + id + ")");
                        buttonFound = true;
                        break;
                    }
                }
                
                // 如果通过ID找不到，尝试通过描述或内容描述查找
                if (!buttonFound) {
                    if (isElementPresent(By.xpath("//*[@content-desc='搜索']"))) {
                        driver.findElement(By.xpath("//*[@content-desc='搜索']")).click();
                        System.out.println("通过content-desc找到并点击搜索按钮");
                    } else if (isElementPresent(By.xpath("//*[@text='搜索']"))) {
                        driver.findElement(By.xpath("//*[@text='搜索']")).click();
                        System.out.println("通过text找到并点击搜索按钮");
                    } else {
                        System.out.println("无法找到搜索按钮，请手动检查最新的微信界面元素");
                        // 截图以便分析
                        File screenshot = driver.getScreenshotAs(OutputType.FILE);
                        System.out.println("已保存当前界面截图: " + screenshot.getAbsolutePath());
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("点击搜索按钮失败: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // 等待搜索框出现
            System.out.println("等待搜索框出现...");
            Thread.sleep(2000);
            
            // 尝试在可能的搜索框元素中输入关键词
            try {
                String[] searchBoxIds = {
                    "com.tencent.mm:id/cd7",  // 原始测试中使用的ID
                    "com.tencent.mm:id/l36",  // 其他可能的ID
                    "com.tencent.mm:id/search_input", // 通用名称
                    "com.tencent.mm:id/edit_query" // 通用名称
                };
                
                boolean inputFound = false;
                for (String id : searchBoxIds) {
                    if (isElementPresent(By.id(id))) {
                        driver.findElement(By.id(id)).sendKeys(keyword);
                        System.out.println("成功在搜索框中输入: " + keyword);
                        inputFound = true;
                        break;
                    }
                }
                
                // 如果通过ID找不到搜索框，尝试通过其他方式
                if (!inputFound) {
                    if (isElementPresent(By.xpath("//*[@resource-id='com.tencent.mm:id/android:id/search_src_text']"))) {
                        driver.findElement(By.xpath("//*[@resource-id='com.tencent.mm:id/android:id/search_src_text']")).sendKeys(keyword);
                        System.out.println("通过复合ID找到并输入关键词");
                    } else if (isElementPresent(By.xpath("//android.widget.EditText"))) {
                        driver.findElement(By.xpath("//android.widget.EditText")).sendKeys(keyword);
                        System.out.println("通过EditText类型找到并输入关键词");
                    } else {
                        System.out.println("无法找到搜索输入框，请手动检查最新的微信界面元素");
                        // 截图以便分析
                        File screenshot = driver.getScreenshotAs(OutputType.FILE);
                        System.out.println("已保存当前界面截图: " + screenshot.getAbsolutePath());
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("输入搜索关键词失败: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // 等待搜索结果显示
            System.out.println("等待搜索结果加载...");
            Thread.sleep(3000);
            
            // 截图保存结果
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            System.out.println("搜索完成，截图已保存到: " + screenshot.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("执行微信搜索测试过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 主方法，用于直接运行测试
     */
    public static void main(String[] args) {
        WeChatSearchTest test = new WeChatSearchTest();
        try {
            System.out.println("启动微信搜索测试...");
            test.setup();
            test.searchInWeChat("cvl"); // 搜索关键词为"cvl"
            System.out.println("测试执行完成");
        } catch (Exception e) {
            System.err.println("测试过程中出现错误:");
            e.printStackTrace();
        } finally {
            test.tearDown();
        }
    }
} 