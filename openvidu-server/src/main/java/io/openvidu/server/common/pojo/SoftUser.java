package io.openvidu.server.common.pojo;

import lombok.Data;

/**
 * @author even
 * @date 2020/8/18 18:50
 */
@Data
public class SoftUser {

    /**
     * 用户UUID
     */
    private Long uuid;
    /**
     * 用户名
     */
    private String username;
}
