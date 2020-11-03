package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.HiddenSpecifyVisible;
import io.openvidu.server.common.pojo.HiddenSpecifyVisibleExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HiddenSpecifyVisibleMapper {
    long countByExample(HiddenSpecifyVisibleExample example);

    int deleteByExample(HiddenSpecifyVisibleExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HiddenSpecifyVisible record);

    int insertSelective(HiddenSpecifyVisible record);

    List<HiddenSpecifyVisible> selectByExample(HiddenSpecifyVisibleExample example);

    HiddenSpecifyVisible selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HiddenSpecifyVisible record, @Param("example") HiddenSpecifyVisibleExample example);

    int updateByExample(@Param("record") HiddenSpecifyVisible record, @Param("example") HiddenSpecifyVisibleExample example);

    int updateByPrimaryKeySelective(HiddenSpecifyVisible record);

    int updateByPrimaryKey(HiddenSpecifyVisible record);
}