package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.DeviceManage;
import io.openvidu.server.common.pojo.Device;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author geedow
 * @date 2019/10/21 14:41
 */
@Service
public class DeviceManageImpl implements DeviceManage {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DepartmentManage departmentManage;

    @Override
    public List<Device> getSubDeviceByDeptId(Long deptId) {
        List<Long> subDeptIds = departmentManage.getSubDeptIds(deptId);
        if (CollectionUtils.isEmpty(subDeptIds)) return null;

        List<String> deviceSerialNumbers = deviceMapper.selectDevSerialNumsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(deviceSerialNumbers) ?
                deviceMapper.getDevicesBySerialNumsList(deviceSerialNumbers) : null;
    }
}
