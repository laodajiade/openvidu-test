package io.openvidu.server.utils;


import com.softkey.jsyunew3;


/**
 * @program: testFind
 * @description:
 * @author: WuBing
 * @create: 2021-06-18 09:57
 **/
public class SoftKeyUtils {

    private static jsyunew3 j9;

    private static String WHkey = "D7AF3A72";
    private static String WLkey = "E5ECEB6E";
    private static String RHkey = "01234567";
    private static String RLkey = "89ABCDEF";




/*   public static void main(String[] args) throws Exception {
        if (checkSoftKey()) {
            String outString = readStringToDongle(RHkey, RLkey);
            RsaEncrypt rsa = new RsaEncrypt();
            if (StringUtils.isNotBlank(outString)) {
                rsa.loadPublicKey(DEFAULT_PUBLIC_KEY);
                String data = "";
                String sign = "";
                data = outString.substring(1, outString.lastIndexOf('}'));
                System.out.println("验证的字符串：" + data);
                sign = outString.substring(outString.indexOf('[') + 1, outString.lastIndexOf(']'));
                System.out.println("签名：" + sign);
                byte[] bytes = Base64.decodeBase64(sign);
                boolean flag = rsa.doCheck(data, bytes, rsa.getPublicKey());
                if (flag) {
                    System.out.println("验签成功");
                }
            }


            System.out.println("读取预存字符 : " + outString);
        }
      if (checkSoftKey()) {

            String devicePath = getDevicePath();
            Boolean flag = writeStringToDongle("写入字符串测试啊啊啊啊啊啊啊啊啊啊啊啊啊", WHkey, WLkey);
            if (!flag) {
                System.out.println("写入失败");
            }

        } else {
            System.out.println("当前加密狗未插入");
        }
            String data = "速递科技啊啊啊啊速递科技啊啊啊啊速递科技啊啊啊啊速递科技啊啊啊啊速递科技啊啊啊啊速递科技啊啊";
        System.out.println("data.length() = " + data.length());
        String encString = encString(data);
        System.out.println("加密后的数据= " + encString + "数据长度:" + encString.length());

        String decString = decString(encString);
        System.out.println("解密后的数据= " + decString);*//*
    }*/

    //判断加密狗是否插入
    public static boolean checkSoftKey() {

        //这个用于判断系统中是否存在着加密锁。不需要是指定的加密锁,
        j9.FindPort(0);
        if (j9.get_LastError() != 0) {
            System.out.println("未找到加密锁,请插入加密锁后，再进行操作。");
            return false;
        }
        return true;
    }

    public static String getDevicePath() {
        String DevicePath = j9.FindPort(0);
        if (j9.get_LastError() != 0) {
            System.out.println("获取地址失败");
            return "";
        }
        return DevicePath;
    }

    public static String getChipID() {
        return j9.GetChipID(getDevicePath());
    }

    public static String getDogVersion(String DevicePath) {
        int id1, id2, ver;
        id1 = (int) j9.GetID_1(DevicePath);
        if (j9.get_LastError() != 0) {
            System.out.println("返回ID1错误");
            return "";
        }
        id2 = (int) j9.GetID_2(DevicePath);
        if (j9.get_LastError() != 0) {
            System.out.println("返回ID1错误");
            return "";
        }
        System.out.println("已成功返回锁的ID号:" + Integer.toHexString(id1) + Integer.toHexString(id2));

        //用于返回加密狗的版本号
        ver = j9.GetVersion(DevicePath);
        if (j9.get_LastError() != 0) {
            System.out.println("返回版本号错误");
            return "";
        }
        return Integer.toString(ver);
    }

    //写入字符串到地址
    public Boolean writeStringToDongle(String message, String Hkey, String Lkey) {
        byte[] buf = new byte[1];
        if (message.length() > 245 || message.length() == 0) {
            return false;
        }
        //写入字符串到地址1
        int WriteLen = j9.NewWriteString(message, (short) 1, Hkey, Lkey, getDevicePath());//WriteLen返回写入的字符串的长度，以字节来计算
        if (j9.get_LastError() != 0) {
            System.out.println("写字符串失败");
            return false;
        }
        buf[0] = (byte) WriteLen;
        j9.SetBuf(0, buf[0]);
        int ret = j9.YWriteEx((short) 0, (short) 1, Hkey, Lkey, getDevicePath());
        if (ret != 0) {
            return false;
        }
        return true;
    }


    //读取字符串
    public static String readStringToDongle(String Hkey, String Lkey) {
        int ret;
        short nlen;
        String outString;
        //先从地址0读到以前写入的字符串的长度
        ret = j9.YReadEx((short) 0, (short) 1, Hkey, Lkey, getDevicePath());
        nlen = j9.GetBuf(0);
        if (ret != 0) {
            System.out.println("读取字符串长度错误。错误码：" + ret);
            return "";
        }
        //再读取相应长度的字符串
        outString = j9.NewReadString((short) 1, nlen, Hkey, Lkey, getDevicePath());
        if (j9.get_LastError() != 0)
            return "";
        else
            return outString;
    }

    //对数据进行加密
    public static String encString(String inputData) {
        String encString = j9.SM2_EncString(inputData, getDevicePath());
        if (j9.get_LastError() != 0) {
            System.out.println("对数据进行加密时出现错误");
            return "";
        }
        return encString;
    }

    //对数据进行解密
    public static String decString(String encString) {
        String decString = j9.SM2_DecString(encString, "123", getDevicePath());
        if (j9.get_LastError() != 0) {
            System.out.println("对数据进行解时出现错误");
            return "";
        }
        return decString;
    }


}
