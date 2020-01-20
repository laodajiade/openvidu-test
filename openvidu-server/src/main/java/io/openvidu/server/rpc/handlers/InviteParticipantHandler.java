package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author geedow
 * @date 2019/11/5 20:02
 */
@Slf4j
@Service
public class InviteParticipantHandler extends RpcAbstractHandler {

    @Value("${invite.part.step.size}")
    private int stepSize;

    @Value("${invite.part.delay.time}")
    private int delayTime;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM);
        if (CollectionUtils.isEmpty(targetIds)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId,
                rpcConnection.getParticipantPrivateId()).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        if (!Objects.isNull(sessionManager.getSession(sessionId))) {
            Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
            if ((majorParts.size() + targetIds.size()) > preset.getRoomCapacity()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_LIMITED);
                return;
            }
        }

        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        String username = String.valueOf(userInfo.get("username"));
        String deviceName = userInfo.containsKey("deviceName") ? String.valueOf(userInfo.get("deviceName")) : null;

        // find the target rpc connection by targetId list and notify info.
        int i = 1;
        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, StringUtils.isEmpty(username) ? "" : username);
        if (!StringUtils.isEmpty(deviceName)) {
            params.addProperty(ProtocolElements.INVITE_PARTICIPANT_DEVICE_NAME_PARAM, deviceName);
        }
        Collection<RpcConnection> rpcConnections = this.notificationService.getRpcConnections();
        for (RpcConnection rpcConnect : rpcConnections) {
            if (Objects.isNull(rpcConnect.getUserId())) continue;
            String userId = String.valueOf(rpcConnect.getUserId());
            if (targetIds.contains(userId)) {
                if (i % stepSize == 0) {
                    try {
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, userId);
                this.notificationService.sendNotification(rpcConnect.getParticipantPrivateId(),
                        ProtocolElements.INVITE_PARTICIPANT_METHOD, params);
                i++;
            }
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
