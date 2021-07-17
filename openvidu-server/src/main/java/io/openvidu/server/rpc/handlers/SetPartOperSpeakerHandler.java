package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;

/**
 * @author even
 * @date 2020/8/18 19:31
 */
@Slf4j
@Service
public class SetPartOperSpeakerHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_PART_OPER_SPEAKER_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_SOURCE_ID_PARAM);
        String allowPartOperSpeaker = getStringParam(request, ProtocolElements.SETPARTOPERSPEAKER_ALLOWPARTOPERSPEAKER_PARAM);

        Session session = sessionManager.getSession(sessionId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        // verify operate permission
        Participant operatePart = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
        if (!operatePart.getRole().isController()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        SessionPreset preset = session.getPresetInfo();
        preset.setAllowPartOperSpeaker(SessionPresetEnum.valueOf(allowPartOperSpeaker));

        Set<Participant> participants = session.getParticipants();
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SET_PART_OPER_SPEAKER_METHOD, request.getParams());
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
