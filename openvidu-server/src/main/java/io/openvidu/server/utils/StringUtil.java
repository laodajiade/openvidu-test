package io.openvidu.server.utils;

import java.util.Random;

public class StringUtil {
    private static Random random = new Random();
    private static final String[] chars = { "0", "1", "2", "3", "5", "6", "7", "8", "9", "4", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

    public static void assertNotEmpty(String param, String paramName) {
        if (param == null)
            throw new NullPointerException("Parameter Is Null:" + paramName);
        if (param.length() == 0)
            throw new NullPointerException("Parameter String Is Empty:" + paramName);
    }

    public static String getNonce(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(chars[random.nextInt(35)]);
        }
        return sb.toString();
    }

    public static String createSessionId() {
        StringBuffer sb = new StringBuffer();
        sb.append("2");
        for (int i = 0; i < 10; i++) {
            sb.append(chars[random.nextInt(9)]);
        }
        return sb.toString();
    }

}
