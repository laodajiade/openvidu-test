package io.openvidu.server.common.manage.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.openvidu.server.common.dao.ConferenceRecordInfoMapper;
import io.openvidu.server.common.dao.ConferenceRecordMapper;
import io.openvidu.server.common.enums.ConferenceRecordOperationTypeEnum;
import io.openvidu.server.common.manage.ConferenceRecordInfoManage;
import io.openvidu.server.common.manage.ConferenceRecordLogManage;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ConferenceRecordInfoManageImpl implements ConferenceRecordInfoManage {
    @Resource
    private ConferenceRecordInfoMapper conferenceRecordInfoMapper;
    @Resource
    private ConferenceRecordMapper conferenceRecordMapper;
    @Resource
    private ConferenceRecordLogManage conferenceRecordLogManage;

    @Override
    public int deleteByPrimaryKey(Long id) {
        return conferenceRecordInfoMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(ConferenceRecordInfo record) {
        return conferenceRecordInfoMapper.insert(record);
    }

    @Override
    public int insertSelective(ConferenceRecordInfo record) {
        return conferenceRecordInfoMapper.insertSelective(record);
    }

    @Override
    public ConferenceRecordInfo selectByPrimaryKey(Long id) {
        return conferenceRecordInfoMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(ConferenceRecordInfo record) {
        return conferenceRecordInfoMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(ConferenceRecordInfo record) {
        return conferenceRecordInfoMapper.updateByPrimaryKey(record);
    }

    @Override
    public Page<ConferenceRecordInfo> getPageListBySearch(ConferenceRecordSearch search) {
        return PageHelper.startPage(search.getPageNum(), search.getSize(),
                search.getFilter().getFilter() + " " + search.getSort().name())
                .doSelectPage(() -> conferenceRecordInfoMapper.getPageListBySearch(search.getRuidList()));
    }

    @Override
    public long selectConfRecordsInfoCountByCondition(ConferenceRecordSearch condition) {
        return conferenceRecordInfoMapper.selectConfRecordsInfoCountByCondition(condition);
    }

    @Override
    @Transactional
    public void deleteConferenceRecordInfo(ConferenceRecordInfo conferenceRecordInfo, User operator) {
        // ????????????????????????
        this.deleteByPrimaryKey(conferenceRecordInfo.getId());
        // ??????????????????????????????
        conferenceRecordMapper.decreaseConferenceRecordCountByRuid(conferenceRecordInfo.getRuid());
        // ???????????????????????????recordCount???0???????????????
        conferenceRecordMapper.deleteUselessRecord();
        // ??????????????????
        conferenceRecordLogManage.insertOperationLog(conferenceRecordInfo, ConferenceRecordOperationTypeEnum.DELETE.name().toLowerCase(), operator);
    }

    @Override
    @Transactional
    public void playbackConferenceRecord(ConferenceRecordInfo conferenceRecordInfo, User operator) {
        // ????????????????????????
        conferenceRecordInfo.setAccessTime(new Date());
        this.updateByPrimaryKeySelective(conferenceRecordInfo);
        // ??????????????????
        conferenceRecordLogManage.insertOperationLog(conferenceRecordInfo, ConferenceRecordOperationTypeEnum.PLAYBACK.name().toLowerCase(), operator);
    }

    @Override
    public void downloadConferenceRecord(ConferenceRecordInfo conferenceRecordInfo, User operator) {
        // ??????????????????
        conferenceRecordLogManage.insertOperationLog(conferenceRecordInfo, ConferenceRecordOperationTypeEnum.DOWNLOAD.name().toLowerCase(), operator);
    }

    @Override
    public List<ConferenceRecordInfo> selectByIds(List<Long> ids) {
        return CollectionUtils.isEmpty(ids) ? Collections.emptyList() : conferenceRecordInfoMapper.selectByIds(ids);
    }

}
