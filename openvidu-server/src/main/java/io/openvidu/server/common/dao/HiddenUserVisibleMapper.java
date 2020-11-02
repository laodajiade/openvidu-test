package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.HiddenUserVisible;
import io.openvidu.server.common.pojo.HiddenUserVisibleExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface HiddenUserVisibleMapper {
    long countByExample(HiddenUserVisibleExample example);

    int deleteByExample(HiddenUserVisibleExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HiddenUserVisible record);

    int insertSelective(HiddenUserVisible record);

    List<HiddenUserVisible> selectByExample(HiddenUserVisibleExample example);

    HiddenUserVisible selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HiddenUserVisible record, @Param("example") HiddenUserVisibleExample example);

    int updateByExample(@Param("record") HiddenUserVisible record, @Param("example") HiddenUserVisibleExample example);

    int updateByPrimaryKeySelective(HiddenUserVisible record);

    int updateByPrimaryKey(HiddenUserVisible record);

    default HiddenUserVisible selectOne(HiddenUserVisibleExample example) {
        List<HiddenUserVisible> list = selectByExample(example);
        return list.size() == 0 ? null : list.get(0);
    }
}