package com.flyiu.ai.mcp.mobile.service.appium;

import com.flyiu.ai.mcp.mobile.config.AppiumConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Appium服务器管理服务
 */
@Slf4j
@Service
public class AppiumServerService {

    private final AppiumConfig appiumConfig;
    private Process appiumProcess;

    @Autowired
    public AppiumServerService(AppiumConfig appiumConfig) {
        this.appiumConfig = appiumConfig;

        // 初始化日志
        log.info("初始化Appium服务器管理，服务器URL: {}, Appium路径: {}, 自动启动: {}",
                appiumConfig.getServer().getUrl(),
                appiumConfig.getServer().getAppiumPath(),
                appiumConfig.getServer().isAutoStart() ? "是" : "否");
    }

    /**
     * 检查Appium服务器是否运行中
     * 
     * @return 是否运行中
     */
    public boolean isServerRunning() {
        try {
            URL url = new URL(appiumConfig.getServer().getUrl() + "/status");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(appiumConfig.getServer().getConnectTimeout());
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            log.debug("Appium服务器未运行: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 启动Appium服务器
     * 
     * @return 是否成功启动
     */
    public boolean startServer() {
        if (isServerRunning()) {
            log.info("Appium服务器已经在运行中");
            return true;
        }

        try {
            log.info("正在启动Appium服务器...");

            String appiumPath = appiumConfig.getServer().getAppiumPath();
            log.info("使用Appium路径: {}", appiumPath);

            // 检查是否是文件路径
            if (appiumPath.contains("/") || appiumPath.contains("\\")) {
                // 验证路径是否存在
                Path path = Paths.get(appiumPath);
                if (!Files.exists(path)) {
                    log.error("Appium可执行文件不存在: {}", appiumPath);
                    return false;
                }
            }

            // 构建启动命令 - 使用cmd /c来执行命令
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", appiumPath,
                    "--address", "0.0.0.0",
                    "--port", String.valueOf(getPortFromUrl(appiumConfig.getServer().getUrl())),
                    "--log-level", "debug",
                    "--relaxed-security");
            processBuilder.redirectErrorStream(true);

            log.info("执行命令: cmd /c {} --address 0.0.0.0 --port {} --log-level debug --relaxed-security",
                    appiumPath, getPortFromUrl(appiumConfig.getServer().getUrl()));

            // 启动进程
            appiumProcess = processBuilder.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(appiumProcess.getInputStream()));
            String line;
            int timeoutSeconds = 30;
            long startTime = System.currentTimeMillis();

            // 等待Appium服务器启动
            while ((System.currentTimeMillis() - startTime) < timeoutSeconds * 1000) {
                if (isServerRunning()) {
                    log.info("Appium服务器已成功启动");
                    return true;
                }

                // 输出Appium启动日志
                while (reader.ready() && (line = reader.readLine()) != null) {
                    log.info("Appium: {}", line);
                }

                Thread.sleep(1000);
            }

            Runnable runnable = () -> {
                try {
                    appiumProcess.waitFor();
                } catch (InterruptedException e) {
                    log.error("Appium服务器异常", e);
                }
            };
            new Thread(runnable).start();

            log.error("Appium服务器启动超时");
            return false;
        } catch (IOException | InterruptedException e) {
            log.error("启动Appium服务器失败", e);
            return false;
        }
    }

    /**
     * 停止Appium服务器
     */
    public void stopServer() {
        if (appiumProcess != null && appiumProcess.isAlive()) {
            log.info("正在停止Appium服务器...");
            appiumProcess.destroy();

            try {
                // 等待进程结束
                if (!appiumProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("Appium服务器未能在10秒内停止，强制终止");
                    appiumProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                log.error("停止Appium服务器时被中断", e);
                Thread.currentThread().interrupt();
            }

            log.info("Appium服务器已停止");
        }
    }

    /**
     * 从URL中提取端口号
     * 
     * @param url Appium服务器URL
     * @return 端口号，默认为4723
     */
    private int getPortFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getPort() != -1 ? parsedUrl.getPort() : 4723;
        } catch (Exception e) {
            log.warn("解析URL端口失败: {}, 使用默认端口4723", url);
            return 4723;
        }
    }

    public static void main(String[] args) {

        AppiumConfig appiumConfig = new AppiumConfig();
        appiumConfig.setServer(new AppiumConfig.Server());
        // appiumConfig.getServer().setAppiumPath(
        // "D:\\Program\\Appium\\Appium-2.11.0-win64\\Appium-2.11.0-win64\\node_modules\\appium\\bin\\appium.js");
        appiumConfig.getServer().setUrl("http://127.0.0.1:4723");
        appiumConfig.getServer().setAutoStart(true);

        AppiumServerService appiumServerService = new AppiumServerService(appiumConfig);
        boolean started = appiumServerService.startServer();

        if (started) {
            log.info("Appium服务器已启动，程序将保持运行状态");
            // 添加无限循环，防止程序退出导致Appium服务器关闭
            try {
                while (true) {
                    Thread.sleep(10000); // 每10秒检查一次服务器状态
                    if (!appiumServerService.isServerRunning()) {
                        log.warn("Appium服务器已停止运行，尝试重新启动");
                        appiumServerService.startServer();
                    }
                }
            } catch (InterruptedException e) {
                log.error("程序被中断", e);
                // 确保在程序退出时关闭Appium服务器
                appiumServerService.stopServer();
            }
        } else {
            log.error("Appium服务器启动失败");
        }
    }
}