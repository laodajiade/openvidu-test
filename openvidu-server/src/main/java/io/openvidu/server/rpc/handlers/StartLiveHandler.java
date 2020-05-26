package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.LivingProperties;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class StartLiveHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.START_LIVE_ROOMID_PARAM);
        if (StringUtil.isNullOrEmpty(roomId)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        // 校验会议
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(roomId);
        search.setStatus(ConferenceStatus.PROCESS.getStatus());
        List<Conference> conferenceList = conferenceMapper.selectBySearchCondition(search);
        if (Objects.isNull(conferenceList) || conferenceList.isEmpty()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "startLive");
        } catch (OpenViduException e) {
            return;
        }

        // 权限校验（web：管理员，terminal：主持人）
        if (!isModerator(participant.getRole())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // 判断该会议是否正在直播
        Session session = sessionManager.getSession(roomId);
        if (!session.sessionAllowedStartToLive()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_IS_LIVING);
            return;
        }
        // session中设置开始直播时间
        sessionManager.setStartLivingTime(roomId, System.currentTimeMillis());

        // 通知媒体服务开始直播
        KurentoSession kurentoSession = (KurentoSession) session;
        String liveUrl = livingManager.startLiving(session,
                new LivingProperties.Builder()
                        .name("")
                        .outputMode(kurentoSession.getSessionProperties().defaultOutputMode())
                        .livingLayout(kurentoSession.getSessionProperties().defaultLivingLayout())
                        .customLayout(kurentoSession.getSessionProperties().defaultCustomLayout())
                        .build(),
                rpcConnection.getUserUuid());

        JsonObject respJson = new JsonObject();
        String livingUrl = "";
        session.setLivingUrl(livingUrl = this.openviduConfig.getOpenviduLivingUrlPrefix() + roomId);
        respJson.addProperty("livingUrl", livingUrl);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);

        // 缓存直播地址
        cacheManage.saveLivingInfo(roomId, CacheKeyConstants.CONFERENCE_LIVING_INFO_LIVINGURL, liveUrl);
        // 通知与会者开始直播
        notifyStartLiving(rpcConnection.getSessionId());
    }

    private void notifyStartLiving(String sessionId) {
        sessionManager.getSession(sessionId).getParticipants().forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.START_LIVE_METHOD, new JsonObject()));
    }
}

