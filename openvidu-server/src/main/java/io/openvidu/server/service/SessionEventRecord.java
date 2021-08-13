package io.openvidu.server.service;

import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import lombok.extern.slf4j.Slf4j;

/**
 * SESSION-EVENT {timestamp} {EVENT} {sessionId}({ruid[-8:]})
 */
@Slf4j
public class SessionEventRecord {


    /**
     * SESSION-EVENT {timestamp} {publishVideo} {sessionId}({ruid[-8:]}) {part_uuid} {streamId}
     */
    public static void newPublisher(Participant participant, Session session, String streamId) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {}", System.currentTimeMillis()
                    , "publishVideo", session.getSessionId(), subRuid(session.getRuid()),
                    participant.getUuid(), streamId);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {stopPublishVideo} {sessionId}({ruid[-8:]}) {part_uuid} {streamId} {reason}
     */
    public static void stopPublisher(Participant participant, Session session, String streamId, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {} {}",
                    System.currentTimeMillis(), "stopPublishVideo", session.getSessionId(), subRuid(session.getRuid()),
                    participant.getUuid(), streamId, reason);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {subscribeVideo} {sessionId}({ruid[-8:]}) {part_uuid} {streamId}
     */
    public static void newSubscriber(Participant participant, Session session, String streamId) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {}",
                    System.currentTimeMillis(), "subscribeVideo", session.getSessionId(), subRuid(session.getRuid()),
                    participant.getUuid(), streamId);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {cancelSubscribeVideo} {sessionId}({ruid[-8:]}) {part_uuid} {streamId}
     */
    public static void stopSubscriber(Participant participant, Session session, String streamId, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {} {}",
                    System.currentTimeMillis(), "stopSubscribeVideo", session.getSessionId(), subRuid(session.getRuid()),
                    participant.getUuid(), streamId, reason);
        }
    }


    private static String subRuid(String ruid) {
        if (ruid == null || ruid.length() == 0) {
            return "";
        }
        if (ruid.length() <= 6) {
            return ruid;
        }
        return ruid.substring(ruid.length() - 6);
    }
}
