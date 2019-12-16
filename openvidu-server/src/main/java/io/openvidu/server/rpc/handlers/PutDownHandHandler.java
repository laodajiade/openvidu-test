package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 16:57
 */
@Slf4j
@Service
public class PutDownHandHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM);
        String targetId = request.getParams().has(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM) ?
                request.getParams().get(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM).getAsString() : null;
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!StringUtils.isEmpty(targetId)) {
            Participant raisePart = sessionManager.getParticipants(sessionId).stream().filter(participant -> Objects.equals(targetId,
                    participant.getUserId()) && Objects.equals(StreamType.MAJOR, participant.getStreamType()))
                    .findFirst().orElse(null);
            if (!Objects.isNull(raisePart)) {
                raisePart.setHandStatus(ParticipantHandStatus.down);
            } else return;
        } else {
            participants.forEach(part -> part.setHandStatus(ParticipantHandStatus.down));
        }

        User user = userMapper.selectByPrimaryKey(Long.valueOf(sourceId));
        String	serialNumber = rpcConnection.getSerialNumber();
        if (!Objects.isNull(serialNumber))
            log.info("serialNumber", serialNumber);
        // device info.
        Device device=null;
        if (!StringUtils.isEmpty(serialNumber)) {
            DeviceSearch deviceSearch = new DeviceSearch();
            deviceSearch.setSerialNumber(serialNumber);
            device = deviceMapper.selectBySearchCondition(deviceSearch);
        }
        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.PUT_DOWN_USERNAME_PARAM, user.getUsername());
        if (!StringUtils.isEmpty(device))
            params.addProperty(ProtocolElements.PUT_DOWN_APPSHOW_NAME_PARAM, device.getDeviceName());
        if (!StringUtils.isEmpty(targetId)) {
            params.addProperty(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM, targetId);
            int raiseHandNum = 0;
            for (Participant p : sessionManager.getParticipants(sessionId)) {
                if (Objects.equals(ParticipantHandStatus.up, p.getHandStatus()) &&
                        Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                    ++raiseHandNum;
                }
            }
            params.addProperty(ProtocolElements.PUT_DOWN_HAND_RAISEHAND_NUMBER_PARAM, raiseHandNum);
        }
        participants.forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.PUT_DOWN_HAND_METHOD, params));

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
