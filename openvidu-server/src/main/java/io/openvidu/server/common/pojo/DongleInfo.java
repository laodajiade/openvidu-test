package io.openvidu.server.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @program: security
 * @description: 加密狗信息对象
 * @author: WuBing
 * @create: 2021-06-22 17:29
 **/
@Data
public class DongleInfo {


    //软终端链接数量
    private int softTerminal;

    //硬终端链接数量
    private int terminal;

    //最大链接数
    private int maxConcurrentNum;

    //录制许可
    private int recordingLicense;

    //穿透服务许可 以及可穿透个数
    private int maxTraversing;

    //sip设备接入数
    private int sipEquipmentNum;

    //h323设备接入数
    private int h323EquipmentNum;

    //直播许可
    private int livingLicense;

    //有效期
    private String validDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;


}
