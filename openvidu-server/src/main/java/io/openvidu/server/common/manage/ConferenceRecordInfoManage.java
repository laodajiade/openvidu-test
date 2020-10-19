package io.openvidu.server.common.manage;

import com.github.pagehelper.Page;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.User;

import java.util.List;

public interface ConferenceRecordInfoManage {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceRecordInfo record);

    int insertSelective(ConferenceRecordInfo record);

    ConferenceRecordInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceRecordInfo record);

    int updateByPrimaryKey(ConferenceRecordInfo record);

    Page<ConferenceRecordInfo> getPageListBySearch(ConferenceRecordSearch search);

    long selectConfRecordsInfoCountByCondition(ConferenceRecordSearch condition);

    void deleteConferenceRecordInfo(ConferenceRecordInfo conferenceRecordInfo, User operator);

    void playbackConferenceRecord(ConferenceRecordInfo conferenceRecordInfo, User operator);

    void downloadConferenceRecord(ConferenceRecordInfo conferenceRecordInfo, User operator);

    List<ConferenceRecordInfo> selectByIds(List<Long> ids);

}
