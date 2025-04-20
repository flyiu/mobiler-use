package com.flyiu.ai.mcp.mobile.service.screenshot;

import com.flyiu.ai.mcp.mobile.config.AppiumConfig;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumOperationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.screenrecording.BaseStartScreenRecordingOptions;
import io.appium.java_client.screenrecording.ScreenRecordingUploadOptions;
import io.appium.java_client.screenrecording.CanRecordScreen;

/**
 * Android设备录屏服务
 * 支持按时间录制和按事件停止，并将视频同步到电脑上
 */
@Slf4j
@Service
public class RecordService {

    private final AppiumOperationService operationService;
    private final AppiumConfig appiumConfig;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // 用于存储当前正在进行的录制任务
    private final ConcurrentHashMap<String, RecordingSession> activeRecordings = new ConcurrentHashMap<>();
    // 线程池用于异步处理录制任务
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    @Autowired
    public RecordService(AppiumOperationService operationService, AppiumConfig appiumConfig) {
        this.operationService = operationService;
        this.appiumConfig = appiumConfig;

        // 确保视频存储目录存在
        createVideoDirectory();
    }

    /**
     * 开始录制设备屏幕
     * 
     * @param deviceName      设备名称
     * @param durationSeconds 录制时长（秒），0表示使用默认时长
     * @return 录制会话ID
     */
    public String startRecording(String deviceName, int durationSeconds) {
        // 如果传入0，则使用配置的默认时长
        if (durationSeconds == 0) {
            durationSeconds = appiumConfig.getRecording().getDefaultDuration();
        }

        // 限制不超过最大录制时长
        int maxDuration = appiumConfig.getRecording().getMaxDuration();
        if (durationSeconds > maxDuration) {
            log.warn("录制时长{}秒超过最大限制{}秒，已调整为最大值", durationSeconds, maxDuration);
            durationSeconds = maxDuration;
        }

        log.info("开始录制设备屏幕: {}, 时长: {}秒", deviceName, durationSeconds);

        // 检查该设备是否已经在录制
        if (activeRecordings.containsKey(deviceName)) {
            log.warn("设备 {} 已经在录制中，先停止当前录制", deviceName);
            stopRecording(deviceName);
        }

        // 每次录制60秒，直到录制时间大于durationSeconds
        AtomicInteger currentRecordingTime = new AtomicInteger(0);
        final int needRecordingTime = durationSeconds;

        try {
            // 获取AndroidDriver
            AndroidDriver driver = (AndroidDriver) operationService.getDriverByName(deviceName);

            // 录制参数配置
            Map<String, Object> options = new HashMap<>();
            // 视频质量，可选值: low, medium, high, photo
            options.put("videoQuality", appiumConfig.getRecording().getQuality());
            // 视频大小限制（MB），超过后自动停止录制
            options.put("videoSize", "100");
            // 录制时间限制（秒），超过后自动停止录制
            options.put("timeLimit", String.valueOf(durationSeconds));
            // 是否在录制时隐藏虚拟按键
            options.put("hideNaviBar", true);

            // 开始录制
            driver.startRecordingScreen();

            // 创建录制会话
            String sessionId = deviceName + "_" + DATE_FORMATTER.format(LocalDateTime.now());
            RecordingSession session = new RecordingSession(deviceName, LocalDateTime.now());
            activeRecordings.put(deviceName, session);

            // 安排在到达时间后自动停止录制
            scheduledExecutor.schedule(
                    () -> {
                        if (activeRecordings.containsKey(deviceName)) {
                            log.info("录制时间到达，自动停止录制: {}", deviceName);
                            stopAndSaveRecording(deviceName, true);
                            startRecording(deviceName, 60);
                            if (currentRecordingTime.get() < needRecordingTime) {
                                currentRecordingTime.addAndGet(60);
                            }
                        }
                    },
                    60,
                    TimeUnit.SECONDS);
            // currentRecordingTime += 60;

        } catch (Exception e) {
            log.error("开始录制失败: {}", deviceName, e);
            throw new RuntimeException("开始录制失败: " + e.getMessage(), e);
        }
        return null;

    }

    /**
     * 停止录制并保存视频
     * 
     * @param deviceName 设备名称
     * @return 保存的视频文件路径
     */
    public String stopRecording(String deviceName) {
        return stopAndSaveRecording(deviceName, true);
    }

    /**
     * 停止录制但不保存视频
     * 
     * @param deviceName 设备名称
     */
    public void cancelRecording(String deviceName) {
        stopAndSaveRecording(deviceName, false);
    }

