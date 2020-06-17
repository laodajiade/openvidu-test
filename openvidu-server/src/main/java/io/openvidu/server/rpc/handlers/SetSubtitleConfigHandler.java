package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.SubtitleConfigEnum;
import io.openvidu.server.common.enums.SubtitleLanguageEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/6/17 10:02
 */
@Service
public class SetSubtitleConfigHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SETSUBTITLECONFIG_ROOMID_PARAM);
        SubtitleConfigEnum subtitleConfig = SubtitleConfigEnum.valueOf(getStringParam(request, ProtocolElements.SETSUBTITLECONFIG_OPERATION_PARAM));
        SubtitleLanguageEnum language = SubtitleLanguageEnum.valueOf(getStringParam(request, ProtocolElements.SETSUBTITLECONFIG_SOURCELANGUAGE_PARAM));

        // check request ever from moderator
        Participant participant;
        Session session = sessionManager.getSession(sessionId);
        if (Objects.isNull(session) || Objects.isNull(participant = session.getPartByPrivateIdAndStreamType(rpcConnection.getParticipantPrivateId(), StreamType.MAJOR))
                || !OpenViduRole.MODERATOR.equals(participant.getRole())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // set subtitle config into session and this participant
        session.setSubtitleConfig(subtitleConfig, language);
        participant.setSubtitleConfig(subtitleConfig).setSubtitleLanguage(language);

        // resp
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // send notification to other participant
        session.getParticipants().forEach(part -> {
            if (!Objects.equals(part, participant) && StreamType.MAJOR.equals(part.getStreamType())) {
                notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SETSUBTITLECONFIG_METHOD, request.getParams());
            }
        });
    }
}
