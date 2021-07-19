package io.openvidu.server.utils;


import io.openvidu.server.common.enums.DogCheckEnum;
import io.openvidu.server.common.pojo.DongleInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import static io.openvidu.server.utils.SoftKeyUtils.*;

/**
 * @program: security
 * @description:
 * @author: WuBing
 * @create: 2021-06-22 11:32
 **/
@Slf4j
@Component
public class CheckUtils {


    private String publicKey;
    private String rhkey;
    private String rlkey;

    public SecurityResposne checkDog(String rhkey, String rlkey, String publicKey) {
        boolean exist = checkSoftKey();
        if (!exist) {
            return SecurityResposne.errorMsg(DogCheckEnum.CANNOT_FIND_DOG.type, DogCheckEnum.CANNOT_FIND_DOG.value);
        }
        SecurityResposne loadProperties = loadProperties(rhkey, rlkey, publicKey);
        if (loadProperties.getCode() != 200) {
            return loadProperties;
        }
        String readMsg = readStringToDongle(rhkey, rlkey);
        if (StringUtils.isEmpty(readMsg)) {
            return SecurityResposne.errorMsg(DogCheckEnum.READ_MESSAGE_ISEMPTY.type, DogCheckEnum.READ_MESSAGE_ISEMPTY.value);
        }
        String licInfo = readMsg.substring(readMsg.indexOf("LicInfo") + 8, readMsg.indexOf("Sign") - 1);
        String sign = readMsg.substring(readMsg.indexOf("Sign") + 5, readMsg.indexOf("Ver") - 1);
        String ver = readMsg.substring(readMsg.indexOf("Ver") + 4);
        String chipID = SoftKeyUtils.getChipID();

        if (StringUtils.isBlank(sign)) {
            return SecurityResposne.errorMsg(DogCheckEnum.SIGN_ERROR_ISEMPTY.type, DogCheckEnum.SIGN_ERROR_ISEMPTY.value);
        }
        RsaEncrypt rsa = new RsaEncrypt();
        SecurityResposne securityResposne = rsa.loadPublicKey(publicKey);
        if (securityResposne.getCode() != 200) {
            return securityResposne;
        }
        //验签
        String digest = MD5Util.encode(licInfo + chipID);
        SecurityResposne decrypt = rsa.decrypt(rsa.getPublicKey(), Base64.decodeBase64(sign));
        if (decrypt.getCode() != 200) {
            return decrypt;
        }
        String decryptsign = new String((byte[]) decrypt.getData());
        if (!digest.equals(decryptsign)) {
            return SecurityResposne.errorMsg(DogCheckEnum.VERIFICATION_SIGN_ERROR.type, DogCheckEnum.VERIFICATION_SIGN_ERROR.value);
        }
        DongleInfo dongleInfo = getDongleInfo(licInfo);

        SecurityResposne checkdongle = checkDongleMessage(dongleInfo);

        if (checkdongle.getCode() != 200) {
            return checkdongle;
        }

        return SecurityResposne.ok(dongleInfo);
    }

    public SecurityResposne checkDongleMessage(DongleInfo dongleInfo) {
        //  校验加密狗内存储信息 目前启动只验证服务授权时间
        String validDate = dongleInfo.getValidDate();
        if (StringUtils.isEmpty(validDate)) {
            return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.type, DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.value);
        }
        try {
            LocalDateTime begin = dongleInfo.getStartDate();
            LocalDateTime end = dongleInfo.getEndDate();
            LocalDateTime now = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
            if (now.isBefore(begin) || now.isAfter(end)) {
                return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.type, DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.type, DogCheckEnum.DONGLE_INFO_VALIDDATE_ERROR.value);
        }


        return SecurityResposne.ok();
    }

    public DongleInfo getDongleInfo(String licInfo) {
        try {
            DongleInfo dongleInfo = new DongleInfo();
            String[] split = licInfo.split(",");
            String validDate = null;
            for (String s : split) {
                switch (s.substring(0, 1)) {
                    case "A":
                        validDate = s.substring(s.indexOf("=") + 1);
                        break;
                    case "B":
                        dongleInfo.setSoftTerminal(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "C":
                        dongleInfo.setTerminal(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "D":
                        dongleInfo.setMaxConcurrentNum(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "E":
                        dongleInfo.setRecordingLicense(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "F":
                        dongleInfo.setMaxTraversing(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "G":
                        dongleInfo.setSipEquipmentNum(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "H":
                        dongleInfo.setH323EquipmentNum(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                    case "I":
                        dongleInfo.setLivingLicense(Integer.parseInt(s.substring(s.indexOf("=") + 1)));
                        break;
                }
            }
            if (StringUtils.isNotBlank(validDate)) {
                String[] date = validDate.split("-");
                String beaginData = date[0];
                String endData = date[1];
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                Date begin = sdf.parse(beaginData);
                Date end = sdf.parse(endData);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(end);
                calendar.add(Calendar.HOUR_OF_DAY, 23);
                calendar.add(Calendar.MINUTE, 59);
                calendar.add(Calendar.SECOND, 59);
                end = calendar.getTime();
                dongleInfo.setValidDate(validDate);
                dongleInfo.setStartDate(LocalDateTime.ofInstant(begin.toInstant(), ZoneId.systemDefault()));
                dongleInfo.setEndDate(LocalDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault()));
            }

            return dongleInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SecurityResposne loadProperties(String rhkey, String rlkey, String publicKey) {
        if (StringUtils.isBlank(publicKey)) {
            return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_PUBLICKKEY_NOT_FOUND.type, DogCheckEnum.DONGLE_PUBLICKKEY_NOT_FOUND.value);
        }
        if (StringUtils.isBlank(rhkey)) {
            return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_READ_PASS_NOT_FOUND.type, DogCheckEnum.DONGLE_READ_PASS_NOT_FOUND.value);
        }
        if (StringUtils.isBlank(rlkey)) {
            return SecurityResposne.errorMsg(DogCheckEnum.DONGLE_READ_PASS_NOT_FOUND.type, DogCheckEnum.DONGLE_READ_PASS_NOT_FOUND.value);
        }
        this.publicKey = publicKey;
        this.rhkey = rhkey;
        this.rlkey = rlkey;
        return SecurityResposne.ok();
    }


}
