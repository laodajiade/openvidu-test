package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.HiddenUserGroup;
import io.openvidu.server.common.pojo.HiddenUserGroupExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HiddenUserGroupMapper {
    long countByExample(HiddenUserGroupExample example);

    int deleteByExample(HiddenUserGroupExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HiddenUserGroup record);

    int insertSelective(HiddenUserGroup record);

    List<HiddenUserGroup> selectByExample(HiddenUserGroupExample example);

    HiddenUserGroup selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HiddenUserGroup record, @Param("example") HiddenUserGroupExample example);

    int updateByExample(@Param("record") HiddenUserGroup record, @Param("example") HiddenUserGroupExample example);

    int updateByPrimaryKeySelective(HiddenUserGroup record);

    int updateByPrimaryKey(HiddenUserGroup record);
}