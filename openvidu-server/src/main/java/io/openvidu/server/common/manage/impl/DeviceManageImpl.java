package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.common.manage.DeviceManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.Device;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chosongi
 * @date 2019/10/21 14:41
 */
@Service
public class DeviceManageImpl implements DeviceManage {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public List<Device> getSubDeviceByDeptId(Long deptId) {
        Department rootDept = departmentMapper.selectByPrimaryKey(deptId);
        if (rootDept == null) return null;

        List<Long> subDeptIds = new ArrayList<>();
        subDeptIds.add(rootDept.getId());
        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        if (!CollectionUtils.isEmpty(deptList))
            deptList.forEach(dept -> subDeptIds.add(dept.getOrgId()));

        List<String> deviceSerialNumbers = deviceMapper.selectDevSerialNumsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(deviceSerialNumbers) ?
                deviceMapper.getDevicesBySerialNumsList(deviceSerialNumbers) : null;
    }
}
