package io.openvidu.server.common.cache;

import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoParticipant;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */
public interface CacheManage {
    boolean accessTokenEverValid(String userId, String token);

    void removeSession(String sessionId);

    void recordSession(String sessionId);

    void recordPartAndRelation(Participant participant);

    void updateParticipantStreamInfo(KurentoParticipant kParticipant);

    void removeParticipant(Participant participant);
}
