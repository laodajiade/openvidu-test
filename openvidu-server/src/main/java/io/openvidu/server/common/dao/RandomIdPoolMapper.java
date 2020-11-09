package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.RandomIdPool;
import io.openvidu.server.common.pojo.RandomIdPoolExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RandomIdPoolMapper {
    long countByExample(RandomIdPoolExample example);

    int deleteByExample(RandomIdPoolExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(RandomIdPool record);

    int insertSelective(RandomIdPool record);

    List<RandomIdPool> selectByExample(RandomIdPoolExample example);

    RandomIdPool selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") RandomIdPool record, @Param("example") RandomIdPoolExample example);

    int updateByExample(@Param("record") RandomIdPool record, @Param("example") RandomIdPoolExample example);

    int updateByPrimaryKeySelective(RandomIdPool record);

    int updateByPrimaryKey(RandomIdPool record);

    @Select("select * from sd_random_id_pool order by id asc limit 1")
    @ResultMap(value = "BaseResultMap")
    RandomIdPool getFirstId();
}