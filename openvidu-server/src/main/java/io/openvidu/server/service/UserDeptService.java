package io.openvidu.server.service;

import io.openvidu.server.common.dao.UserDeptMapper;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.UserDeptSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDeptService {

    @Autowired
    private UserDeptMapper userDeptMapper;


    public Set<Long> distinctDeptId(List<Long> deptId) {
        List<UserDept> userDept = userDeptMapper.selectByDeptIdsList(deptId);
        return userDept.stream().map(UserDept::getDeptId).collect(Collectors.toSet());
    }

    public List<UserDept> getByDeptIds(List<Long> deptIds) {
        return userDeptMapper.selectByDeptIdsList(deptIds);
    }

    public UserDept getByUserId(Long userId) {
        UserDeptSearch userDeptSearch = new UserDeptSearch();
        userDeptSearch.setUserId(userId);
        return userDeptMapper.selectBySearchCondition(userDeptSearch);
    }


    public List<Long> getUserIdsList(List<Long> deptIds) {
        List<Long> allList = new ArrayList<>();
        List<Long> userList = userDeptMapper.selectUserByDeptIdsList(deptIds);
        List<Long> deviceList = userDeptMapper.selectDeviceByDeptIdsList(deptIds);
        if (!CollectionUtils.isEmpty(userList)) {
            allList.addAll(userList);
        }
        if (!CollectionUtils.isEmpty(deviceList)) {
            allList.addAll(deviceList);
        }
        return allList;
    }
}
