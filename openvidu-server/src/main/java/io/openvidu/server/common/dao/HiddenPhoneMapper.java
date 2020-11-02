package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.HiddenPhone;
import io.openvidu.server.common.pojo.HiddenPhoneExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HiddenPhoneMapper {
    long countByExample(HiddenPhoneExample example);

    int deleteByExample(HiddenPhoneExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HiddenPhone record);

    int insertSelective(HiddenPhone record);

    List<HiddenPhone> selectByExample(HiddenPhoneExample example);

    HiddenPhone selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") HiddenPhone record, @Param("example") HiddenPhoneExample example);

    int updateByExample(@Param("record") HiddenPhone record, @Param("example") HiddenPhoneExample example);

    int updateByPrimaryKeySelective(HiddenPhone record);

    int updateByPrimaryKey(HiddenPhone record);
}