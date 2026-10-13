package com.nageoffer.shortlink.admin.toolkit;

import java.util.Random;

/**
 * 分组ID随机生成
 */
public class RandomGenerate {
    private static final String CHAR_POOL = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int CODE_LENGTH = 6;

    /**
     * 生成一个6位包含数字和字母的随机字符串
     * @return 6位随机码
     */
    public static String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        Random random = new Random();

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }

        return sb.toString();
    }
}
