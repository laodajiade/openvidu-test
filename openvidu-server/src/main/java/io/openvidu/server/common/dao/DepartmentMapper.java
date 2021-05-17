package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    Department selectRootDeptByUuid(String uuid);

    List<Department> selectChildDept(@Param("parentId") Long parentId, @Param("project") String project);

    List<Department> selectByParentIds(List<Long> deptIds);

    Department getRootDept(String project);


    /**
     * 查询部门信息
     * @param parentId
     * @return
     */
    Department selectByParentId(Long parentId);
}
