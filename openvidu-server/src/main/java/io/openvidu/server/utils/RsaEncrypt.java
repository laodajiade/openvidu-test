package io.openvidu.server.utils;

import io.openvidu.server.common.enums.DogCheckEnum;
import sun.misc.BASE64Decoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * 生成私钥：openssl genrsa -out rsa_private_key.pem 1024
 * 根据私钥生成公钥：openssl rsa -in rsa_private_key.pem -out rsa_public_key.pem -pubout
 * 私钥还不能直接被使用，需要进行PKCS#8编码：openssl pkcs8 -topk8 -in rsa_private_key.pem -out pkcs8_rsa_private_key.pem -nocrypt
 * sha1签名 openssl sha1 -sign rsa_private_key.pem -out rsasign.bin tos.txt
 * pkcs8_rsa_private_key 私钥
 * java可以使用
 */
public class RsaEncrypt {


    /**
     * rsa签名
     *
     * @param content    待签名的字符串
     * @param privateKey rsa私钥字符串
     * @param charset    字符编码
     * @return 签名结果
     * @throws Exception 签名失败则抛出异常
     */
    public byte[] rsaSign(String content, RSAPrivateKey priKey)
            throws SignatureException {
        try {

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(priKey);
            signature.update(content.getBytes("utf-8"));

            byte[] signed = signature.sign();
            return signed;
        } catch (Exception e) {
            throw new SignatureException("RSAcontent = " + content
                    + "; charset = ", e);
        }
    }

    /**
     * rsa验签
     *
     * @param content 被签名的内容
     * @param sign    签名后的结果
     * @param pubKey  rsa公钥
     * @param charset 字符集
     * @return 验签结果
     * @throws SignatureException 验签失败，则抛异常
     */
    public SecurityResposne doCheck(String content, byte[] sign, RSAPublicKey pubKey) {
        try {
            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes("utf-8"));
            boolean verify = signature.verify((sign));
            if (!verify) {
                return SecurityResposne.errorMsg(DogCheckEnum.VERIFICATION_SIGN_ERROR.type, DogCheckEnum.VERIFICATION_SIGN_ERROR.value);
            }
            return SecurityResposne.ok();
        } catch (Exception e) {
/*            throw new SignatureException("RSA验证签名[content = " + content
                    + "; charset = " + "; signature = " + sign + "]发生异常!", e);*/

            return SecurityResposne.errorMsg(DogCheckEnum.VERIFICATION_SIGN_ERROR.type, DogCheckEnum.VERIFICATION_SIGN_ERROR.value);
        }
    }


    /**
     * 私钥
     */
    private RSAPrivateKey privateKey;

    /**
     * 公钥
     */
    private RSAPublicKey publicKey;

    /**
     * 字节数据转字符串专用集合
     */
    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 获取私钥
     *
     * @return 当前的私钥对象
     */
    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 获取公钥
     *
     * @return 当前的公钥对象
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * 随机生成密钥对
     */
    public void genKeyPair() {
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
    }

    /**
     * 从字符串中加载公钥
     *
     * @param publicKeyStr 公钥数据字符串
     * @throws Exception 加载公钥时产生的异常
     */
    public SecurityResposne loadPublicKey(String publicKeyStr) {
        try {
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] buffer = base64Decoder.decodeBuffer(publicKeyStr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            return SecurityResposne.ok();
        } catch (NoSuchAlgorithmException e) {
//            throw new Exception("无此算法");
            return SecurityResposne.errorException("无此算法");
        } catch (InvalidKeySpecException e) {
            return SecurityResposne.errorException("公钥非法");
//            throw new Exception("公钥非法");
        } catch (IOException e) {
//            throw new Exception("公钥数据内容读取错误");
            return SecurityResposne.errorException("公钥数据内容读取错误");
        } catch (NullPointerException e) {
//            throw new Exception("公钥数据为空");
            return SecurityResposne.errorException("公钥数据为空");
        }
    }

    /**
     * 公钥解密过程
     *
     * @param publicKey  公钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public SecurityResposne decrypt(RSAPublicKey publicKey, byte[] cipherData) {
        if (publicKey == null) {
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_ERROR_PUBLICKEY_ISEMPTY.type, DogCheckEnum.SIGN_ERROR_PUBLICKEY_ISEMPTY.value);
//            throw new Exception("解密公钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            // 使用默认RSA
            cipher = Cipher.getInstance("RSA");
            // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(cipherData);
            return SecurityResposne.ok(output);
        } catch (NoSuchAlgorithmException e) {
//            throw new Exception("无此解密算法");
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_ERROR_NOMTHOD.type, DogCheckEnum.SIGN_ERROR_NOMTHOD.value);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return SecurityResposne.errorMsg(DogCheckEnum.VERIFICATION_SIGN_ERROR.type, DogCheckEnum.VERIFICATION_SIGN_ERROR.value);
        } catch (InvalidKeyException e) {
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_ERROR_PUBLICKEY.type, DogCheckEnum.SIGN_ERROR_PUBLICKEY.value);
//            throw new Exception("解密公钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_LENGTH_ERROR.type, DogCheckEnum.SIGN_LENGTH_ERROR.value);
//            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_DATA_ERROR.type, DogCheckEnum.SIGN_DATA_ERROR.value);
            //     throw new Exception("密文数据已损坏");
        }
    }


    /**
     * 解密过程
     *
     * @param privateKey 私钥
     * @param cipherData 密文数据
     * @return 明文
     * @throws Exception 解密过程中的异常信息
     */
    public byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData)
            throws Exception {
        if (privateKey == null) {
            throw new Exception("解密私钥为空, 请设置");
        }
        Cipher cipher = null;
        try {
            // , new BouncyCastleProvider()
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此解密算法");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("解密私钥非法,请检查");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("密文长度非法");
        } catch (BadPaddingException e) {
            throw new Exception("密文数据已损坏");
        }
    }

    /**
     * 字节数据转十六进制字符串
     *
     * @param data 输入数据
     * @return 十六进制内容
     */
    public static String byteArrayToString(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            // 取出字节的高四位 作为索引得到相应的十六进制标识符 注意无符号右移
            stringBuilder.append(HEX_CHAR[(data[i] & 0xf0) >>> 4]);
            // 取出字节的低四位 作为索引得到相应的十六进制标识符
            stringBuilder.append(HEX_CHAR[(data[i] & 0x0f)]);
            if (i < data.length - 1) {
                stringBuilder.append(' ');
            }
        }
        return stringBuilder.toString();
    }

    // btye转换hex函数
    public static String ByteToHex(byte[] byteArray) {
        StringBuffer StrBuff = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
                StrBuff.append("0").append(
                        Integer.toHexString(0xFF & byteArray[i]));
            } else {
                StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
            }
        }
        return StrBuff.toString();
    }

    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public static byte[] readFileByBytes(String fileName) {
        File file = new File(fileName);
        InputStream in = null;
        byte[] txt = new byte[(int) file.length()];
        try {
            // 一次读一个字节
            in = new FileInputStream(file);
            int tempbyte;
            int i = 0;

            while ((tempbyte = in.read()) != -1) {
                txt[i] = (byte) tempbyte;
                i++;
            }
            in.close();
            return txt;
        } catch (IOException e) {
            e.printStackTrace();
            return txt;
        }
    }


}
