package io.openvidu.server.common.enums;

public enum DogCheckEnum {
    CANNOT_FIND_DOG(10, "未找到加密狗"),
    READ_MESSAGE_ISEMPTY(20, "预存信息不存在"),
    READ_MESSAGE_FORMAT_ERROR(21, "预存信息格式不正确"),
    SIGN_ERROR_ISEMPTY(30, "签名为空"),
    SIGN_ERROR_NOMTHOD(31, "验签错误,无此算法"),
    SIGN_ERROR_PUBLICKEY(32, "验签错误,公钥非法"),
    SIGN_ERROR_PUBLICKEY_WRITE_ERROR(33, "验签错误,公钥数据内容读取错误"),
    SIGN_ERROR_PUBLICKEY_ISEMPTY(34, "验签错误,公钥数据为空"),
    VERIFICATION_SIGN_ERROR(35, "签名验证失败"),
    SIGN_LENGTH_ERROR(36, "密文长度非法"),
    SIGN_DATA_ERROR(37, "密文数据已损坏"),
    DOG_CHIPID_ERROR(40, "加密狗ID错误"),
    DONGLE_INFO_VALIDDATE_ERROR(50, "授权服务有效期不存在或已超过"),
    DONGLE_CONFIG_ERROR(60, "加密狗配置文件错误"),
    DONGLE_PUBLICKKEY_NOT_FOUND(61, "加密狗配置公钥未找到"),
    DONGLE_READ_PASS_NOT_FOUND(62, "加密狗配置读密码未找到");

    public final Integer type;
    public final String value;

    DogCheckEnum(Integer type, String value) {
        this.type = type;
        this.value = value;
    }
}
