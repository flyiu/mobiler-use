package com.flyiu.ai.mcp.mobile.service.appium;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Appium基础测试类，提供打开微信等基本功能
 */
public class BaseTes2 {

    protected AppiumDriver driver;
    private static final String WECHAT_PACKAGE = "com.tencent.mm";
    private static final String WECHAT_LAUNCH_ACTIVITY = "com.tencent.mm.ui.LauncherUI";

    /**
     * 初始化Appium驱动
     */
    public void setup() throws MalformedURLException {
        // 使用W3C标准的UiAutomator2Options替代DesiredCapabilities
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setNoReset(true); // 不重置应用状态
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        
        // 如果有特定设备，可以取消下面两行的注释并填入您的设备信息
        // options.setUdid("设备的UDID");
        // options.setDeviceName("您的设备名称");
        
        // 创建AndroidDriver
        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        
        // 尝试解锁设备
        unlockDevice();
    }

    /**
     * 尝试解锁设备
     */
    protected void unlockDevice() {
        try {
            // 检查设备是否已解锁
            boolean isLocked = (Boolean) ((AndroidDriver) driver).executeScript("mobile: isDeviceLocked");
            
            if (isLocked) {
                System.out.println("设备当前已锁定，尝试解锁...");
                
                // 唤醒设备
                ((AndroidDriver) driver).executeScript("mobile: pressKey", Map.of("keycode", 26)); // 电源键
                Thread.sleep(1000);
                
                // 上滑解锁屏幕
                Map<String, Object> args = new HashMap<>();
                args.put("direction", "up");
                args.put("percent", 0.5);
                ((AndroidDriver) driver).executeScript("mobile: swipeGesture", args);
                Thread.sleep(1000);
                
                // 这里可以添加输入解锁密码的代码，如果设备有密码锁
                // 例如：driver.findElement(By.id("密码输入框ID")).sendKeys("1234");
                // driver.findElement(By.id("确认按钮ID")).click();
                
                System.out.println("设备解锁操作完成");
            } else {
                System.out.println("设备已处于解锁状态");
            }
        } catch (Exception e) {
            System.err.println("尝试解锁设备时出错: " + e.getMessage());
            // 不抛出异常，继续执行后续操作
        }
    }

    /**
     * 打开微信应用
     */
    public void openWeChat() {
        try {
            // 启动微信
            startApp(WECHAT_PACKAGE, WECHAT_LAUNCH_ACTIVITY);
            System.out.println("微信已成功启动");
            
            // 等待微信加载完成，等待5秒
            Thread.sleep(5000);
            
            // 检查是否有"发现"或"我"等标签存在
            boolean isWeChatOpen = isElementPresent(By.xpath("//*[@text='发现']"))
                    || isElementPresent(By.xpath("//*[@text='我']"));
            
            if (isWeChatOpen) {
                System.out.println("成功验证微信已打开");
                
                // 截图（可选）
                File screenshot = driver.getScreenshotAs(OutputType.FILE);
                System.out.println("截图已保存到: " + screenshot.getAbsolutePath());
            } else {
                System.out.println("微信启动可能失败，未找到预期的界面元素");
            }
            
        } catch (Exception e) {
            System.err.println("打开微信测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 启动指定的应用
     */
    protected void startApp(String appPackage, String appActivity) {
        try {
            // 直接使用activateApp方法启动应用
            ((AndroidDriver) driver).activateApp(appPackage);
            System.out.println("通过activateApp方法启动应用: " + appPackage);
        } catch (Exception e) {
            System.err.println("启动应用失败: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试使用更原始的shell命令启动
            try {
                String command = String.format("am start -n %s/%s", appPackage, appActivity);
                ((AndroidDriver) driver).executeScript("mobile: shell", Map.of("command", command));
                System.out.println("通过shell命令启动应用: " + command);
            } catch (Exception e2) {
                System.err.println("备用方法启动失败: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }
    
    /**
     * 检查元素是否存在
     */
    protected boolean isElementPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 等待元素出现
     */
    protected boolean waitForElement(By locator, int timeoutInSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutInSeconds * 1000L);
        while (System.currentTimeMillis() < endTime) {
            if (isElementPresent(locator)) {
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
     * 关闭驱动
     */
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * 主方法，用于直接运行测试
     */
    public static void main(String[] args) {
        BaseTest test = new BaseTest();
        try {
            test.setup();
            test.openWeChat();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.tearDown();
        }
    }
}
