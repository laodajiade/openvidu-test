package io.openvidu.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.vo.DeptInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    public IPage<Department> listByParentId(DeptInfoVO vo) {

        if (vo.getDeptId() == 0L) {
            LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Department::getParentId, vo.getDeptId()).eq(Department::getProject, vo.getProject());
            Department department = departmentMapper.selectOne(queryWrapper);
            vo.setDeptId(department.getId());
        }

        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getParentId, vo.getDeptId()).eq(Department::getProject, vo.getProject());

        IPage<Department> page = new Page<>();
        page.setCurrent(vo.getPageNum());
        page.setSize(vo.getPageSize());
        return departmentMapper.selectPage(page, queryWrapper);
    }

    public List<Department> listByParentId(List<Long> parentIds) {
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Department::getParentId, parentIds);
        return departmentMapper.selectList(queryWrapper);
    }

    public Department getById(Long deptId) {
        return departmentMapper.selectById(deptId);
    }

    public Department getByParentId(Long parentId) {
        if (parentId == 0) {
            return null;
        }
        LambdaQueryWrapper<Department> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Department::getParentId, parentId);
        return departmentMapper.selectOne(queryWrapper);
    }
}
