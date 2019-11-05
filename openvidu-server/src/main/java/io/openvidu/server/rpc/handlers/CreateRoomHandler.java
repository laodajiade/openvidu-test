package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * @author chosongi
 * @date 2019/11/5 16:18
 */
@Slf4j
@Component
public class CreateRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
        String password = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_PASSWORD_PARAM);
        if (StringUtils.isEmpty(sessionId)) {
            sessionId = generalRoomId();
        }

        if (isExistingRoom(sessionId, rpcConnection.getUserUuid())) {
            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
            return ;
        }

        if (sessionManager.isNewSessionIdValid(sessionId)) {
            JsonObject respJson = new JsonObject();
            respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
            // save conference info
            Conference conference = new Conference();
            conference.setRoomId(sessionId);
            conference.setPassword(StringUtils.isEmpty(password) ? null : password);
            conference.setStatus(1);
            conference.setStartTime(new Date());
            int insertResult = conferenceMapper.insert(conference);

            // setPresetInfo.
            String roomSubject = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SUBJECT_PARAM);
            String micStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MIC_STATUS_PARAM);
            String videoStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_VIDEO_STATUS_PARAM);
            String sharePowerInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SHARE_POWER_PARAM);
            Integer roomCapacity = getIntOptionalParam(request, ProtocolElements.CREATE_ROOM_ROOM_CAPACITY_PARAM);
            Float roomDuration = getFloatOptionalParam(request, ProtocolElements.CREATE_ROOM_DURATION_PARAM);
            String useIdInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_USE_ID_PARAM);
            String allowPartOperMic = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_MIC_PARAM);
            String allowPartOperShare = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM);

            SessionPreset preset = new SessionPreset(micStatusInRoom, videoStatusInRoom, sharePowerInRoom,
                    roomSubject, roomCapacity, roomDuration, useIdInRoom, allowPartOperMic, allowPartOperShare);
            sessionManager.setPresetInfo(sessionId, preset);

            // store this inactive session
            sessionManager.storeSessionNotActiveWhileRoomCreated(sessionId);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
        } else {
            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
        }
    }
}
