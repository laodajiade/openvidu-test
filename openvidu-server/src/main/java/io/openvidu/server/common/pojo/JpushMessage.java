package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author even
 * @date 2021/1/13 10:18
 */
@Data
public class JpushMessage {

    private Long id;

    private String uuid;

    private String msgContent;

    private Integer msgType;

    private Integer readType;

    private String ruid;

    private Date createTime;

}
