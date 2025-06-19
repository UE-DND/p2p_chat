package com.uednd.p2pchat.ui.cli.util;

import com.uednd.p2pchat.util.ANSIcolor;

/**
 * 菜单显示工具类
 * <p>
 * 用于显示菜单和分隔线
 * 
 * @version 1.0.1
 * @since 2025-06-17
 */
public class MenuDisplay {

    /**
     * 清空控制台（但是真的清除了吗）
     */
    public static void clearScreen() {
        // 打印 50个空行
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
        
        System.out.print("\033[H");  // 将光标移到左上角后打印菜单
        System.out.flush();  // 强制输出
    }

    /**
     * 清空当前行并将光标移动到行首
     * <p>
     * 用于聊天界面，当收到新消息时清除用户正在输入的内容，
     * 显示新消息后再重新显示用户输入提示
     */
    public static void clearCurrentLine() {
        System.out.print("\r" + " ".repeat(80) + "\r");
    }

    /**
     * 打印分隔线的方法
     * @param title 标题 (可为 null)
     */
    public static void printSeparator(String title) {
        int totalWidth = 60;  // 统一宽度
        String line = "━";

        // 如果标题为空，则打印统一宽度的分隔线
        // if (title.trim().isEmpty() || title == null) {
        if (title == null || title.trim().isEmpty()) {
            System.out.println(ANSIcolor.CYAN + line.repeat(totalWidth) + ANSIcolor.RESET);
        } else {
            // 计算标题的实际长度：减去颜色代码
            int titleLength = title.replaceAll("\u001B\\[[;\\d]*m", "").length();
            int len = totalWidth - titleLength - 2;  // 标题两侧各一个空格
            
            // 计算左右分割线的长度
            int leftLen = len / 2;
            int rightLen = len - leftLen;

            String separator = ANSIcolor.CYAN +
                    line.repeat(leftLen) +
                    " " +
                    ANSIcolor.YELLOW + ANSIcolor.BOLD +
                    title +
                    ANSIcolor.RESET +
                    ANSIcolor.CYAN +
                    " " +
                    line.repeat(rightLen) +
                    ANSIcolor.RESET;
            System.out.println(separator);
        }
    }
} 