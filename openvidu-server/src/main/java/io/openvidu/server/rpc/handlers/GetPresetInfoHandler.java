package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author chosongi
 * @date 2019/11/5 20:09
 */
@Slf4j
@Service
public class GetPresetInfoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.GET_PRESET_INFO_ID_PARAM);
        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        JsonObject params = new JsonObject();

        params.addProperty(ProtocolElements.GET_PRESET_INFO_MIC_STATUS_PARAM, preset.getMicStatusInRoom().name());
        params.addProperty(ProtocolElements.GET_PRESET_INFO_SHARE_POWER_PARAM, preset.getSharePowerInRoom().name());
        params.addProperty(ProtocolElements.GET_PRESET_INFO_VIDEO_STATUS_PARAM, preset.getVideoStatusInRoom().name());
        params.addProperty(ProtocolElements.GET_PRESET_INFO_SUBJECT_PARAM, preset.getRoomSubject());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
