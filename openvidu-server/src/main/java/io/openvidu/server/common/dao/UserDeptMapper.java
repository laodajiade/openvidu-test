package io.openvidu.server.common.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.openvidu.server.common.pojo.RootDept;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.UserDeptSearch;

public interface UserDeptMapper extends BaseMapper<UserDept> {

    String selectByUserId(Long userId);

    int deleteByPrimaryKey(Long id);

    int insert(UserDept record);

    int insertSelective(UserDept record);

    UserDept selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDept record);

    int updateByPrimaryKey(UserDept record);

    UserDept selectBySearchCondition(UserDeptSearch search);

    RootDept selectRootDeptByUuid(String uuid);
}
