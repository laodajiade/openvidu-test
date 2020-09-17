package io.openvidu.server.utils;

import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;

public class StringUtil {
    private static Random random = new Random();
    private static final String[] chars = { "0", "1", "2", "3", "5", "6", "7", "8", "9", "4", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

    private static final String  passwordRegs = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$";

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

    public static String getRandomPassWord(int length) {
        Random random = new Random();
        StringBuilder pass = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int code = random.nextInt(10);
            pass.append(code);
        }
        return pass.toString();
    }

    public static String createSessionId() {
        StringBuffer sb = new StringBuffer();
        sb.append("2");
        for (int i = 0; i < 10; i++) {
            sb.append(chars[random.nextInt(9)]);
        }
        return sb.toString();
    }

    public static boolean passwordCheck(String password) {
        if (Objects.isNull(password)) return false;
        return Pattern.compile(passwordRegs).matcher(password).matches();
    }
}
