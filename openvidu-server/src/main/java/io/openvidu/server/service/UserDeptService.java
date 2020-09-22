package io.openvidu.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.openvidu.server.common.dao.UserDeptMapper;
import io.openvidu.server.common.pojo.UserDept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDeptService {

    @Autowired
    private UserDeptMapper userDeptMapper;

    public IPage<UserDept> getByDeptId(Long deptId, int pageNum, int pageSize) {
        IPage<UserDept> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);

        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDept::getDeptId, deptId);

        queryWrapper.orderByAsc(UserDept::getUserId);

        return userDeptMapper.selectPage(page, queryWrapper);
    }

    public Set<Long> distinctDeptId(List<Long> deptId) {
        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserDept::getDeptId, deptId);
        List<UserDept> userDept = userDeptMapper.selectList(queryWrapper);
        return userDept.stream().map(UserDept::getDeptId).collect(Collectors.toSet());
    }

    public List<UserDept> getByDeptIds(List<Long> deptIds) {
        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserDept::getDeptId, deptIds);
        return userDeptMapper.selectList(queryWrapper);
    }

    public UserDept getByUserId(Long userId) {
        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDept::getUserId, userId);
        return userDeptMapper.selectOne(queryWrapper);
    }
}
