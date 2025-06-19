package com.uednd.p2pchat.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.uednd.p2pchat.model.Message;
import com.uednd.p2pchat.model.User;

/**
 * 数据库管理类，负责处理SQLite数据库操作
 * 
 * @version 1.0.1
 * @since 2025-06-12
 */
public class ChatRepository {
    
    // 数据库文件路径
    private final String sql_path;
    
    // 数据库连接
    private Connection connection;
    
    /**
     * 构造函数
     * @param sql_path 数据库文件路径
     */
    public ChatRepository(String sql_path) {
        // System.out.println("DEBUG: [ChatRepository::构造函数] - 初始化数据库管理，路径: " + sql_path);
        this.sql_path = sql_path;
    }
    
    /**
     * 初始化数据库连接
     * @throws SQLException 如果数据库连接失败则抛出异常
     */
    public void initDatabase() throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::initDatabase] - 开始初始化数据库");
        /* Java11不用驱动注册，直接创建连接 */

        // 创建/打开数据库文件，并创建数据库连接
        connection = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
        // System.out.println("DEBUG: [ChatRepository::initDatabase] - 数据库连接已建立");
        
        // 创建Statement对象，该接口用于与数据库交互
        Statement stmt = connection.createStatement();

        // 更新数据库：创建用户表
        // System.out.println("DEBUG: [ChatRepository::initDatabase] - 创建用户表");
        stmt.executeUpdate(
            /*
             * 如果不存在名为"users"的表，则创建这个表，包含以下字段：
             * id：整数类型，作为主键，自动递增（用户ID）
             * username：文本类型，不允许为空（用户名）
             * ip_address：文本类型，可以为空（用户IP地址）
             * port：整数类型，可以为空（用户端口号）
             */
            "CREATE TABLE IF NOT EXISTS users ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL, ip_address TEXT, port INTEGER)"
        );
        
        // 更新数据库：创建消息记录表
        // System.out.println("DEBUG: [ChatRepository::initDatabase] - 创建消息记录表");
        stmt.executeUpdate(
            /*
             * 如果不存在名为"messages"的表，则创建这个表，包含以下字段：
             * id：整数类型，作为主键，自动递增（消息ID）
             * sender：文本类型，不允许为空（发送者）
             * receiver：文本类型，不允许为空（接收者）
             * content：文本类型，不允许为空（消息内容）
             * type：文本类型，不允许为空（消息类型）
             * file_path：文本类型，可以为空（文件路径）
             */
            "CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT,sender TEXT NOT NULL, receiver TEXT NOT NULL, content TEXT NOT NULL, type TEXT NOT NULL, file_path TEXT)"
        );
        
        // 关闭交互接口
        stmt.close();
        // System.out.println("DEBUG: [ChatRepository::initDatabase] - 数据库初始化完成");
    }
    
    
    /**
     * 保存用户信息
     * @param user 用户对象
     * @throws SQLException 如果保存失败则抛出异常
     */
    public void saveUser(User user) throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::saveUser] - 保存用户信息: " + user.getUsername());
        // 检查用户是否已存在
        /*
         * 从users表中查询指定用户名的用户ID
         * 如果查询结果有数据，说明用户存在
         * 如果查询结果为空，说明用户不存在
         */
        String to_check_user = "SELECT id FROM users WHERE username = '" + user.getUsername() + "'";
        Statement check_user = connection.createStatement();  // 创建新连接
        ResultSet is_user_exists = check_user.executeQuery(to_check_user);  // 执行SQL查询语句后，数据库返回查询结果并封装在ResultSet对象中
        
        if (is_user_exists.next()) {
            // 如果查询结果中下一行有数据，说明用户已存在，则更新用户信息
            // System.out.println("DEBUG: [ChatRepository::saveUser] - 用户已存在，更新信息");
            /*
             * 更新users表中指定用户名的用户信息
             * 设置新的IP地址和端口号
             * WHERE子句指定只更新匹配用户名的记录
             */
            String to_update_user_state = "UPDATE users SET ip_address = '" + user.getIpAddress() + "', port = " + user.getPort() + " WHERE username = '" + user.getUsername() + "'";
            Statement update_user_state = connection.createStatement();
            update_user_state.executeUpdate(to_update_user_state);
            update_user_state.close();
        } else {
            // 用户不存在，插入新用户
            // System.out.println("DEBUG: [ChatRepository::saveUser] - 创建新用户");
            /*
             * 向users表中插入新的用户记录
             * 指定用户名、IP地址和端口号的值
             * 自增ID会自动生成
             */
            String to_insert_user = "INSERT INTO users (username, ip_address, port) VALUES ('" + user.getUsername() + "', '" + user.getIpAddress() + "', " + user.getPort() + ")";
            Statement insert_user = connection.createStatement();
            insert_user.executeUpdate(to_insert_user);
            insert_user.close();
        }
        
        is_user_exists.close();
        check_user.close();
        // System.out.println("DEBUG: [ChatRepository::saveUser] - 用户信息保存完成");
    }
    
    /**
     * 保存消息记录
     * @param message 消息对象
     * @throws SQLException 如果保存失败则抛出异常
     */
    public void saveMessage(Message message) throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::saveMessage] - 保存消息记录，发送者: " + message.getSender() + ", 类型: " + message.getType());
        /*
         * 向messages表中插入新的消息记录
         * 指定发送者、接收者、内容、类型和文件路径的值
         * 如果文件路径为空，则插入空字符串
         * 自增ID会自动生成
         */
        String to_insert_message = "INSERT INTO messages (sender, receiver, content, type, file_path) VALUES ('" + message.getSender() + "', '" + message.getReceiver() + "', '" + message.getContent() + "', '" + message.getType() + "', '" + 
                     (message.getFilePath() != null ? message.getFilePath() : "") + "')";  // 如果文件路径不为空，则插入文件路径，否则插入空字符串
        Statement insert_message = connection.createStatement();
        insert_message.executeUpdate(to_insert_message);
        insert_message.close();
        // System.out.println("DEBUG: [ChatRepository::saveMessage] - 消息记录保存完成");
    }
    
    /**
     * 获取与特定用户的聊天记录
     * @param user1 用户1
     * @param user2 用户2
     * @return 消息列表
     * @throws SQLException 查询失败则抛出异常
     */
    public List<Message> getChatHistory(String user1, String user2) throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::getChatHistory] - 获取聊天记录: " + user1 + " 和 " + user2);
        /*
         * 从messages表中查询两个用户之间的所有消息
         * 查询条件：
         * （发送者是user1且接收者是user2）或（发送者是user2且接收者是user1）
         * 按照消息ID排序，确保消息按时间顺序显示
         */
        String to_get_chat_history = "SELECT * FROM messages WHERE (sender = '" + user1 + "' AND receiver = '" + user2 + "') OR (sender = '" + user2 + "' AND receiver = '" + user1 + "') ORDER BY id";
        Statement get_chat_history = connection.createStatement();
        ResultSet results_found_by_sql = get_chat_history.executeQuery(to_get_chat_history);
        List<Message> history_list = new ArrayList<Message>();  // 创建 Message类型的列表
        
        while (results_found_by_sql.next()) {
            Message save_results_found_by_sql = new Message();
            // 根据 ResultSet的查询结果，储存到新的 Message对象中
            save_results_found_by_sql.setSender(results_found_by_sql.getString("sender"));
            save_results_found_by_sql.setReceiver(results_found_by_sql.getString("receiver"));
            save_results_found_by_sql.setContent(results_found_by_sql.getString("content"));
            save_results_found_by_sql.setType(results_found_by_sql.getString("type"));
            save_results_found_by_sql.setFilePath(results_found_by_sql.getString("file_path"));
            
            history_list.add(save_results_found_by_sql);  // 将 Message对象添加到列表中
        }
        
        // 关闭资源
        results_found_by_sql.close();
        get_chat_history.close();
        
        // System.out.println("DEBUG: [ChatRepository::getChatHistory] - 获取到 " + history_list.size() + " 条聊天记录");
        return history_list;
    }
    
    /**
     * 清除特定用户的聊天记录
     * @param user1 用户1
     * @param user2 用户2
     * @throws SQLException 如果删除失败则抛出异常
     */
    public void clearChatHistory(String user1, String user2) throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::clearChatHistory] - 清除聊天记录: " + user1 + " 和 " + user2);
        /*
         * 从messages表中删除两个用户之间的所有消息
         * 删除条件：
         * 发送者是user1且接收者是user2，或者发送者是user2且接收者是user1
         */
        String to_clear_chat_history = "DELETE FROM messages WHERE (sender = '" + user1 + "' AND receiver = '" + user2 + "') OR (sender = '" + user2 + "' AND receiver = '" + user1 + "')";
        Statement clear_chat_history = connection.createStatement();
        clear_chat_history.executeUpdate(to_clear_chat_history);
        // int rowsAffected = clear_chat_history.executeUpdate(to_clear_chat_history);
        // System.out.println("DEBUG: [ChatRepository::clearChatHistory] - 已清除 " + rowsAffected + " 条聊天记录");
        clear_chat_history.close();
    }

    /**
     * 关闭数据库连接
     * @throws SQLException 如果关闭失败则抛出异常
     */
    public void closeConnection() throws SQLException {
        // System.out.println("DEBUG: [ChatRepository::closeConnection] - 关闭数据库连接");
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
            // System.out.println("DEBUG: [ChatRepository::closeConnection] - 数据库连接已关闭");
        }
    }
} 