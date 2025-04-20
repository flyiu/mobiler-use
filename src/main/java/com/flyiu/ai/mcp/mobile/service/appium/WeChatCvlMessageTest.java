package com.flyiu.ai.mcp.mobile.service.appium;

/**
 * 专门用于打开微信、搜索cvl用户并发送"你好"消息的测试类
 */
public class WeChatCvlMessageTest {
    
    public static void main(String[] args) {
        // 创建微信测试实例
        WeChatTest test = new WeChatTest();
        
        try {
            // 初始化
            System.out.println("========== 开始测试：打开微信搜索cvl用户并发送消息 ==========");
            test.setup();
            
            // 搜索cvl用户并发送"你好"消息
            test.searchUserAndSendMessage("cvl", "你好");
            
            // 等待一段时间，确保操作完成
            Thread.sleep(5000);
            
            System.out.println("========== 测试完成 ==========");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误:");
            e.printStackTrace();
        } finally {
            // 关闭驱动
            test.tearDown();
        }
    }
} 