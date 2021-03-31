package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ScrollingBannersConfig;
import io.openvidu.server.common.pojo.UserDto;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author even
 * @date 2021/1/20 21:10
 */
@Slf4j
@Service
public class GetScrollingBannersHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.GET_SCROLLING_BANNERS_ROOMID_PARAM);
        Session session = sessionManager.getSession(roomId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        SessionPreset preset = session.getPresetInfo();
        String operation = preset.getScrollingBannersInRoom().name();
        ScrollingBannersConfig config = preset.getScrollingBannersConfig();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operation", operation);
        if (Objects.nonNull(config)) {
            List<UserDto> userDtos = config.getTargetIds();
            if (CollectionUtils.isEmpty(userDtos)) {
                jsonObject.put("config", config);
            } else {
                List<String> uuidList = userDtos.stream().map(UserDto::getUuid).collect(Collectors.toList());
                if (uuidList.contains(rpcConnection.getUdid())) {
                    jsonObject.put("operation", SessionPresetEnum.on.name());
                    jsonObject.put("config", config);
                } else {
                    jsonObject.put("operation", SessionPresetEnum.off.name());
                    jsonObject.put("config", new ScrollingBannersConfig());
                }
            }
        } else {
            jsonObject.put("config", new ScrollingBannersConfig());
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
