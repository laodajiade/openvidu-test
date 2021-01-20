package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.ScrollingBannersConfig;
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
 * @date 2021/1/20 19:25
 */
@Slf4j
@Service
public class SetScrollingBannersHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.SET_SCROLLING_BANNERS_ROOMID_PARAM);
        String operation = getStringParam(request, ProtocolElements.SET_SCROLLING_BANNERS_OPERATION_PARAM);
        JsonElement config = getParam(request, ProtocolElements.SET_SCROLLING_BANNERS_CONFIG_PARAM);
        ScrollingBannersConfig scrollingBannersConfig = JSONObject.parseObject(config.toString(),ScrollingBannersConfig.class);

        Session session = sessionManager.getSession(roomId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        // verify operate permission
        Participant operatePart = session.getPartByPrivateIdAndStreamType(rpcConnection.getParticipantPrivateId(), StreamType.MAJOR);
        if (!operatePart.getRole().isController()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        SessionPreset preset = session.getPresetInfo();
        preset.setScrollingBannersInRoom(SessionPresetEnum.valueOf(operation));
        preset.setScrollingBannersConfig(scrollingBannersConfig);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        Set<Participant> participants = session.getParticipants();
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                if (Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SET_SCROLLING_BANNERS_METHOD, request.getParams());
                }
            }
        }
    }

}
