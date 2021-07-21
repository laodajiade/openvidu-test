package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.PushStreamStatusEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

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
        StreamType streamType = StreamType.valueOf(getStringParam(request, ProtocolElements.SET_PUSH_STREAM_STATUS_STREAM_TYPE_PARAM));
        String status = getStringParam(request, ProtocolElements.SETPUSHSTREAMSTATUS_STATUS_PARAM);

        Participant participant = sanityCheckOfSession(rpcConnection);
        Session session = sessionManager.getSession(roomId);

        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kParticipant.getPublisher(streamType);
        if (publisher == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject(), ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
            return;
        }
        publisher.setPushStreamStatus(PushStreamStatusEnum.valueOf(status));

        JsonObject params = request.getParams();
        params.addProperty("publishId", publisher.getStreamId());
        this.notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.SETPUSHSTREAMSTATUS_METHOD, params);

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
