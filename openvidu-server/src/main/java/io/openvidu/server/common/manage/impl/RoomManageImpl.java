package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.core.Participant;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author chosongi
 * @date 2020/5/27 16:37
 */
@Service
public class RoomManageImpl implements RoomManage {

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;


    @Override
    public List<Conference> getAllRoomsOfCorp(ConferenceSearch conferenceSearch) {
        return conferenceMapper.selectBySearchCondition(conferenceSearch);
    }

    @Override
    public List<ConferencePartHistory> getConfHistoryRecordsByCondition(ConferencePartHistory search) {
        return conferencePartHistoryMapper.selectByCondition(search);
    }

    @Override
    public void storePartHistory(Participant participant, Conference conference) {
        ConferencePartHistory history = new ConferencePartHistory();
        history.setRuid(conference.getRuid());
        history.setUserId(Long.valueOf(participant.getUserId()));
        history.setUuid(participant.getUuid());
        history.setUsername(participant.getUsername());
        history.setUserType(participant.getUserType().getType());
        history.setTerminalType(participant.getClientType());
        history.setStatus(ParticipantStatusEnum.PROCESS.getStatus());
        history.setStartTime(new Date());
        history.setAccessKey(conference.getProject());
        history.setProject(conference.getProject());
        conferencePartHistoryMapper.insert(history);

        // save part info in cache
        /*Map<String, Object> partInfo = new HashMap<>();
        partInfo.put("userId", participant.getUserId());
        partInfo.put("ruid", conference.getRuid());
        partInfo.put("roomId", participant.getSessionId());
        partInfo.put("role", participant.getRole().name());
        partInfo.put("publicId", participant.getParticipantPublicId());
        partInfo.put("shareStatus", participant.getShareStatus().name());
        partInfo.put("speakerStatus", participant.getSpeakerStatus().name());
        partInfo.put("handStatus", participant.getHandStatus().name());
        cacheManage.savePartInfo(participant.getUuid(), partInfo);*/
    }

    @Override
    public void updatePartHistory(String ruid, String uuid, Long createdAt) {
        ConferencePartHistory update = new ConferencePartHistory();
        update.setRuid(ruid);
        update.setUuid(uuid);

        update.setStatus(ParticipantStatusEnum.LEAVE.getStatus());
        Date endTime = new Date();
        update.setEndTime(endTime);
        int duration = (int) ((endTime.getTime() - createdAt) / 60000);
        update.setDuration(duration == 0 ? 1 : duration);
        conferencePartHistoryMapper.updatePartHistroy(update);
    }

    @Override
    public Conference getConferenceByRuid(String ruid) {
        return conferenceMapper.selectByRuid(ruid);
    }
}
