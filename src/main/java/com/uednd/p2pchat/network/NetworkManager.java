package com.uednd.p2pchat.network;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 网络管理类，负责处理 Socket连接
 * 
 * @version 1.0.1
 * @since 2025-06-09
 */
public class NetworkManager {
    
    // 监听端口和接收连接请求（接收模式）
    private ServerSocket port_listening;
    
    // 连接客户端的Socket（接收模式）
    private Socket socket_to_client;

    // 连接服务端的Socket（发送模式）
    private Socket socket_to_server;
    
    // 文本输入流
    private BufferedReader textIn;
    
    // 文本输出流
    private PrintWriter textOut;
    
    // 对象输入流
    private ObjectInputStream fileIn;
    
    // 对象输出流
    private ObjectOutputStream fileOut;

    @Getter
    private final int port;  // 端口号
    
    /**
     * 构造函数
     * @param port 用户自定义端口号或默认端口号
     */
    public NetworkManager(int port) {
        // System.out.println("DEBUG: [NetworkManager::构造函数] - 初始化网络管理器，端口: " + port);
        this.port = port;
    }
    
    /**
     * 获取连接的客户端IP地址
     * @return 客户端IP地址
     */
    public String getConnectedClientIp() {
        if (socket_to_client != null)
            return socket_to_client.getInetAddress().getHostAddress();
        return null;
    }
    
    /**
     * 获取连接的客户端端口
     * @return 客户端端口
     */
    public int getConnectedClientPort() {
        if (socket_to_client != null)
            return socket_to_client.getPort();
        return 11451;
    }

    /**
     * 启动服务器，并设置超时
     * @param timeout 超时时间（毫秒）
     * @throws IOException 如果启动失败则抛出异常
     */
    public void startServer(int timeout) throws IOException {
        // System.out.println("DEBUG: [NetworkManager::startServer] - 启动服务器，端口: " + port + ", 超时: " + timeout + "ms");
        port_listening = new ServerSocket(port);  // checkPort的try_with已经自动关闭了socket，所以这里可以重新打开
        port_listening.setSoTimeout(timeout);
        // System.out.println("DEBUG: [NetworkManager::startServer] - 服务器启动成功");
    }
    
    /**
     * 等待客户端连接（作为服务端）
     * @throws IOException 如果连接失败则抛出异常
     */
    public void waitForConnection() throws IOException {
        // System.out.println("DEBUG: [NetworkManager::waitForConnection] - 等待客户端连接...");
        socket_to_client = port_listening.accept();  // 没有客户端连接端口时程序阻塞。直到有客户端连接，返回Socket对象
        
        // 初始化输入输出流，传入已连接的Socket对象【客户端连接】
        // System.out.println("DEBUG: [NetworkManager::waitForConnection] - 客户端已连接: " + socket_to_client.getInetAddress().getHostAddress() + ":" + socket_to_client.getPort());
        initStreams(socket_to_client);
    }
    
    /**
     * 连接到服务器（作为客户端）
     * @param opposite_UserIP 对方用户IP地址
     * @param opposite_UserPort 对方用户端口号
     * @throws IOException 如果连接失败则抛出异常
     */
    public void connectToServer(String opposite_UserIP, int opposite_UserPort) throws IOException {
        // System.out.println("DEBUG: [NetworkManager::connectToServer] - 尝试连接到 " + opposite_UserIP + ":" + opposite_UserPort);
        socket_to_server = new Socket(opposite_UserIP, opposite_UserPort);
        
        // 初始化输入输出流
        // System.out.println("DEBUG: [NetworkManager::connectToServer] - 连接成功，本地端口: " + socket_to_server.getLocalPort());
        initStreams(socket_to_server);
    }
    
    /**
     * 初始化输入输出流
     * @param socket 已连接的Socket
     * @throws IOException 如果初始化失败则抛出异常
     */
    private void initStreams(Socket socket) throws IOException {
        // System.out.println("DEBUG: [NetworkManager::initStreams] - 初始化输入输出流");
        // 同时初始化输入流和输出流，实现类似聊天软件的实时交互效果。
        
        /* 初始化文本流，用于文本传输 */
        // 文本输入流初始化
        InputStream inputStream = socket.getInputStream();  // 从socket中获取原始字节输入流
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");  // 将字节流转换为字符流，并指定UTF-8编码
        textIn = new BufferedReader(inputStreamReader);  // 将字符流转换为缓冲流，可以使用readLine()读取文本
        // 文本输出流初始化
        OutputStream outputStream = socket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
        textOut = new PrintWriter(outputStreamWriter, true);  // 使用true立即清空缓冲
        
        /* 初始化对象流，用于文件传输 */
        // 对象输出流初始化
        OutputStream objOutputStream = socket.getOutputStream();  // 获取原始字节输出流
        fileOut = new ObjectOutputStream(objOutputStream);  // 创建对象输出流，可以直接写入Java对象
        
        // 对象输入流初始化
        InputStream objInputStream = socket.getInputStream();
        fileIn = new ObjectInputStream(objInputStream);
        // System.out.println("DEBUG: [NetworkManager::initStreams] - 输入输出流初始化完成");
    }
    
