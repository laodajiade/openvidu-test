package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.common.pojo.CorpMcuConfig;
import io.openvidu.server.common.pojo.dto.CorpRoomsSearch;
import io.openvidu.server.core.Participant;

import java.util.List;

/**
 * @author chosongi
 * @date 2020/5/27 16:37
 */
public interface RoomManage {
    List<Conference> getAllRoomsOfCorp(CorpRoomsSearch search);

    List<ConferencePartHistory> getConfHistoryRecordsByCondition(ConferencePartHistory search);

    void storePartHistory(Participant participant, Conference conference);

    void updatePartHistory(String ruid, String uuid, Long createdAt);

    Conference getConferenceByRuid(String ruid);

    CorpMcuConfig getCorpMcuConfig(String project);

    int createMeetingRoom(Conference conference);

    void storeConcurrentNumber(Conference conference);

    List<String> getSubRoomIds(String roomId, Long orgId);
}
