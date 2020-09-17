package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.PushStreamStatusEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author even
 * @date 2020/9/16 17:02
 */
@Slf4j
@Service
public class SetPushStreamStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.SETPUSHSTREAMSTATUS_ROOMID_PARAM);
        String uuid = getStringParam(request, ProtocolElements.SETPUSHSTREAMSTATUS_UUID_PARAM);
        String streamId = getStringParam(request, ProtocolElements.SETPUSHSTREAMSTATUS_STREAMID_PARAM);
        String status = getStringParam(request, ProtocolElements.SETPUSHSTREAMSTATUS_STATUS_PARAM);
        Session session = sessionManager.getSession(roomId);
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        Participant participant = session.getParticipantByUUID(uuid);
        participant.setPushStreamStatus(PushStreamStatusEnum.valueOf(status));
        //send notify
        session.getParticipants().forEach(part -> {
            if (StreamType.MAJOR.equals(part.getStreamType())) {
                this.notificationService.sendNotification(part.getParticipantPrivateId(), ProtocolElements.SETPUSHSTREAMSTATUS_METHOD, request.getParams());
            }
        });
    }
}
