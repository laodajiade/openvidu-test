package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.kurento.core.KurentoSession;
import org.kurento.client.MediaEvent;

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

}
