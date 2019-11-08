package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/11/5 20:35
 */
@Slf4j
@Service
public class GetNotFinishedRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject params = new JsonObject();
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        if (Objects.isNull(userInfo)) {
            log.warn("local userInfo:{}", userInfo);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_UNKNOWN_ERROR);
            return ;
        }

        String oldPrivateId = String.valueOf(userInfo.get("reconnect"));
        log.info("userInfo status:{}, reconnect:{}", userInfo.get("status"), userInfo.get("reconnect"));
        if (!Objects.equals(UserOnlineStatusEnum.reconnect.name(), userInfo.get("status")) ||
                StringUtils.isEmpty(oldPrivateId)) {
            log.info("---------------------------------------------");
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
            return ;
        }

        RpcConnection oldRpcConnection = notificationService.getRpcConnection(oldPrivateId);
        try {
            KurentoParticipant p = (KurentoParticipant) sessionManager.getParticipant(oldRpcConnection.getParticipantPrivateId());
            if (!Objects.isNull(p)) {
                // room info
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ID_PARAM, p.getSessionId());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SUBJECT_PARAM, p.getRoomSubject());
                String roomPwd = sessionManager.getSession(p.getSessionId()).getConference().getPassword();
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_PASSWORD_PARAM, !StringUtils.isEmpty(roomPwd) ? roomPwd : "");
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_REMAINTIME_PARAM, p.getSession().getConfRemainTime());

                // participant info.
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ROLE_PARAM, p.getRole().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_AUDIOACTIVE_PARAM, p.isStreaming() && p.getPublisherMediaOptions().isAudioActive());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_VIDEOACTIVE_PARAM, p.isStreaming() && p.getPublisherMediaOptions().isVideoActive());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SPEAKERSTATUS_PARAM, p.getSpeakerStatus().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_HANDSTATUS_PARAM, p.getHandStatus().name());
                params.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SHARESTATUS_PARAM, p.getShareStatus().name());
            }
        } catch (OpenViduException e) {
            log.warn("the privateId:{} not belong any session.", oldPrivateId);
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
