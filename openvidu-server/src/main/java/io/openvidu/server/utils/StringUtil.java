package io.openvidu.server.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public class StringUtil {
    private static Random random = new Random();
    private static final String[] chars = { "0", "1", "2", "3", "5", "6", "7", "8", "9", "4", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

    private static final String  passwordRegs = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$";

    public static final String SPECIFIED_VERSION = "1.3.2";

    public static final String MEETING_INVITE = "%s邀请您参加会议，会议主题：%s，时间：%s-%s";
    public static final String MEETING_NOTIFY = "您有一个会议即将召开，会议主题：%s，时间：%s-%s";

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

    public static String uploadBase64Image(String base64Data, String serialNumber, String rootPath, String rootUrl) throws Exception {
        // 图片格式
        String dataPrix = "";
        // 图片内容
        String data = "";
        String[] d = base64Data.split("base64,");
        if (d != null && d.length == 2) {
            dataPrix = d[0];
            data = d[1];
        } else {
            throw new Exception("上传失败，数据不合法");
        }
        String suffix = "";
        if ("data:image/jpeg;".equalsIgnoreCase(dataPrix)
                || "data:image/jpg;".equalsIgnoreCase(dataPrix)) {
            suffix = ".jpg";
        } else if ("data:image/x-icon;".equalsIgnoreCase(dataPrix)) {
            suffix = ".ico";
        } else if ("data:image/gif;".equalsIgnoreCase(dataPrix)) {
            suffix = ".gif";
        } else if ("data:image/png;".equalsIgnoreCase(dataPrix)) {
            suffix = ".png";
        } else {
            throw new Exception("上传图片格式不合法");
        }
        data = data.replaceAll("(\\\\n)", "");
        byte [] bs = Base64.decodeBase64(data);
        // 图片存储根路径
        String realPath = rootPath + serialNumber;
        File file = new File(realPath);
        if (!file.exists()) {
            file.mkdir();
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 新文件名格式：uuid+后缀名
        String newFileName = new StringBuffer(uuid).append(suffix).toString();
        log.info("文件保存至：" + realPath + "/" + newFileName);
        try {
            // 使用apache提供的工具类操作流
            FileUtils.writeByteArrayToFile(new File(realPath + "/" + newFileName), bs);
        } catch (Exception ee) {
            throw new Exception("上传失败，写入文件失败，" + ee.getMessage());
        }
        return new StringBuffer().append(serialNumber).append("/").append(newFileName).toString();

    }

    public static String checkVersionReg(String version){
        return version.replaceAll("\\(.*\\)","").trim();
    }

    public static boolean compareVersion(String version1,String version2){
        return version1.compareTo(version2) > 0;
    }

}
