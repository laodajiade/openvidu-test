package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

/**
 * 预置点信息
 * @author even
 * @date 2020/8/28 10:34
 */
@Data
public class Preset {

    private Long id;
    /**
     * 设备序列号
     */
    private String serialNumber;
    /**
     * 预置点索引
     */
    private Integer index;
    /**
     * 预置点配置信息
     */
    private String configInfo;
    /**
     * 预置点缩略图地址
     */
    private String thumbnailUrl;

    private Date createTime;

    private Date updateTime;
    /**
     * 是否删除（0:否 1:是）
     */
    private Integer deleted;
}
