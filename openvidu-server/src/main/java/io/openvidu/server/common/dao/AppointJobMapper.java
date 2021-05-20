package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AppointJob;
import io.openvidu.server.common.pojo.AppointJobExample;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AppointJobMapper {
    long countByExample(AppointJobExample example);

    int deleteByExample(AppointJobExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(AppointJob record);

    int insertSelective(AppointJob record);

    List<AppointJob> selectByExampleWithBLOBs(AppointJobExample example);

    List<AppointJob> selectByExample(AppointJobExample example);

    AppointJob selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") AppointJob record, @Param("example") AppointJobExample example);

    int updateByExampleWithBLOBs(@Param("record") AppointJob record, @Param("example") AppointJobExample example);

    int updateByExample(@Param("record") AppointJob record, @Param("example") AppointJobExample example);

    int updateByPrimaryKeySelective(AppointJob record);

    int updateByPrimaryKeyWithBLOBs(AppointJob record);

    int updateByPrimaryKey(AppointJob record);


    @Select("select * from sd_appoint_job where start_time <= now() and `status` = 0 order by start_time asc")
    @ResultMap("BaseResultMap")
    List<AppointJob> selectNextJobs();

    @Update("update sd_appoint_job set status = 1,exec_time = now() where id = #{id} and status = 0")
    int doExec(Integer id);

    @Update("update sd_appoint_job set status = 2,exec_time = now()  where id = #{id} and status = 1")
    int finishExec(Integer id);

    @Update("update sd_appoint_job set status = 3,exec_time = now()  where id = #{id}")
    int errorExec(Integer id);

    @Update("update sd_appoint_job set status = 4 where ruid = #{ruid} and schedule_name = #{name}")
    int cancelByRuid(@Param("ruid") String ruid, @Param("name") String name);
}