# mobiler-usr MCP

基于Spring Boot和Appium的移动设备控制服务，支持通过REST API和MCP协议操作移动设备。

## 主要功能

- 封装Appium常规操作，提供简洁API
- 支持设备屏幕截图
- 自动录制手机界面并回传电脑
- 基于MCP协议，方便与AI模型交互，可快速集成到cursor中，或自行开发MCP客户端（后续会开源一个JAVA版本）
- 目前仅支持Android

## 环境要求

- JDK 21+
- Maven 3.6+
- Node.js和npm
- Appium 2.0+
- Android SDK（操作Android设备）

相关技术请自行查询文档。

## 快速开始

### 1. 准备环境

安装并运行Appium服务器：

```bash
npm install -g appium
appium driver install uiautomator2  # Android驱动
appium                              # 启动Appium服务器，目前程序自带的启动存在BUG
```

连接移动设备：
- 通过USB连接Android设备并启用USB调试
- 或使用Android模拟器
- 注意：程序虽自带Appium启动功能，但目前存在BUG

### 2. 配置应用

复制并编辑配置文件：

```bash
# 在项目根目录执行
cd mobile
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
```

编辑`application-local.properties`文件，填入API密钥和其他敏感配置：

```properties
# OpenAI API配置 主要用于 图片识别，测试时使用gemini-2.0-flash
spring.ai.openai.api-key=your-actual-api-key-here
spring.ai.openai.base-url=your-actual-base-url-here
spring.ai.openai.chat.options.model=your-actual-model-name-here
```

修改`src/main/resources/application.yml`配置Appium服务器和设备信息：


### 3. 运行服务

```bash
com.flyiu.ai.mcp.mobile.MobileApplication
```

## API使用说明

### REST API示例

```
# 连接设备
POST /api/mobile/android/connect

# 截取屏幕
GET /api/mobile/screenshot?deviceName=default-android

# 点击元素
POST /api/mobile/click
参数：
- deviceName: 设备名称
- locatorType: 定位器类型（id/xpath等）
- locatorValue: 定位器值
```

### MCP工具列表

本服务基于Spring AI的MCP协议实现，提供以下工具用于与AI模型交互：

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

### 调用示例

LLM工具调用示例：

```json
{
  "name": "takeScreenshot",
  "parameters": {
    "deviceName": "default-android",
    "asBase64": true
  }
}
```

## 在Cursor、其它MCP中使用

{ "mcpServers": { "mobile": { "url": "http://localhost:9080/sse" } } }


### 推荐模型
- 建议使用Claude 3.7或Gemini 2.5

### 故障排除

**服务连接问题**
- 确保MCP服务正在运行 (`mvn spring-boot:run`)
- 检查端口是否正确 (默认9080)
- 确认没有防火墙阻止

**设备连接问题**
- 运行 `adb devices` 确认设备已连接
- 确保Appium服务器正在运行
- 检查设备USB调试是否开启

**API调用失败**
- 检查API密钥配置是否正确
- 查看MCP服务日志获取详细错误信息

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