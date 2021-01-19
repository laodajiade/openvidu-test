package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author even
 * @date 2021/1/18 12:01
 */
@Data
public class CallHistory {

    private Long id;

    private String roomId;

    private String uuid;

    private String username;

    private String ruid;

    private Date createTime;

    private Date updateTime;
}
