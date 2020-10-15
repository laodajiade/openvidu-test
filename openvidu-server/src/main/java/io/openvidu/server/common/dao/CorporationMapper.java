package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Corporation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CorporationMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Corporation record);

    int insertSelective(Corporation record);

    Corporation selectByPrimaryKey(Long id);

    Corporation selectByCorpProject(String project);

    int updateByPrimaryKeySelective(Corporation record);

    int updateByPrimaryKey(Corporation record);

    List<Corporation> listCorpExpire(@Param("expire") String expire);

}
