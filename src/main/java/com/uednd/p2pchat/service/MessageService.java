package com.uednd.p2pchat.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import com.uednd.p2pchat.core.BackgroundService;
import com.uednd.p2pchat.model.Message;
import com.uednd.p2pchat.network.NetworkManager;
import com.uednd.p2pchat.repository.ChatRepository;

/**
 * 消息服务类，负责发送和接收文本消息
 * 
 * @version 1.0.1
 * @since 2025-06-14
 */
public class MessageService extends BackgroundService {
    
    /**
     * 消息处理器接口，定义了消息处理和错误处理方法
     */
    public interface MessageHandler {
        void handleMessage(String message);
        void handleError(String errorMessage);
    }
    
    // 网络管理器
    private final NetworkManager networkManager;
    
    // 数据库管理器
    private final ChatRepository dbManager;
    
    // 本地用户名
    private final String localUsername;
    
    // 对方用户名
    private final String opposite_Username;

    // 消息处理器
    private final MessageHandler messageHandler;
    
    /**
     * 构造函数
     * @param networkManager 网络管理器
     * @param dbManager 数据库管理器
     * @param localUsername 本地用户名
     * @param opposite_Username 对方用户名
     * @param messageHandler 消息处理器
     */
    public MessageService(NetworkManager networkManager, ChatRepository sql_path, String localUsername, String opposite_Username, MessageHandler messageHandler) {
        // System.out.println("DEBUG: [MessageService::构造函数] - 初始化消息服务，本地用户: " + localUsername + ", 对方用户: " + opposite_Username);
        this.networkManager = networkManager;
        this.dbManager = sql_path;
        this.localUsername = localUsername;
        this.opposite_Username = opposite_Username;
        this.messageHandler = messageHandler;
    }
    
    /**
     * 发送文本消息
     * @param content 消息内容
     * @throws IOException 如果发送失败则抛出异常
     * @throws SQLException 如果保存消息记录失败则抛出异常
     */
    public void sendTextMessage(String content) throws IOException, SQLException {
        // System.out.println("DEBUG: [MessageService::sendTextMessage] - 准备发送消息: " + content);
        // 检查网络连接
        if (!networkManager.isConnected()) {
            // System.out.println("DEBUG: [MessageService::sendTextMessage] - 未连接到对方，无法发送消息");
            throw new IOException("未连接到对方，无法发送消息");
        }
        
        // 发送消息
        networkManager.sendTextMessage(content);
        // System.out.println("DEBUG: [MessageService::sendTextMessage] - 消息发送成功");

        // 创建消息对象并保存消息记录
        Message message = new Message(localUsername, opposite_Username, content);
        message.setType("TEXT");
        dbManager.saveMessage(message);
        // System.out.println("DEBUG: [MessageService::sendTextMessage] - 消息记录已保存到数据库");
    }
    
    /**
     * 接收文本消息
     * @throws Exception 如果接收失败则抛出异常
     */
    @Override
    protected void task() throws Exception {
        // System.out.println("DEBUG: [MessageService::task] - 开始执行消息接收任务");
        try {
            if (networkManager.isConnected()) {
                // System.out.println("DEBUG: [MessageService::task] - 网络已连接，等待接收消息");
                String message = networkManager.receiveTextMessage();
                if (message != null) {
                    // 如果接收到消息，回调handleMessage处理
                    // System.out.println("DEBUG: [MessageService::task] - 接收到消息: " + message);
                    messageHandler.handleMessage(message);
                } else {
                    // 如果接收到null，表示对方可能已断开连接
                    // System.out.println("DEBUG: [MessageService::task] - 接收到null消息，对方可能已断开连接");
                    throw new IOException("对方已断开连接。");
                }
            } else {
                // 如果未连接，则短暂休眠以避免CPU空转
                // System.out.println("DEBUG: [MessageService::task] - 当前未连接，等待100ms");
                Thread.sleep(100);
            }
        } catch (Exception e) {
            if (isRunning()) {
                // System.out.println("DEBUG: [MessageService::task] - 消息接收异常: " + e.getMessage());
                messageHandler.handleError(e.getMessage());
            }
            throw e;  // 重新抛出异常，让BackgroundService的异常处理机制接管
        }
        // System.out.println("DEBUG: [MessageService::task] - 消息接收任务执行完毕");
    }
    
    /**
     * 获取与Opposite_User的聊天记录
     * @return 聊天记录列表
     * @throws SQLException 如果查询失败则抛出异常
     */
    public List<Message> getChatHistory() throws SQLException {
        // System.out.println("DEBUG: [MessageService::getChatHistory] - 获取聊天记录: " + localUsername + " 和 " + opposite_Username);
        List<Message> history = dbManager.getChatHistory(localUsername, opposite_Username);
        // System.out.println("DEBUG: [MessageService::getChatHistory] - 获取到 " + history.size() + " 条聊天记录");
        return history;
    }
    
    /**
     * 清除与Opposite_User的聊天记录
     * @throws SQLException 如果清除失败则抛出异常
     */
    public void clearChatHistory() throws SQLException {
        // System.out.println("DEBUG: [MessageService::clearChatHistory] - 清除聊天记录: " + localUsername + " 和 " + opposite_Username);
        dbManager.clearChatHistory(localUsername, opposite_Username);
        // System.out.println("DEBUG: [MessageService::clearChatHistory] - 聊天记录已清除");
    }
} 