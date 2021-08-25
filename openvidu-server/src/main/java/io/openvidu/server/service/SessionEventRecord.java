package io.openvidu.server.service;

import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Composite;
import org.kurento.client.HubPort;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SESSION-EVENT {timestamp} {EVENT} {sessionId}({ruid[-8:]})
 */
@Slf4j
public class SessionEventRecord {

    private static final Map<String, String> sessionRuid = new ConcurrentHashMap<>();

    /**
     * SESSION-EVENT {timestamp} {publishVideo} {sessionId}({ruid[-8:]}) {part_uuid} {streamId}
     */
    public static void newPublisher(Participant participant, Session session, String streamId) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {}", System.currentTimeMillis()
                    , "publishVideo", session.getSessionId(), subRuid(session),
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
                    System.currentTimeMillis(), "stopPublishVideo", session.getSessionId(), subRuid(session),
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
                    System.currentTimeMillis(), "subscribeVideo", session.getSessionId(), subRuid(session),
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
                    System.currentTimeMillis(), "stopSubscribeVideo", session.getSessionId(), subRuid(session),
                    participant.getUuid(), streamId, reason);
        }
    }


    /**
     * SESSION-EVENT {timestamp} {leaveRoom} {sessionId}({ruid[-8:]}) {part_uuid} {reconnected}
     */
    public static void joinRoom(Session session, Participant participant, boolean reconnected) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {}",
                    System.currentTimeMillis(), "joinRoom", session.getSessionId(), subRuid(session),
                    participant.getUuid(), reconnected);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {leaveRoom} {sessionId}({ruid[-8:]}) {part_uuid} {reason}
     */
    public static void leaveRoom(KurentoSession session, Participant participant, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {} {}",
                    System.currentTimeMillis(), "leaveRoom", session.getSessionId(), subRuid(session),
                    participant.getUuid(), reason);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {startMcu} {sessionId}({ruid[-8:]}) partSize:{} partSize
     */
    public static void startMcu(KurentoSession session, Composite composite, HubPort hubPortOut) {
        if (session == null) {
            return;
        }

        if (log.isInfoEnabled()) {
            String extra = MessageFormat.format("partSize:{0} compositeId:{1}  hubPortOut:{2}", session.getPartSize(),
                    composite.getId(), hubPortOut.getId());
            log.info("SESSION-EVENT {} {} {}({}) {}",
                    System.currentTimeMillis(), "startMcu", session.getSessionId(), subRuid(session), extra);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {endMcu} {sessionId}({ruid[-8:]})
     */
    public static void endMcu(KurentoSession session) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({})",
                    System.currentTimeMillis(), "endMcu", session.getSessionId(), subRuid(session));
        }
    }

    private static String subRuid(Session session) {
        String ruid = session.getRuid();
        if (ruid == null || ruid.length() == 0) {
            return "";
        }
        if (ruid.length() <= 6) {
            return ruid;
        }
        String shortRuid = ruid.substring(ruid.length() - 6);
        sessionRuid.put(session.getSessionId(), shortRuid);
        return shortRuid;
    }

    /**
     * SESSION-EVENT {timestamp} {method} {sessionId}({ruid[-8:]}) {msg}
     */
    public static void other(KurentoSession session, String method, String msg) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {}",
                    System.currentTimeMillis(), method, session.getSessionId(), subRuid(session), msg);
        }
    }

    /**
     * SESSION-EVENT {timestamp} {method} {sessionId}({ruid[-8:]}) {msg}
     */
    public static void other(String sessionId, String method, String msg) {
        if (sessionId == null) {
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {} {} {}({}) {}",
                    System.currentTimeMillis(), method, sessionId, sessionRuid.get(sessionId), msg);
        }
    }
}
