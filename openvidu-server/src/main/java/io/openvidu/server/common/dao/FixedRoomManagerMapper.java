package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.FixedRoomManager;
import io.openvidu.server.common.pojo.FixedRoomManagerExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface FixedRoomManagerMapper {
    long countByExample(FixedRoomManagerExample example);

    int deleteByExample(FixedRoomManagerExample example);

    int deleteByPrimaryKey(Long id);

    int insert(FixedRoomManager record);

    int insertSelective(FixedRoomManager record);

    List<FixedRoomManager> selectByExample(FixedRoomManagerExample example);

    FixedRoomManager selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") FixedRoomManager record, @Param("example") FixedRoomManagerExample example);

    int updateByExample(@Param("record") FixedRoomManager record, @Param("example") FixedRoomManagerExample example);

    int updateByPrimaryKeySelective(FixedRoomManager record);

    int updateByPrimaryKey(FixedRoomManager record);
}