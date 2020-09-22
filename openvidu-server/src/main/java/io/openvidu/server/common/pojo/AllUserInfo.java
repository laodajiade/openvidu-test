package io.openvidu.server.common.pojo;

import lombok.Data;

/**
 * @author even
 * @date 2020/9/22 15:58
 */
@Data
public class AllUserInfo {

    private Long userId;

    private String userName;

    private String uuid;

    private Integer type;

    private String phone;

    private String serialNumber;

}
