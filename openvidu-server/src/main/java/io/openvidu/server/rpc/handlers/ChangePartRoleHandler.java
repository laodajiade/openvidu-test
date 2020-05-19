package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * @author chosongi
 * @date 2020/5/19 13:53
 */
@Slf4j
@Service
public class ChangePartRoleHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject toOnWallObj = getParam(request, ProtocolElements.CHANGE_PART_ROLE_CHANGED_TO_ON_THE_WALL_PARAM).getAsJsonObject();
        JsonObject toDownWallObj = getParam(request, ProtocolElements.CHANGE_PART_ROLE_CHANGED_TO_DOWN_THE_WALL_PARAM).getAsJsonObject();
        String toOnWallConnId = toOnWallObj.get(ProtocolElements.CHANGE_PART_ROLE_CHANGED_CONNECTION_ID_PARAM).getAsString();
        String toDownWallConnId = toDownWallObj.get(ProtocolElements.CHANGE_PART_ROLE_CHANGED_CONNECTION_ID_PARAM).getAsString();
        Participant toOnWallPart = null, toDownWallPart = null;

        // check parameters valid
        Session session = sessionManager.getSession(rpcConnection.getSessionId());
        Set<Participant> participants = session.getParticipants();
        for (Participant participant : participants) {
            // get toDownWallPart
            if (toDownWallConnId.equals(participant.getParticipantPublicId())
                    && !OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
                toDownWallPart = participant;
            }

            // get toOnWallPart
            if (toOnWallConnId.equals(participant.getParticipantPublicId())
                    && OpenViduRole.SUBSCRIBER.equals(participant.getRole())) {
                toOnWallPart = participant;
            }
        }

        if (Objects.isNull(toDownWallPart) || Objects.isNull(toOnWallPart)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        if (OpenViduRole.MODERATOR.equals(toDownWallPart.getRole()) ||
                !ConferenceModeEnum.MCU.equals(session.getConferenceMode()) ||
                session.getMajorPartSize() != openviduConfig.getMcuMajorPartLimit()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.INVALID_METHOD_CALL);
            return;
        }

        // unPublish to down wall participant stream
        session.dealUpAndDownTheWall(toDownWallPart, toOnWallPart, sessionManager, false);

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
