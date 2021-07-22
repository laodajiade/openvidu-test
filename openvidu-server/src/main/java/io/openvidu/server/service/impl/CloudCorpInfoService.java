package io.openvidu.server.service.impl;

import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.UserCorpInfo;
import io.openvidu.server.service.CorpInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: prepaid-platform
 * @description:
 * @author: WuBing
 * @create: 2021-06-28 15:23
 **/
@Service
public class CloudCorpInfoService implements CorpInfoService {

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
        return corporationMapper.selectByPrimaryKey(id);
    }

    @Override
    public Corporation selectByCorpProject(String project) {
        return corporationMapper.selectByCorpProject(project);
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
        return corporationMapper.listCorpExpire();
    }

    @Override
    public List<Corporation> listByCorpRecordExpireDay(String expire) {
        List<Corporation> corporations = corporationMapper.listByCorpRecordExpireDay(expire);
        return corporations;
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
        return corporationMapper.selectIsRechargeConcurrent(project);
    }

    @Override
    public boolean isConcurrentServiceDuration(long corpId) {
        return corporationMapper.isConcurrentServiceDuration(corpId);
    }

    @Override
    public boolean isConcurrentServiceDuration(Corporation corporation) {
        return corporationMapper.isConcurrentServiceDuration(corporation);
    }
}
