package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.Device;

import java.util.List;

/**
 * @author chosongi
 * @date 2019/10/21 14:41
 */
public interface DeviceManage {
    List<Device> getSubDeviceByDeptId(Long deptId);
}
