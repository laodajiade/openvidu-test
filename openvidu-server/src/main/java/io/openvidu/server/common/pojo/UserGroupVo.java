package io.openvidu.server.common.pojo;

import lombok.Data;

/**
 * @author even
 * @date 2020/9/9 20:12
 */
@Data
public class UserGroupVo {

    private Long userId;

    private String account;

    private String username;

    private String status;

    private Integer type;

    private String phone;
}
