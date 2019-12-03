package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:42
 */
@Slf4j
@Service
public class UnsubscribeFromVideoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "unsubscribe");
        } catch (OpenViduException e) {
            return;
        }

        String senderName = getStringParam(request, ProtocolElements.UNSUBSCRIBEFROMVIDEO_SENDER_PARAM);
        String streamModeStr = getStringOptionalParam(request, ProtocolElements.UNSUBSCRIBEFROMVIDEO_STREAMMODE_PARAM);
        StreamModeEnum streamModeEnum = !StringUtils.isEmpty(streamModeStr) ? StreamModeEnum.valueOf(streamModeStr) : null;
        senderName = senderName.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamModeEnum) ?
                senderName.lastIndexOf("_") : senderName.indexOf("_"));
        sessionManager.unsubscribe(participant, senderName, request.getId());
    }
}
