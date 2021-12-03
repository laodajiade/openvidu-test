package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.FixedRoom;
import io.openvidu.server.common.pojo.FixedRoomExample;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface FixedRoomMapper {
    long countByExample(FixedRoomExample example);

    int deleteByExample(FixedRoomExample example);

    int deleteByPrimaryKey(Long id);

    int insert(FixedRoom record);

    int insertSelective(FixedRoom record);

    List<FixedRoom> selectByExample(FixedRoomExample example);

    FixedRoom selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") FixedRoom record, @Param("example") FixedRoomExample example);

    int updateByExample(@Param("record") FixedRoom record, @Param("example") FixedRoomExample example);

    int updateByPrimaryKeySelective(FixedRoom record);

    int updateByPrimaryKey(FixedRoom record);

    @Select("select a.* from sd_fixed_room a INNER JOIN sd_fixed_room_manager b on a.id = b.fixed_id " +
            " where a.deleted = 0 and b.deleted = 0 and a.status != 0 and b.user_id = #{userId} and a.corp_id = #{corpId}")
    @ResultMap("BaseResultMap")
    List<FixedRoom> getFixedRoomList(@Param("userId") long userId, @Param("corpId") long corpId);

    @Select("select a.* from sd_fixed_room a where a.deleted = 0 and a.status != 0 and a.corp_id = #{corpId}")
    @ResultMap("BaseResultMap")
    List<FixedRoom> getCorpFixedRoomList(@Param("corpId") long corpId);

    @Select("select * from sd_fixed_room where room_id = #{roomId}")
    @ResultMap("BaseResultMap")
    FixedRoom selectByRoomId(String roomId);


    @Select("select * from sd_fixed_room where short_id = #{shortId} and corp_id = #{corpId} and deleted = 0")
    @ResultMap("BaseResultMap")
    FixedRoom selectByShortId(@Param("shortId") String shortId, @Param("corpId")long corpId);

    @Select("select count(*) from sd_fixed_room where status != 0 and deleted=0 and corp_id = #{corpId}")
    int countActivationRoom(long corpId);

    /**
     * 查询固定会议室是否充值过录制存储记录
     * @param roomId
     * @return
     */
    boolean selectISRechargeCardRecord(String roomId);
}