package io.openvidu.server.common.pojo.vo;


import lombok.Data;

/**
 * @author Administrator
 */
@Data
public class OftenContactsVo {
    private Long id;
    private String uuid;
    private String phone;
    private String userName;
    private Integer accountType;
    private String status;
}
