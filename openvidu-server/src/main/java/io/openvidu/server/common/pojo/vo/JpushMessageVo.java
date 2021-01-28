package io.openvidu.server.common.pojo.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author even
 * @date 2021/1/28 10:25
 */
@Data
public class JpushMessageVo {

    private Long id;

    private String uuid;

    private String msgContent;

    private Integer msgType;

    private Integer readType;

    private String ruid;

    private Long createTime;
}
