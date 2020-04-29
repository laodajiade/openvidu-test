package io.openvidu.server.core;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * @author chosongi
 * @date 2020/4/29 15:10
 */
@Slf4j
@Component
public class TempCompensateForReconnect {

    @Resource
    private SessionManager sessionManager;

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Resource(name = "reconnectCompensationScheduler")
    private TaskScheduler taskScheduler;

    @Resource
    private CacheManage cacheManage;

    private static final int EVICT_PART_NOT_RECONNECT_DURATION = 90;

    public void addReconnectCheck(Session session, Participant participant) {
        UnReconnectPartEvictHandler unReconnectPartEvictHandler = new UnReconnectPartEvictHandler(session.getSessionId(),
                session.getStartTime(), participant.getParticipantPrivateId(), participant.getParticipantPublicId(),
                participant.getCreatedAt());
        unReconnectPartEvictHandler.evictIfNecessary();
    }

    private class UnReconnectPartEvictHandler {
        private String sessionId;
        private Long sessionStartTime;
        private String partPrivateId;
        private String partPublicId;
        private Long partCreateTime;

        UnReconnectPartEvictHandler(String sessionId, Long startTime, String partPrivateId, String partPublicId,
                                    Long partCreateTime) {
            this.sessionId = sessionId;
            this.sessionStartTime = startTime;
            this.partPrivateId = partPrivateId;
            this.partPublicId = partPublicId;
            this.partCreateTime = partCreateTime;
        }

        private ScheduledFuture<?> evictTask;
        private Runnable evictThread = new Runnable() {
            @Override
            public void run() {
                Session session;
                if (Objects.nonNull(session = sessionManager.getSession(sessionId))
                        && Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)
                        && sessionStartTime.compareTo(session.getStartTime()) == 0) {
                    Participant participant;
                    if (Objects.nonNull(participant = session.getParticipantByPublicId(partPublicId))
                            && partCreateTime.compareTo(participant.getCreatedAt()) == 0) {
                        if (!OpenViduRole.MODERATOR.equals(participant.getRole())) {
                            String speakerId = null, moderatorId = null;
                            Set<Participant> participants = sessionManager.getParticipants(sessionId);
                            for (Participant part : participants) {
                                if (StreamType.MAJOR.equals(part.getStreamType())) {
                                    if (part.getRole().equals(OpenViduRole.MODERATOR)) {
                                        moderatorId = part.getParticipantPublicId();
                                    }
                                    if (Objects.equals(ParticipantHandStatus.speaker, part.getHandStatus())) {
                                        speakerId = part.getParticipantPublicId();
                                    }
                                }
                            }

                            log.info("Before CompensateForReconnect majorShareMixLinkedArr:{}",
                                    session.getMajorShareMixLinkedArr().toString());
                            session.leaveRoomSetLayout(participant, !Objects.equals(speakerId, participant.getParticipantPublicId()) ?
                                    speakerId : moderatorId);

                            log.info("After CompensateForReconnect majorShareMixLinkedArr:{}",
                                    session.getMajorShareMixLinkedArr().toString());
                            session.invokeKmsConferenceLayout();

                            log.info("CompensateForReconnect evict part:{}", partPublicId);
                            sessionManager.evictParticipant(participant, null, null,
                                    EndReason.sessionClosedByServer);

                            for (Participant participant1 : participants) {
                                if (Objects.equals(StreamType.MAJOR, participant1.getStreamType())
                                        && !Objects.equals(participant, participant1)) {
                                    rpcNotificationService.sendNotification(participant1.getParticipantPrivateId(),
                                            ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, session.getLayoutNotifyInfo());
                                }
                            }

                        } else {
                            sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
                                if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                                    return;
                                }
                                rpcNotificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
                                RpcConnection rpcConnect = rpcNotificationService.getRpcConnection(p.getParticipantPrivateId());
                                if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                                    cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
                                }});

                            sessionManager.updateConferenceInfo(sessionId);
                            sessionManager.closeSession(sessionId, EndReason.closeSessionByModerator);
                        }

                        log.info("CompensateForReconnect clean up the unused rpc connection:{}", partPrivateId);
                        rpcNotificationService.closeRpcSession(partPrivateId);
                    }
                }

                evictTask.cancel(false);
            }
        };

        void evictIfNecessary() {
            evictTask = taskScheduler.schedule(evictThread,
                    new Date(System.currentTimeMillis() + EVICT_PART_NOT_RECONNECT_DURATION * 1000 ));
        }
    }
}
