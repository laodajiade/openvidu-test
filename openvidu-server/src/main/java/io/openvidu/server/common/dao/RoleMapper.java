package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Role;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RoleMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Role record);

    int insertSelective(Role record);

    Role selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Role record);

    int updateByPrimaryKey(Role record);

    @Select("select dept_limit from sd_role a join sd_user_role b on a.id = b.role_id where b.user_id = #{userId}")
    Integer getDeptLimitByUserId(@Param("userId") long userId);

}