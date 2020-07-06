package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.SubtitleType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class SendSubtitleHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String status = getStringParam(request, ProtocolElements.SENDSUBTITLE_STATUS_PARAM);
        JsonArray subtitles = getParam(request, ProtocolElements.SENDSUBTITLE_SUBTITLES_PARAM).getAsJsonArray();

        Session session = sessionManager.getSession(rpcConnection.getSessionId());

        // check sender permission
        Participant senderPart = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
        if (!senderPart.getSubtitleConfig().ableToSendSubtitle()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (subtitles.size() > 0) {
            JsonObject recogSubtitleObj = null;
            Map<String, JsonObject> translateSubtitleMap = new HashMap<>();
            for (JsonElement jsonElement : subtitles) {
                JsonObject subObj = jsonElement.getAsJsonObject();
                SubtitleType type = SubtitleType.valueOf(subObj.get("type").getAsString());
                if (SubtitleType.translation.equals(type)) {    // put translation subtitle into map according to language type
                    translateSubtitleMap.put(subObj.get("language").getAsString(), subObj);
                } else {    // get recognition subtitle
                    recogSubtitleObj = subObj;
                }
            }

            // check recogSubtitleObj legal
            if (Objects.isNull(recogSubtitleObj)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }

            // resp
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

            // construct notification param according to part's subtitle config and source language
            // send notification to other participant
            JsonObject notifyParam = new JsonObject();
            notifyParam.addProperty("roomId", rpcConnection.getSessionId());
            notifyParam.addProperty("sourceId", rpcConnection.getUserUuid());
            notifyParam.addProperty("username", rpcConnection.getUsername());
            notifyParam.addProperty("status", status);

            JsonObject recognitionSubtitleObj = recogSubtitleObj;
            session.getMajorPartEachConnect().forEach(participant -> {
                if (participant.getSubtitleConfig().needToDisPatchSubtitle()) {
                    JsonArray eachSubtitleArr = new JsonArray(8);
                    eachSubtitleArr.add(recognitionSubtitleObj);

                    if (participant.getSubtitleConfig().needToDispatchTranslation()) {
                        // get translate subtitle
                        JsonObject translationObj = translateSubtitleMap.get(participant.getSubtitleLanguage().name());
                        if (Objects.nonNull(translationObj)) {
                            eachSubtitleArr.add(translationObj);
                        }
                    }

                    JsonObject notification = notifyParam.deepCopy();
                    notification.add("subtitles", eachSubtitleArr);
                    notificationService.sendNotification(participant.getParticipantPrivateId(),
                            ProtocolElements.SENDDISPLAYSUBTITLE_NOTIFY, notification);
                }
            });
        }
    }
}
