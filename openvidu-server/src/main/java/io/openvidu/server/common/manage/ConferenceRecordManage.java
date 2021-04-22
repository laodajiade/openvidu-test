package io.openvidu.server.common.manage;

import com.github.pagehelper.Page;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.RoomRecordSummary;
import io.openvidu.server.kurento.core.KurentoSession;
import org.kurento.client.MediaEvent;

import java.math.BigDecimal;
import java.util.List;

public interface ConferenceRecordManage {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceRecord record);

    int insertSelective(ConferenceRecord record);

    ConferenceRecord selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceRecord record);

    int updateByPrimaryKey(ConferenceRecord record);

    List<ConferenceRecord> getByCondition(ConferenceRecord record);

    void dealConfRecordEvent(KurentoSession session, MediaEvent event);

    Page<RoomRecordSummary> getRoomRecordSummaryByCondition(ConferenceRecordSearch search);

    void clearRoomRecords(String roomId, List<String> ruids, String project);

    void deleteConferenceRecord(List<ConferenceRecordInfo> conferenceRecordInfos);

    List<RoomRecordSummary> getAllRoomRecordSummaryByProject(ConferenceRecordSearch search);

    void updatePreRecordErrorStatus(ConferenceRecord record);

    BigDecimal getCorpRecordStorage(String project);


    /**
     * 根据ruId 查询录制状态
     * @param ruId
     * @return
     */
    ConferenceRecord getByRuIdRecordStatus(String ruId);
}
