package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ImMsg;
import io.openvidu.server.common.pojo.ImMsgExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ImMsgMapper {
    long countByExample(ImMsgExample example);

    int deleteByExample(ImMsgExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ImMsg record);

    int insertSelective(ImMsg record);

    List<ImMsg> selectByExample(ImMsgExample example);


    ImMsg selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ImMsg record, @Param("example") ImMsgExample example);

    int updateByExample(@Param("record") ImMsg record, @Param("example") ImMsgExample example);

    int updateByPrimaryKeySelective(ImMsg record);

    int updateByPrimaryKey(ImMsg record);

    default ImMsg selectOne(ImMsgExample example) {
        List<ImMsg> list = selectByExample(example);
        return list.size() == 0 ? null : list.get(0);
    }
}