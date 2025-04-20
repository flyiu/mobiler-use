# Appium 微信自动化测试

本项目提供了基于Appium的微信自动化测试功能。

## 前提条件

1. 已安装Appium服务器（推荐版本2.0或以上）
2. 已配置Android SDK和设备（真机或模拟器）
3. 已安装微信应用
4. 微信应用已经登录

## 设置Appium服务器

在运行测试前，请确保Appium服务器已启动：

```bash
appium
```

默认情况下，Appium服务器会在`http://127.0.0.1:4723`上运行。

## 测试类说明

### BaseTest

基础测试类，提供通用的Appium操作功能：

- 初始化Appium驱动
- 尝试解锁设备
- 打开微信应用
- 等待元素出现
- 检查元素是否存在
- 关闭驱动等

### WeChatTest

微信特定的测试类，提供微信应用的专门操作：

- 搜索功能
- 切换标签页（聊天、通讯录、发现、我）
- 搜索用户并进入聊天
- 发送消息

### WeChatCvlMessageTest

专门用于打开微信、搜索cvl用户并发送"你好"消息的测试类。

## 使用方法

### 运行特定测试：搜索cvl用户并发送"你好"消息

直接运行`WeChatCvlMessageTest`类的`main`方法即可，它会执行以下操作：

1. 启动微信应用
2. 搜索"cvl"用户
3. 进入聊天界面
4. 发送"你好"消息

### 自定义搜索用户和发送消息

您可以使用`WeChatTest`类的`searchUserAndSendMessage`方法来搜索任意用户并发送自定义消息：

```java
WeChatTest test = new WeChatTest();
try {
    test.setup();
    test.searchUserAndSendMessage("用户名", "您要发送的消息");
} catch (Exception e) {
    e.printStackTrace();
} finally {
    test.tearDown();
}
```

### 更多自定义测试

您可以扩展这些基础类来创建自己的测试用例：

```java
public class MyWeChatTest extends WeChatTest {
    
    public void myCustomTest() {
        // 打开微信
        openWeChat();
        
        // 执行自定义操作
        searchUserAndSendMessage("朋友1", "你好，这是自动发送的消息");
        searchUserAndSendMessage("朋友2", "你好，这是测试消息");
        
        // 其他操作...
    }
    
    public static void main(String[] args) {
        MyWeChatTest test = new MyWeChatTest();
        try {
            test.setup();
            test.myCustomTest();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            test.tearDown();
        }
    }
}
```

## 注意事项

1. 微信的元素ID可能随版本更新而变化，如果测试失败，可能需要更新元素定位器。
2. 初次运行时，确保微信已经登录，否则测试将不能正常进行。
3. 确保设备已连接，并在运行测试前已开启USB调试。
4. 如果设备有锁屏密码，可能需要在`unlockDevice`方法中添加解锁密码的代码。

## 常见问题解决

### W3C Capabilities 错误

如果遇到以下错误：
```
Caused by: java.lang.IllegalArgumentException: Illegal key values seen in w3c capabilities: [automationName, newCommandTimeout, noReset]
```

这是因为较新版本的Selenium和Appium使用W3C标准的capabilities格式。解决方法是使用特定平台的Options类而不是DesiredCapabilities：

```java
// 错误方式
DesiredCapabilities capabilities = new DesiredCapabilities();
capabilities.setCapability("platformName", "Android");
capabilities.setCapability("automationName", "UiAutomator2");

// 正确方式
UiAutomator2Options options = new UiAutomator2Options();
options.setPlatformName("Android");
options.setAutomationName("UiAutomator2");
options.setNoReset(true);
options.setNewCommandTimeout(Duration.ofSeconds(300));
```

### 找不到元素

如果测试无法找到页面元素，可能是以下原因：
1. 微信版本更新导致元素ID变化
2. 元素尚未加载完成
3. 元素不在当前视图中

解决方法：
1. 使用Appium Inspector重新获取最新的元素定位信息
2. 增加等待时间
3. 添加滑动操作以显示元素

### 搜索用户失败

如果搜索用户并发送消息失败，可能是因为：

1. 搜索按钮或输入框的ID发生变化
2. 搜索结果列表项的ID发生变化
3. 用户不存在或不在联系人列表中

解决方法：

1. 使用Appium Inspector更新元素ID
2. 确保搜索的用户存在于您的联系人或最近聊天列表中
3. 在代码中增加等待时间，让微信有足够时间处理搜索请求

## 元素定位调试

如果需要更新元素定位器，可以使用Appium Inspector工具来定位元素：

1. 启动Appium Inspector
2. 连接到设备
3. 打开微信应用
4. 使用选择器找到需要操作的元素
5. 更新相应的定位器代码 