package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.FixedRoomManager;
import io.openvidu.server.common.pojo.FixedRoomManagerExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Select("select * from sd_fixed_room_manager where user_id = #{userId} and room_id = #{roomId} and deleted = 0")
    @ResultMap("BaseResultMap")
    FixedRoomManager selectByUserId(@Param("userId") Long userId, @Param("roomId") String roomId);

    /**
     * 查询是否是固定会议室管理员
     * @param uuid
     * @return
     */
    boolean selectIsFixedRoomAdmin(String uuid);
}
