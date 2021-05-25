package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.UserCorpInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    List<Corporation> listCorpExpire();

    List<Corporation> listByCorpRecordExpireDay(@Param("expire") String expire);

    List<Corporation> selectAllCorp();

    int updateCorpRemainderDuration(Corporation record);

    UserCorpInfo getUserCorpInfo(String uuid);

    /**
     * 查询企业是否充值过
     *
     * @param project
     * @return
     */
    boolean selectIsRechargeConcurrent(String project);

    /**
     * 是否在并发服务期间
     */
    default boolean isConcurrentServiceDuration(long corpId) {
        Corporation corporation = selectByPrimaryKey(corpId);
        return isConcurrentServiceDuration(corporation);
    }

    /**
     * 是否在并发服务期间
     */
    default boolean isConcurrentServiceDuration(Corporation corporation) {
        if (corporation == null) {
            return false;
        }
        return corporation.getActivationDate().isBefore(LocalDateTime.now()) && corporation.getExpireDate().isAfter(LocalDateTime.now());
    }

}
