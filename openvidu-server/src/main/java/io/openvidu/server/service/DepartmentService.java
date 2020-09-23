package io.openvidu.server.service;

import com.github.pagehelper.PageHelper;
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

    public List<Department> listByParentId(DeptInfoVO vo) {

        if (vo.getDeptId() == 0L) {
            Department department = departmentMapper.selectRootDept(vo.getProject());
            vo.setDeptId(department.getId());
        }

        PageHelper.startPage(vo.getPageNum(),vo.getPageSize());
        return departmentMapper.selectChildDept(vo.getDeptId(), vo.getProject());
    }

    public List<Department> listByParentId(List<Long> parentIds) {
        return departmentMapper.selectByParentIds(parentIds);
    }

    public Department getById(Long deptId) {
        return departmentMapper.selectByPrimaryKey(deptId);
    }

}