    /**
     * 停止录制并选择是否保存视频
     * 
     * @param deviceName 设备名称
     * @param saveVideo  是否保存视频
     * @return 如果保存，返回保存的视频文件路径；否则返回null
     */
    private String stopAndSaveRecording(String deviceName, boolean saveVideo) {
        log.info("停止设备录屏: {}, 保存视频: {}", deviceName, saveVideo);

        if (!activeRecordings.containsKey(deviceName)) {
            log.warn("设备 {} 未在录制中", deviceName);
            return null;
        }

        RecordingSession session = activeRecordings.remove(deviceName);

        try {
            // 获取AndroidDriver
            AndroidDriver driver = (AndroidDriver) operationService.getDriverByName(deviceName);

            // 停止录制并获取Base64编码的视频数据
            String base64Video = driver.stopRecordingScreen();

            if (!saveVideo) {
                log.info("录制已停止，视频未保存: {}", deviceName);
                return null;
            }

            // 解码Base64数据并保存为视频文件
            byte[] videoData = Base64.getDecoder().decode(base64Video);

            // 生成文件名
            String fileName = generateVideoFileName(deviceName);
            Path videoPath = Paths.get(getVideoStoragePath(), fileName);

            // 保存视频文件
            try (FileOutputStream fos = new FileOutputStream(videoPath.toFile())) {
                fos.write(videoData);
            }

            log.info("录制视频已保存: {}", videoPath);

            // 异步将视频同步到计算机
            CompletableFuture.runAsync(() -> {
                syncVideoToComputer(videoPath.toFile());
            }, executorService);

            return videoPath.toString();

        } catch (WebDriverException e) {
            if (e.getMessage().contains("not recording")) {
                log.warn("设备未在录制中: {}", deviceName);
                return null;
            }
            log.error("停止录制失败: {}", deviceName, e);
            throw new RuntimeException("停止录制失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("停止录制或保存视频失败: {}", deviceName, e);
            throw new RuntimeException("停止录制或保存视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定设备当前的录制状态
     * 
     * @param deviceName 设备名称
     * @return 录制状态信息，null表示未在录制
     */
    public Map<String, Object> getRecordingStatus(String deviceName) {
        RecordingSession session = activeRecordings.get(deviceName);
        if (session == null) {
            return null;
        }

        Map<String, Object> status = new HashMap<>();
        status.put("deviceName", deviceName);
        status.put("startTime", session.getStartTime().toString());
        status.put("duration", Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds());
        status.put("isRecording", true);

        return status;
    }

    /**
     * 同步视频到计算机
     * 可以通过网络传输、文件共享等方式实现
     * 
     * @param videoFile 视频文件
     */
    private void syncVideoToComputer(File videoFile) {
        try {
            log.info("开始同步视频到计算机: {}", videoFile.getName());

            // 获取配置的同步路径，如果未配置则使用默认路径
            String syncDir = appiumConfig.getRecording().getSyncPath();
            if (syncDir == null || syncDir.trim().isEmpty()) {
                syncDir = System.getProperty("user.home") + File.separator + "AppiumVideos";
            }

            File syncDirFile = new File(syncDir);
            if (!syncDirFile.exists()) {
                syncDirFile.mkdirs();
            }

            File targetFile = new File(syncDirFile, videoFile.getName());
            FileUtils.copyFile(videoFile, targetFile);

            log.info("视频已同步到计算机: {}", targetFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("同步视频到计算机失败", e);
        }
    }

    /**
     * 生成视频文件名
     */
    private String generateVideoFileName(String deviceName) {
        String timestamp = DATE_FORMATTER.format(LocalDateTime.now());
        String format = appiumConfig.getRecording().getFormat();
        return String.format("%s_recording_%s.%s", deviceName, timestamp, format);
    }

    /**
     * 获取视频存储路径
     */
    private String getVideoStoragePath() {
        // 优先使用专门的视频存储路径配置
        if (appiumConfig.getRecording() != null &&
                appiumConfig.getRecording().getStoragePath() != null &&
                !appiumConfig.getRecording().getStoragePath().trim().isEmpty()) {
            return appiumConfig.getRecording().getStoragePath();
        }

        // 否则使用与截图相同的目录 + /videos
        return appiumConfig.getScreenshots().getStoragePath() + File.separator + "videos";
    }

    /**
     * 创建视频存储目录
     */
    private void createVideoDirectory() {
        String videoPath = getVideoStoragePath();
        try {
            Files.createDirectories(Paths.get(videoPath));
            log.info("创建视频存储目录: {}", videoPath);
        } catch (IOException e) {
            log.error("创建视频存储目录失败", e);
        }
    }

    /**
     * 录制会话类，用于跟踪录制状态
     */
    private static class RecordingSession {
        private final String deviceName;
        private final LocalDateTime startTime;
        private final AtomicBoolean active = new AtomicBoolean(true);

        public RecordingSession(String deviceName, LocalDateTime startTime) {
            this.deviceName = deviceName;
            this.startTime = startTime;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public boolean isActive() {
            return active.get();
        }

        public void setInactive() {
            active.set(false);
        }
    }
}
