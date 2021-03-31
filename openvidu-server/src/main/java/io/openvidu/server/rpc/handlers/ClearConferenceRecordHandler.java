package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author chosongi
 * @date 2020/9/10 15:22
 */
@Service
public class ClearConferenceRecordHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, "roomId");

        if (!isReadWrite(rpcConnection.getUserUuid())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        if (Objects.nonNull(sessionManager.getSession(roomId)) && sessionManager.getSession(roomId).isRecording.get()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_IS_RECORDING);
            return;
        }

        ConferenceRecord search = new ConferenceRecord();
        search.setRoomId(roomId);
        List<ConferenceRecord> roomRecords = conferenceRecordManage.getByCondition(search);

        if (!CollectionUtils.isEmpty(roomRecords)) {
            // delete roomRecordSummary,conferenceRecord,conferenceRecordInfo
            conferenceRecordManage.clearRoomRecords(roomId, roomRecords.stream().map(ConferenceRecord::getRuid).collect(Collectors.toList()), rpcConnection.getProject());
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
