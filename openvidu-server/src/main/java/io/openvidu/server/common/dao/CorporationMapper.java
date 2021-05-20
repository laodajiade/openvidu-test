package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.UserCorpInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CorporationMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Corporation record);

    int insertSelective(Corporation record);

    Corporation selectByPrimaryKey(Long id);

    Corporation selectByCorpProject(String project);

    int updateByPrimaryKeySelective(Corporation record);

    int updateByPrimaryKey(Corporation record);

    List<Corporation> listCorpExpire(@Param("expire") String expire);

    List<Corporation> listByCorpRecordExpireDay(@Param("expire") String expire);

    List<Corporation> selectAllCorp();

    int updateCorpRemainderDuration(Corporation record);

    UserCorpInfo getUserCorpInfo(String uuid);

    /**
     * 查询企业是否充值过
     * @param project
     * @return
     */
    boolean selectIsRechargeConcurrent(String project);
}
