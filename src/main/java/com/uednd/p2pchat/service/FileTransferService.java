package com.uednd.p2pchat.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import lombok.Getter;

import com.uednd.p2pchat.model.FileInfo;
import com.uednd.p2pchat.model.Message;
import com.uednd.p2pchat.network.NetworkManager;
import com.uednd.p2pchat.repository.ChatRepository;
import com.uednd.p2pchat.util.DirectoryUtils;

/**
 * 文件传输服务类，负责发送和接收文件
 * 
 * @version 1.0.1
 * @since 2025-06-14
 */
public class FileTransferService {

    // 最大文件大小
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 1024;  // 1GB
    
    // 网络管理器
    private final NetworkManager networkManager;
    
    // 数据库管理器
    private final ChatRepository dbManager;
    
    // 本地用户名
    private final String localUsername;
    
    // 对方用户名
    private final String opposite_Username;

    // 下载文件保存路径
    @Getter
    private final String download_path;
    
    /**
     * 构造函数
     * @param networkManager 网络管理器
     * @param dbManager 数据库管理器
     * @param localUsername 本地用户名
     * @param opposite_Username 对方用户名
     * @param downloadPath 下载文件保存路径
     */
    public FileTransferService(NetworkManager networkManager, ChatRepository dbManager, String localUsername, String opposite_Username, String download_path) {
        // System.out.println("DEBUG: [FileTransferService::构造函数] - 初始化文件传输服务，本地用户: " + localUsername + ", 下载路径: " + download_path);
        this.networkManager = networkManager;
        this.dbManager = dbManager;
        this.localUsername = localUsername;
        this.opposite_Username = opposite_Username;
        this.download_path = download_path;
        
        // 确保下载目录存在
        if (!DirectoryUtils.createDirectoryIfNotExists(download_path)) {
            // System.out.println("DEBUG: [FileTransferService::构造函数] - 创建下载目录失败: " + download_path);
            throw new UncheckedIOException(new IOException("无法创建下载目录: " + download_path));
        }
        // System.out.println("DEBUG: [FileTransferService::构造函数] - 下载目录确认可用");
    }


    /**
     * 检查消息是否是文件类型
     * @param message 消息内容
     * @return 如果是文件类型则返回true，否则返回false
     */
    public static boolean isFileMessage(String message) {
        boolean isFile = message != null && message.startsWith("FILE:");
        // System.out.println("DEBUG: [FileTransferService::isFileMessage] - 检查消息是否是文件标识: " + message + " -> " + isFile);
        return isFile;
    }
    
    /**
     * 从文件创建 FileInfo 对象
     * @param file 要读取的文件
     * @return 创建的 FileInfo 对象
     * @throws IOException 如果读取文件失败
     */
    private FileInfo createFileInfo(File file) throws IOException {
        // System.out.println("DEBUG: [FileTransferService::createFileInfo] - 读取文件内容: " + file.getName());
        // 读取文件内容
        try (FileInputStream file_stream = new FileInputStream(file)) {
            // 获取文件长度
            long file_length = file.length();
            // 如果文件太大，则抛出异常
            if (file_length > MAX_FILE_SIZE) {
                // System.out.println("DEBUG: [FileTransferService::createFileInfo] - 文件太大，无法处理: " + file.getName() + ", 大小: " + fileLength + " 字节");
                throw new IOException("文件大小超过1GB，无法处理: " + file.getName());
            }
            
            // 创建byte数组，存储文件二进制内容
            byte[] file_data = new byte[(int) file_length];
            int bytes_read = file_stream.read(file_data);
            
            if (bytes_read < file_length) {
                // System.out.println("DEBUG: [FileTransferService::createFileInfo] - 无法完全读取文件: " + file.getName() + ", 已读取: " + bytesRead + "/" + fileLength + " 字节");
                throw new IOException("读取文件失败: " + file.getName());
            }
            
            // System.out.println("DEBUG: [FileTransferService::createFileInfo] - 文件读取成功，创建 FileInfo 对象");
            return new FileInfo(
                    file.getName(),
                    file_length,
                    file_data,
                    localUsername,
                    opposite_Username
            );
        }
    }

