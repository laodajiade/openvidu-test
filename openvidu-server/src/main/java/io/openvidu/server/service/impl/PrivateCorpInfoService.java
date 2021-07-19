package io.openvidu.server.service.impl;

import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.DongleInfo;
import io.openvidu.server.common.pojo.UserCorpInfo;
import io.openvidu.server.job.DognelScheduledExecutor;
import io.openvidu.server.service.CorpInfoService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: prepaid-platform
 * @description:
 * @author: WuBing
 * @create: 2021-06-28 15:18
 **/
public class PrivateCorpInfoService implements CorpInfoService {


    @Autowired
    CorporationMapper corporationMapper;

    @Override
    public int deleteByPrimaryKey(Long id) {
        return corporationMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(Corporation record) {
        return corporationMapper.insert(record);
    }

    @Override
    public int insertSelective(Corporation record) {
        return corporationMapper.insertSelective(record);
    }

    @Override
    public Corporation selectByPrimaryKey(Long id) {
        Corporation corporation = corporationMapper.selectByPrimaryKey(id);
        return converPrivate(corporation);
    }

    @Override
    public Corporation selectByCorpProject(String project) {
        Corporation corporation = corporationMapper.selectByCorpProject(project);
        return converPrivate(corporation);
    }

    @Override
    public int updateByPrimaryKeySelective(Corporation record) {
        return corporationMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(Corporation record) {
        return corporationMapper.updateByPrimaryKey(record);
    }

    @Override
    public List<Corporation> listCorpExpire() {
        if (LocalDateTime.now().isAfter(DognelScheduledExecutor.dongleInfo.getEndDate())) {
            return corporationMapper.selectAllCorp();
        }

        return new ArrayList<Corporation>();
    }

    @Override
    public List<Corporation> listByCorpRecordExpireDay(String expire) {

        if (DognelScheduledExecutor.dongleInfo != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime time = DognelScheduledExecutor.dongleInfo.getEndDate();
            String localTime = df.format(time);
            if (expire.equals(localTime)) {
                return corporationMapper.selectAllCorp();
            }
        }
        return new ArrayList<Corporation>();

    }

    @Override
    public List<Corporation> selectAllCorp() {
        return corporationMapper.selectAllCorp();
    }

    @Override
    public int updateCorpRemainderDuration(Corporation record) {
        return corporationMapper.updateCorpRemainderDuration(record);
    }

    @Override
    public UserCorpInfo getUserCorpInfo(String uuid) {
        return corporationMapper.getUserCorpInfo(uuid);
    }

    @Override
    public boolean selectIsRechargeConcurrent(String project) {
        boolean flag = corporationMapper.selectIsRechargeConcurrent(project);
        return flag;
    }

    @Override
    public boolean isConcurrentServiceDuration(long corpId) {
        Corporation corporation = corporationMapper.selectByPrimaryKey(corpId);
        corporation = converPrivate(corporation);
        return corporation.getActivationDate().isBefore(LocalDateTime.now()) && corporation.getExpireDate().isAfter(LocalDateTime.now());
    }


    private Corporation converPrivate(Corporation corporation) {
        if (corporation == null) {
            return null;
        }
        DongleInfo dongleInfo = DognelScheduledExecutor.dongleInfo;
        corporation.setCapacity(dongleInfo.getMaxConcurrentNum());
        corporation.setActivationDate(corporation.getActivationDate());
        corporation.setExpireDate(corporation.getExpireDate());
        corporation.setRecordingActivationDate(corporation.getActivationDate());
        corporation.setRecordingExpireDate(corporation.getExpireDate());
        return corporation;
    }

}
