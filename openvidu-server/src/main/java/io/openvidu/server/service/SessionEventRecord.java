package io.openvidu.server.service;

import com.alibaba.fastjson.JSONObject;
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
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {publishVideo} {timestamp} {part_uuid} {streamId}
     */
    public static void newPublisher(Participant participant, Session session, String streamId) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {} {}", session.getSessionId(), subRuid(session), "publishVideo",
                    System.currentTimeMillis(), participant.getUuid(), streamId);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {stopPublishVideo} {timestamp} {part_uuid} {streamId} {reason}
     */
    public static void stopPublisher(Participant participant, Session session, String streamId, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {} {} {}", session.getSessionId(), subRuid(session), "stopPublishVideo",
                    System.currentTimeMillis(), participant.getUuid(), streamId, reason);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {subscribeVideo} {timestamp} {part_uuid} {streamId}
     */
    public static void newSubscriber(Participant participant, Session session, String streamId) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {} {}",
                    session.getSessionId(), subRuid(session), "subscribeVideo", System.currentTimeMillis(),
                    participant.getUuid(), streamId);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {cancelSubscribeVideo} {timestamp} {part_uuid} {streamId}
     */
    public static void stopSubscriber(Participant participant, Session session, String streamId, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {} {} {}",
                    session.getSessionId(), subRuid(session), "stopSubscribeVideo", System.currentTimeMillis(),
                    participant.getUuid(), streamId, reason);
        }
    }


    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {createRoom} {timestamp}
     */
    public static void createRoom(Session session) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {}",
                    session.getSessionId(), subRuid(session), "createRoom", System.currentTimeMillis());
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {closeRoom} {timestamp} {reason}
     */
    public static void closeRoom(Session session, EndReason reason) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {}",
                    session.getSessionId(), subRuid(session), "closeRoom", System.currentTimeMillis(), reason.name());
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {joinRoom} {timestamp} {part_uuid} {json_msg} {reconnected}
     */
    public static void joinRoom(Session session, Participant participant, boolean reconnected) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            JSONObject json = new JSONObject();
            json.put("role", participant.getRole());
            json.put("order", participant.getOrder());
            json.put("micStatus", participant.getMicStatus());
            json.put("videoStatus", participant.getVideoStatus());

            log.info("SESSION-EVENT {}({}) {} {} {} {} {}",
                    session.getSessionId(), subRuid(session), "joinRoom", System.currentTimeMillis(),
                    participant.getUuid(), json.toString(), reconnected);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {leaveRoom} {timestamp} {part_uuid} {reason}
     */
    public static void leaveRoom(KurentoSession session, Participant participant, EndReason reason) {
        if (session == null || participant == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {} {}",
                    session.getSessionId(), subRuid(session), "leaveRoom", System.currentTimeMillis(),
                    participant.getUuid(), reason);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {startMcu} {timestamp} partSize:{} partSize
     */
    public static void startMcu(KurentoSession session, Composite composite, HubPort hubPortOut) {
        if (session == null) {
            return;
        }

        if (log.isInfoEnabled()) {
            String extra = MessageFormat.format("partSize:{0} compositeId:{1}  hubPortOut:{2}", session.getPartSize(),
                    composite.getId(), hubPortOut.getId());
            log.info("SESSION-EVENT {}({}) {} {} {}",
                    session.getSessionId(), subRuid(session), "startMcu", System.currentTimeMillis(), extra);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {endMcu} {timestamp}
     */
    public static void endMcu(KurentoSession session) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {}",
                    session.getSessionId(), subRuid(session), "endMcu", System.currentTimeMillis());
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
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {method} {timestamp} {msg}
     */
    public static void other(KurentoSession session, String method, String msg) {
        if (session == null) {
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("SESSION-EVENT {}({}) {} {} {}",
                    session.getSessionId(), subRuid(session), method, System.currentTimeMillis(), msg);
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
            log.info("SESSION-EVENT {}({}) {} {} {}",
                    System.currentTimeMillis(), method, sessionId, sessionRuid.get(sessionId), msg);
        }
    }

    /**
     * SESSION-EVENT {sessionId}({ruid[-8:]}) {method} {timestamp} {args}
     */
    public static void other(String sessionId, String method, String... args) {
        if (sessionId == null) {
            return;
        }

        if (log.isInfoEnabled()) {
            String msg = String.join(", ", args);
            log.info("SESSION-EVENT {}({}) {} {} {}",
                    sessionId, sessionRuid.get(sessionId), method, System.currentTimeMillis(), msg);
        }
    }
}
