package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author even
 * @date 2021/1/11 16:46
 */
@Slf4j
@Service
public class GetInviteInfoHandler extends RpcAbstractHandler {


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.GET_INVITE_INFO_ROOMID_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.GET_INVITE_INFO_RUID_PARAM);
        Session session = sessionManager.getSession(roomId);
        // 如果ruid为空则复制会议邀请信息   否则复制预约会议邀请信息
        if (StringUtils.isEmpty(ruid)) {
            if (Objects.isNull(session)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }
            // verify operate permission
            Optional<Participant> participantOptional = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId(), rpcConnection.getUserUuid());
            if (!participantOptional.isPresent() || !participantOptional.get().getRole().isController()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PERMISSION_LIMITED);
                return;
            }

            ConferenceSearch search = new ConferenceSearch();
            search.setRoomId(roomId);
            search.setStatus(ConferenceStatus.PROCESS.getStatus());
            List<Conference> list = conferenceMapper.selectBySearchCondition(search);

            JSONObject respJson = new JSONObject();
            respJson.put("userName", participantOptional.get().getUsername());
            respJson.put("subject", session.getConference().getConferenceSubject());
            respJson.put("startTime", session.getStartTime());
            respJson.put("roomId", roomId);
            respJson.put("password", StringUtils.isEmpty(session.getConference().getPassword()) ? "" : session.getConference().getPassword());
            respJson.put("inviteUrl", CollectionUtils.isEmpty(list) ? "" : openviduConfig.getConferenceInviteUrl() + list.get(0).getShortUrl());
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
        } else {
            AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
            if (Objects.isNull(appointConference)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }
            JSONObject respJson = new JSONObject();
            respJson.put("userName", appointConference.getModeratorName());
            respJson.put("subject", appointConference.getConferenceDesc());
            respJson.put("startTime", appointConference.getStartTime());
            respJson.put("roomId", appointConference.getRoomId());
            respJson.put("password", appointConference.getPassword());
            respJson.put("inviteUrl", openviduConfig.getConferenceInviteUrl() +  appointConference.getShortUrl());
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
        }
    }
}
