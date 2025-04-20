# MCP移动设备控制服务

基于Spring Boot和Appium的移动设备控制服务，支持通过REST API和Spring AI MCP协议操作移动设备。

## 主要功能

- 封装Appium常规操作，提供简洁API
- 支持设备屏幕截图
- 基于Spring AI的MCP协议，方便与AI模型交互
- 支持Android和iOS设备

## 环境要求

- JDK 21+
- Maven 3.6+
- Appium 2.0+
- Android SDK（操作Android设备时）
- XCode（操作iOS设备时）

## 快速开始

### 1. 准备Appium环境

确保Appium服务器已经安装并运行：

```bash
npm install -g appium
appium driver install uiautomator2  # Android驱动
appium driver install xcuitest      # iOS驱动
appium                              # 启动Appium服务器
```

### 2. 配置应用

首先，复制本地配置模板文件并进行个性化设置：

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

然后，编辑`application-local.properties`文件，填入你的API密钥和其他敏感配置：

```properties
# OpenAI API配置
spring.ai.openai.api-key=your-actual-api-key-here
spring.ai.openai.base-url=your-actual-base-url-here
spring.ai.openai.chat.options.model=your-actual-model-name-here
```

修改`src/main/resources/application.yml`配置Appium服务器和设备信息：

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: mobile-mcp-server
        version: 1.0.0
        type: SYNC   # 同步模式
        stdio: false # 是否支持标准输入/输出
        sse-message-endpoint: /api/mcp/message
        sse-endpoint: /api/mcp/sse

appium:
  server:
    url: http://127.0.0.1:4723  # Appium服务器地址
    
devices:
  android:
    - name: default-android      # 设备名称
      platform-name: Android
      automation-name: UiAutomator2
      udid: auto                 # 自动检测或指定设备ID
      # 更多配置...
```

### 3. 运行应用

```bash
mvn spring-boot:run
```

## API使用示例

### 连接设备

```
POST /api/mobile/android/connect
```

### 截取屏幕

```
GET /api/mobile/screenshot?deviceName=default-android
```

### 点击元素

```
POST /api/mobile/click
参数：
- deviceName: 设备名称
- locatorType: 定位器类型（id/xpath等）
- locatorValue: 定位器值
```

## Spring AI MCP协议使用

本服务基于Spring AI的新MCP协议实现，提供了一组工具（ToolCallable）用于与AI模型交互。

### MCP工具列表

| 工具名称 | 描述 | 参数 |
|---------|------|------|
| connectAndroidDevice | 连接Android设备 | deviceName (可选) |
| connectIOSDevice | 连接iOS设备 | deviceName (可选) |
| disconnectDevice | 断开设备连接 | deviceName |
| takeScreenshot | 获取设备屏幕截图 | deviceName, asBase64 (可选) |
| clickElement | 点击元素 | deviceName, locatorType, locatorValue |
| sendKeys | 在元素中输入文本 | deviceName, locatorType, locatorValue, text |
| waitForElement | 等待元素出现 | deviceName, locatorType, locatorValue, timeoutInSeconds (可选) |
| swipeScreen | 滑动屏幕 | deviceName, direction 或 startX/startY/endX/endY |
| launchApp | 启动应用 | deviceName, appPackage, appActivity |

### 使用示例

使用LLM工具调用接口时请求示例：

```json
{
  "name": "takeScreenshot",
  "parameters": {
    "deviceName": "default-android",
    "asBase64": true
  }
}
```

或使用MCP客户端调用:

```java

```

## 项目结构

```
com.flyiu.ai.mcp.mobile
├── config              // 配置类
├── controller          // REST API控制器
├── mcp                 // MCP工具实现
├── service             // 服务层
│   ├── appium          // Appium服务封装
│   ├── device          // 设备管理
│   └── screenshot      // 截图服务
├── model               // 数据模型
├── util                // 工具类
└── MobileApplication   // 应用入口
```

## 许可证

MIT 