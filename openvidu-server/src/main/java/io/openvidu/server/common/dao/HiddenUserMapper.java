package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.HiddenUser;
import io.openvidu.server.common.pojo.HiddenUserExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HiddenUserMapper {
    long countByExample(HiddenUserExample example);

    int deleteByExample(HiddenUserExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HiddenUser record);

    int insertSelective(HiddenUser record);

    List<HiddenUser> selectByExample(HiddenUserExample example);

    HiddenUser selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HiddenUser record, @Param("example") HiddenUserExample example);

    int updateByExample(@Param("record") HiddenUser record, @Param("example") HiddenUserExample example);

    int updateByPrimaryKeySelective(HiddenUser record);

    int updateByPrimaryKey(HiddenUser record);
}