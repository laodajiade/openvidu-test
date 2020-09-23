package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;

import java.util.List;

public interface DepartmentMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Department record);

    int insertSelective(Department record);

    Department selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Department record);

    int updateByPrimaryKey(Department record);

    List<DepartmentTree> selectByCorpId(Long corpId);

    List<Department> getSubFirstLevelDepts(Long parentId);

    Department selectRootDept(String project);

    List<Department> selectChildDept(Long parentId,String project);

    List<Department> selectByParentIds(List<Long> deptIds);
}
