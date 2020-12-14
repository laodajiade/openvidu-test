package io.openvidu.server.common.pojo.dto;

import lombok.Data;

/**
 * @author chosongi
 * @date 2020/8/7 19:19
 */
@Data
public class UserDeviceDeptInfo {
    private Long userId;
    private String uuid;
    private String username;
    private Long deptId;
    private String deptName;
    private String serialNumber;
    private String deviceName;
    private String deviceModel;
    private String deviceVersion;
}