    /**
     * 发送握手消息
     * @param message 要发送的握手消息
     */
    public void sendHandshakeMessage(String message) {
        // System.out.println("DEBUG: [NetworkManager::sendHandshakeMessage] - 发送握手消息: " + message);
        textOut.println(message);
    }

    /**
     * 接收握手消息
     * @return 接收到的握手消息
     * @throws IOException 如果接收失败则抛出异常
     */
    public String receiveHandshakeMessage() throws IOException {
        String message = textIn.readLine();
        // System.out.println("DEBUG: [NetworkManager::receiveHandshakeMessage] - 接收握手消息: " + message);
        return message;
    }
    
    /**
     * 发送文本消息
     * @param message 要发送的消息
     */
    public void sendTextMessage(String message) {
        // System.out.println("DEBUG: [NetworkManager::sendTextMessage] - 发送消息: " + message);
        textOut.println(message);  // 字符串传入输出流
    }
    
    /**
     * 接收文本消息
     * @return 接收到的消息
     * @throws IOException 如果接收失败则抛出异常
     */
    public String receiveTextMessage() throws IOException {
        String message = textIn.readLine();  // 从输入流中读取一行文本
        // System.out.println("DEBUG: [NetworkManager::receiveTextMessage] - 接收消息: " + message);
        return message;
    }

    /**
     * 发送文件
     * @param file 要发送的文件对象
     * @throws IOException 如果发送失败则抛出异常
     */
    public void sendFile(Object file) throws IOException {
        // System.out.println("DEBUG: [NetworkManager::sendFile] - 发送对象: " + file.getClass().getSimpleName());
        fileOut.writeObject(file);  // 将对象写入对象输出流
        fileOut.flush();  // 刷新输出流，确保数据被立即发送
        // System.out.println("DEBUG: [NetworkManager::sendFile] - 对象发送完成");
    }
    
    /**
     * 接收文件
     * @return 接收到的对象
     * @throws IOException 如果接收失败则抛出异常
     * @throws ClassNotFoundException 如果对象类型不匹配则抛出异常
     */
    public Object receiveFile() throws IOException, ClassNotFoundException {
        // System.out.println("DEBUG: [NetworkManager::receiveFile] - 等待接收对象...");
        // 从输入流中获取对象
        Object obj = fileIn.readObject();
        // System.out.println("DEBUG: [NetworkManager::receiveFile] - 接收到对象: " + (obj != null ? obj.getClass().getSimpleName() : "null"));
        return obj;
    }
    
    /**
     * （服务器）关闭与客户端的连接
     */
    public void closeConnection() {
        // System.out.println("DEBUG: [NetworkManager::closeConnection] - 开始关闭连接");
        try {
            // 关闭所有流
            if (textIn != null)
                textIn.close();
            if (textOut != null)
                textOut.close();
            if (fileIn != null)
                fileIn.close();
            if (fileOut != null)
                fileOut.close();
            
            // 再关闭 Socket连接
            if (socket_to_client != null)
                socket_to_client.close();
            if (socket_to_server != null)
                socket_to_server.close();
            if (port_listening != null)
                port_listening.close();
            
            // System.out.println("连接已关闭");
        } catch (IOException | NullPointerException e) {
            System.out.println("关闭连接时发生错误: " + e.getMessage());
            // System.out.println("DEBUG: [NetworkManager::closeConnection] - 关闭连接异常: " + e.getMessage());
        }

        // 移除对象引用，同时方便isConnected判断
        socket_to_server = null;
        socket_to_client = null;
        port_listening = null;  // 修复对方断开连接时，我方返回主菜单等待连接的端口占用问题
        // System.out.println("DEBUG: [NetworkManager::closeConnection] - 连接已完全关闭，引用已置空");
    }

    /**
     * 检查是否已连接
     * @return 如果已连接则返回true，否则返回false
     */
    public boolean isConnected() {
        boolean connected = (socket_to_client != null && socket_to_client.isConnected()) || 
               (socket_to_server != null && socket_to_server.isConnected());
        // System.out.println("DEBUG: [NetworkManager::isConnected] - 连接状态: " + connected);
        return connected;
    }
    
    /**
     * 关闭整个网络管理器，包括监听socket。
     * 应用退出时调用。
     */
    public void shutdown() {
        // System.out.println("DEBUG: [NetworkManager::shutdown] - 开始关闭网络管理器");
        closeConnection(); // 首先关闭当前所有连接
        try {
            if (port_listening != null && !port_listening.isClosed()) {
                port_listening.close();
                port_listening = null; // 确保监听socket被置为null
                // System.out.println("DEBUG: [NetworkManager::shutdown] - 监听Socket已关闭");
            }
        } catch (IOException e) {
            System.err.println("关闭监听Socket时出错: " + e.getMessage());
            // System.out.println("DEBUG: [NetworkManager::shutdown] - 关闭监听Socket异常: " + e.getMessage());
        }
        // System.out.println("DEBUG: [NetworkManager::shutdown] - 网络管理器关闭完成");
    }
} 