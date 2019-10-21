package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.common.manage.DeviceManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.utils.TreeToolUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        if (!CollectionUtils.isEmpty(deptList)) {
            TreeToolUtils treeToolUtils = new TreeToolUtils(
                    Collections.singletonList(DepartmentTree.builder().orgId(deptId).organizationName(rootDept.getDeptName()).build()),
                    deptList.stream().filter(s -> s.getOrgId().compareTo(deptId) != 0).collect(Collectors.toList()));
            treeToolUtils.getTree();
            subDeptIds = treeToolUtils.getSubDeptIds();
        }
        subDeptIds.add(deptId);

        List<String> deviceSerialNumbers = deviceMapper.selectDevSerialNumsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(deviceSerialNumbers) ?
                deviceMapper.getDevicesBySerialNumsList(deviceSerialNumbers) : null;
    }
}