    /**
     * 发送文件
     * @param filePath 要发送的文件路径
     * @throws IOException 如果发送失败则抛出异常
     * @throws SQLException 如果保存消息记录失败则抛出异常
     */
    public void sendFile(String filePath) throws IOException, SQLException {
        // System.out.println("DEBUG: [FileTransferService::sendFile] - 准备发送文件: " + filePath);
        File file = new File(filePath);
        
        // 检查网络连接
        if (!networkManager.isConnected()) {
            // System.out.println("DEBUG: [FileTransferService::sendFile] - 未连接到对方，无法发送文件");
            throw new IOException("未连接到对方，无法发送文件");
        }
        
        // 发送文件前先发送文件标识
        // System.out.println("DEBUG: [FileTransferService::sendFile] - 发送文件标识消息");
        networkManager.sendTextMessage("FILE:" + file.getName());

        // 创建文件信息对象
        // System.out.println("DEBUG: [FileTransferService::sendFile] - 创建文件信息对象，文件大小: " + file.length() + " 字节");
        FileInfo fileInfo = createFileInfo(file);

        // 发送文件数据，传入要发送的文件对象
        // System.out.println("DEBUG: [FileTransferService::sendFile] - 发送文件数据中...");
        networkManager.sendFile(fileInfo);
        
        // 保存消息记录
        Message message = new Message(localUsername, opposite_Username, "发送文件: " + file.getName(), filePath);
        message.setType("FILE");
        dbManager.saveMessage(message);
        
        System.out.println("文件发送成功: " + file.getName() + " (" + file.length() + " 字节)");
        // System.out.println("DEBUG: [FileTransferService::sendFile] - 文件发送完成并保存消息记录");
    }
    
    /**
     * 接收文件
     * @return 接收到的文件路径
     * @throws IOException 如果接收失败则抛出异常
     * @throws ClassNotFoundException 如果对象类型不匹配则抛出异常
     * @throws SQLException 如果保存消息记录失败则抛出异常
     */
    public String receiveFile() throws IOException, ClassNotFoundException, SQLException {
        // System.out.println("DEBUG: [FileTransferService::receiveFile] - 准备接收文件");
        // 检查网络连接
        if (!networkManager.isConnected()) {
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 未连接到对方，无法接收文件");
            throw new IOException("未连接到对方，无法接收文件");
        }
        
        try {
            // 接收文件对象
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 等待接收文件对象");
            Object obj = networkManager.receiveFile();
            
            // 处理文件信息
            FileInfo fileInfo = (FileInfo) obj;
            String fileName = fileInfo.getFileName();

            // 拼接文件路径
            String filePath = download_path + File.separator + fileName;
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 接收到文件: " + fileName + ", 大小: " + fileInfo.getFileSize() + " 字节");
            
            // 通过文件IO保存文件
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 保存文件到: " + filePath);
            try (FileOutputStream file_stream = new FileOutputStream(filePath)) {
                file_stream.write(fileInfo.getFileData());
            }
            
            // 保存消息记录
            Message message = new Message(opposite_Username, localUsername, "接收文件: " + fileName, filePath);
            message.setType("FILE");
            dbManager.saveMessage(message);
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 消息记录已保存");
            
            System.out.println("文件接收成功: " + fileName + " (" + fileInfo.getFileSize() + " 字节)");
            
            return filePath;
        } catch (ClassNotFoundException e) {
            System.out.println("接收文件失败，类型不匹配: " + e.getMessage());
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - 类型不匹配异常: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.out.println("接收文件失败: " + e.getMessage());
            // System.out.println("DEBUG: [FileTransferService::receiveFile] - IO异常: " + e.getMessage());
            throw e;
        }
    }
} 
