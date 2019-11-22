package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:19
 */
@Slf4j
@Service
public class ReceiveVideoFromHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "subscribe");
        } catch (OpenViduException e) {
            return;
        }

        StreamModeEnum streamMode = StreamModeEnum.valueOf(getStringParam(request,
                ProtocolElements.RECEIVEVIDEO_STREAM_MODE_PARAM));
        String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.SFU_SHARING, streamMode) ?
                senderName.indexOf("_") : senderName.lastIndexOf("_"));
        String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

        KurentoSession kurentoSession = (KurentoSession) this.sessionManager.getSession(rpcConnection.getSessionId());
        if ((Objects.equals(StreamModeEnum.SFU_SHARING, streamMode) || Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING,
                streamMode)) && !kurentoSession.compositeService.isExistSharing()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.NOT_EXIST_SHARING_FLOW);
        }

        sessionManager.subscribe(participant, senderName, streamMode, sdpOffer, request.getId());
    }
}
