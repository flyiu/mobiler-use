package com.flyiu.ai.mcp.mobile.service.screenshot;

import com.flyiu.ai.mcp.mobile.config.AppiumConfig;
import com.flyiu.ai.mcp.mobile.service.appium.AppiumOperationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * 设备截图服务
 */
@Slf4j
@Service
public class ScreenshotService {

    private final AppiumOperationService operationService;
    private final AppiumConfig appiumConfig;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    // 默认压缩质量（0.0-1.0，1.0表示最高质量）
    private static final float DEFAULT_COMPRESSION_QUALITY = 0.5f;
    // 默认缩放比例（1.0表示原尺寸）
    private static final float DEFAULT_SCALE_FACTOR = 1f;

    @Autowired
    public ScreenshotService(AppiumOperationService operationService, AppiumConfig appiumConfig) {
        this.operationService = operationService;
        this.appiumConfig = appiumConfig;

        // 确保截图存储目录存在
        createScreenshotDirectory();
    }

    /**
     * 获取设备截图并保存
     * 
     * @param deviceName 设备名称
     * @return 保存的文件路径
     */
    public String takeAndSaveScreenshot(String deviceName) {
        try {
            // 获取截图
            File screenshotFile = operationService.takeScreenshot(deviceName);

            // 生成保存路径
            String fileName = generateFileName(deviceName);
            Path targetPath = Paths.get(appiumConfig.getScreenshots().getStoragePath(), fileName);

            // 压缩图片并保存
            compressAndSaveImage(screenshotFile, targetPath.toFile(), 
                               DEFAULT_COMPRESSION_QUALITY, 
                               DEFAULT_SCALE_FACTOR);
            log.info("压缩截图保存成功: {}", targetPath);

            // 清理临时文件
            FileUtils.deleteQuietly(screenshotFile);

            return targetPath.toString();
        } catch (IOException e) {
            log.error("保存截图失败", e);
            throw new RuntimeException("保存截图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取设备截图并转换为Base64
     * 
     * @param deviceName 设备名称
     * @return Base64编码的图片
     */
    public String takeScreenshotAsBase64(String deviceName) {
        try {
            // 获取截图
            File screenshotFile = operationService.takeScreenshot(deviceName);
            
            // 生成保存路径
            String fileName = generateFileName(deviceName);
            Path targetPath = Paths.get(appiumConfig.getScreenshots().getStoragePath(), fileName);
            
            // 压缩图片并保存
            File compressedFile = compressAndSaveImage(screenshotFile, targetPath.toFile(), 
                                                     DEFAULT_COMPRESSION_QUALITY, 
                                                     DEFAULT_SCALE_FACTOR);
            log.info("压缩截图保存成功: {}", targetPath);

            // 将压缩后的图片转换为Base64
            byte[] fileContent = Files.readAllBytes(compressedFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);

            // 清理临时文件
            FileUtils.deleteQuietly(screenshotFile);

            return base64Image;
        } catch (IOException e) {
            log.error("截图转Base64失败", e);
            throw new RuntimeException("截图转Base64失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取设备截图并转换为Base64，可指定压缩质量和缩放比例
     * 
     * @param deviceName 设备名称
     * @param quality 压缩质量 (0.0-1.0)
     * @param scaleFactor 缩放比例 (0.0-1.0)
     * @return Base64编码的图片
     */
    public String takeScreenshotAsBase64(String deviceName, float quality, float scaleFactor) {
        try {
            // 获取截图
            File screenshotFile = operationService.takeScreenshot(deviceName);
            
            // 生成保存路径
            String fileName = generateFileName(deviceName);
            Path targetPath = Paths.get(appiumConfig.getScreenshots().getStoragePath(), fileName);
            
            // 压缩图片并保存
            File compressedFile = compressAndSaveImage(screenshotFile, targetPath.toFile(), 
                                                     quality, 
                                                     scaleFactor);
            log.info("压缩截图保存成功: {}", targetPath);

            // 将压缩后的图片转换为Base64
            byte[] fileContent = Files.readAllBytes(compressedFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);

            // 清理临时文件
            FileUtils.deleteQuietly(screenshotFile);

            return base64Image;
        } catch (IOException e) {
            log.error("截图转Base64失败", e);
            throw new RuntimeException("截图转Base64失败: " + e.getMessage(), e);
        }
    }

    /**
     * 压缩图片并保存到指定位置
     * 
     * @param sourceFile 源图片文件
     * @param targetFile 目标文件
     * @param quality 压缩质量 (0.0-1.0)
     * @param scaleFactor 缩放比例 (0.0-1.0)
     * @return 保存的文件
     * @throws IOException 如果处理图片时发生错误
     */
    private File compressAndSaveImage(File sourceFile, File targetFile, float quality, float scaleFactor) throws IOException {
        BufferedImage originalImage = ImageIO.read(sourceFile);
        String format = appiumConfig.getScreenshots().getFormat().toLowerCase();
        
        // 调整图片尺寸
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        // 压缩图片并写入文件
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);
        }
        
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(targetFile)) {
            writer.setOutput(outputStream);
            writer.write(null, new IIOImage(resizedImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        
        return targetFile;
    }

    /**
     * 压缩图片并转换为Base64
     * 
     * @param imageFile 原图片文件
     * @param quality 压缩质量 (0.0-1.0)
     * @param scaleFactor 缩放比例 (0.0-1.0)
     * @return Base64编码的图片
     * @throws IOException 如果处理图片时发生错误
     */
    private String compressAndEncodeToBase64(File imageFile, float quality, float scaleFactor) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        String format = appiumConfig.getScreenshots().getFormat().toLowerCase();
        
        // 调整图片尺寸
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        // 压缩图片
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(quality);
        }
        
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(resizedImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        
        byte[] compressedImageData = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(compressedImageData);
    }

    // 生成文件名
    private String generateFileName(String deviceName) {
        String timestamp = DATE_FORMATTER.format(LocalDateTime.now());
        String format = appiumConfig.getScreenshots().getFormat().toLowerCase();
        return String.format("%s_%s.%s", deviceName, timestamp, format);
    }

    // 创建截图存储目录
    private void createScreenshotDirectory() {
        String storagePath = appiumConfig.getScreenshots().getStoragePath();
        try {
            Files.createDirectories(Paths.get(storagePath));
            log.info("创建截图存储目录: {}", storagePath);
        } catch (IOException e) {
            log.error("创建截图存储目录失败", e);
        }
    }
}