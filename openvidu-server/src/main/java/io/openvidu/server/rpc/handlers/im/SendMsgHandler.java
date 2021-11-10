package io.openvidu.server.rpc.handlers.im;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.ImMsgMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.IMModeEnum;
import io.openvidu.server.common.pojo.ImMsg;
import io.openvidu.server.common.pojo.ImMsgExample;
import io.openvidu.server.core.Notification;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.domain.ImUser;
import io.openvidu.server.domain.SendMsgNotify;
import io.openvidu.server.domain.resp.SendMsgResp;
import io.openvidu.server.domain.vo.SendMsgVO;
import io.openvidu.server.exception.BizException;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service(ProtocolElements.SEND_MSG_METHOD)
public class SendMsgHandler extends ExRpcAbstractHandler<SendMsgVO> {

    @Autowired
    private ImMsgMapper imMsgMapper;

    public static String[] words = new String[0];
    public static Set<Long> mutedUser = new HashSet<>();

    @Override
    public RespResult<SendMsgResp> doProcess(RpcConnection rpcConnection, Request<SendMsgVO> request, SendMsgVO params) {
        BindValidate.notEmpty(params::getClientMsgId, params::getRuid, params::getRoomId, params::getMsgType,
                params::getOperate, params::getContent);
        BindValidate.notEmptyIfMatch(() -> params.getOperate() == 0, params::getReciverAccount);

        if (sensitiveWord(params)) {
            return RespResult.fail(ErrorCodeEnum.SENSITIVE_WORD);
        }
        if (muted(params, rpcConnection)) {
            return RespResult.fail(ErrorCodeEnum.SENSITIVE_WORD);
        }

        String roomId = params.getRoomId();
        Session session = sessionManager.getSession(roomId);
        if (session == null || !session.getRuid().equals(params.getRuid())) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        Participant participant = session.getParticipantByUUID(rpcConnection.getUserUuid()).orElse(null);
        if (participant == null) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        validateImMode(params, session);


        ImMsg imMsg;
        if (Objects.equals(params.getResendFlag(), 1)) {
            imMsg = checkDuplicate(rpcConnection, params);
            if (imMsg == null) {
                imMsg = saveMsg(rpcConnection, params, session);
            }
        } else {
            imMsg = saveMsg(rpcConnection, params, session);
        }

        SendMsgResp resp = new SendMsgResp();
        resp.setMsgId(imMsg.getId());
        resp.setClientMsgId(imMsg.getClientMsgId());
        resp.setRuid(imMsg.getRuid());
        resp.setRoomId(imMsg.getRoomId());
        resp.setTimestamp(imMsg.getTimestamp().getTime());

        Notification notification = buildNotification(params, imMsg, session);

        return RespResult.ok(resp, notification);
    }

    private boolean muted(SendMsgVO params, RpcConnection session) {
        return mutedUser.contains(session.getUserId());
    }

    /**
     *
     */
    private boolean sensitiveWord(SendMsgVO params) {
        for (String word : words) {
            if (params.getContent().contains(word)) {
                return true;
            }
        }
        return false;
    }

    private void validateImMode(SendMsgVO params, Session session) {
        if (session.getPresetInfo().getImMode() == IMModeEnum.NOT_LIMIT.getMode()) {
            return;
        }
        if (session.getConference().getModeratorUuid().equals(params.getSenderAccount())) {
            return;
        }
        if (session.getPresetInfo().getImMode() == IMModeEnum.ALL_LIMIT.getMode()) {
            throw new BizException(ErrorCodeEnum.IM_ALL_LIMIT);
        }
        if (session.getPresetInfo().getImMode() == IMModeEnum.ONLY_PUBLISH.getMode() && params.getOperate() != 1) {
            throw new BizException(ErrorCodeEnum.IM_ONLY_PUBLISH);
        }
        if (session.getPresetInfo().getImMode() == IMModeEnum.ONLY_TO_MODERATOR.getMode()) {
            if (params.getOperate() != 0) {
                throw new BizException(ErrorCodeEnum.IM_ONLY_TO_MODERATOR);
            } else if (params.getReciverAccount().isEmpty() || !Objects.equals(session.getConference().getModeratorUuid(), params.getReciverAccount().get(0))) {
                throw new BizException(ErrorCodeEnum.IM_ONLY_TO_MODERATOR);
            }
        }
    }

    private ImMsg checkDuplicate(RpcConnection rpcConnection, SendMsgVO params) {
        ImMsgExample example = new ImMsgExample();
        example.createCriteria().andSenderUserIdEqualTo(rpcConnection.getUserId()).andClientMsgIdEqualTo(params.getClientMsgId())
                .andRuidEqualTo(params.getRuid());
        return imMsgMapper.selectOne(example);
    }

    private Notification buildNotification(SendMsgVO params, ImMsg imMsg, Session session) {
        SendMsgNotify sendMsgNotify = new SendMsgNotify();
        BeanUtils.copyProperties(params, sendMsgNotify);
        sendMsgNotify.setMsgId(imMsg.getId());

        sendMsgNotify.setSender(new ImUser(imMsg.getSenderUuid(), imMsg.getSenderUsername(), imMsg.getSenderTerminalType()));
        sendMsgNotify.setTimestamp(imMsg.getTimestamp().getTime());

        Notification notification = new Notification(ProtocolElements.NOTIFY_SEND_MSG_METHOD, sendMsgNotify);

        if (params.getOperate() == 0) {
            notification.setParticipantIds(session.getParticipantByUUID(params.getReciverAccount().get(0)).get().getParticipantPrivateId());
            sendMsgNotify.setRecivers(Collections.singletonList(new ImUser(imMsg.getRevicerUuid(),
                    imMsg.getRevicerUsername(), imMsg.getRevicerTerminalType())));
        } else {
            notification.withParticipantIds(imMsg.getRoomId(), sessionManager);
        }
        return notification;
    }

    private ImMsg saveMsg(RpcConnection rpcConnection, SendMsgVO params, Session session) {
        ImMsg imMsg = new ImMsg();
        imMsg.setClientMsgId(params.getClientMsgId());
        imMsg.setRuid(session.getRuid());
        imMsg.setRoomId(session.getSessionId());
        imMsg.setTimestamp(new Date());
        imMsg.setMsgType(params.getMsgType());
        imMsg.setOperate(params.getOperate());
        imMsg.setSenderUserId(rpcConnection.getUserId());
        imMsg.setSenderUuid(rpcConnection.getUserUuid());
        imMsg.setSenderUsername(rpcConnection.getUsername());
        imMsg.setContent(params.getContent());
        imMsg.setExt(params.getExt());

        Participant sendParticipant = session.getParticipantByUUID(rpcConnection.getUserUuid()).orElse(null);
        imMsg.setSenderTerminalType(sendParticipant.getTerminalType().name());
        if (params.getOperate() == 0) {
            BindValidate.notEmpty(params::getReciverAccount);
            String reciverUuid = params.getReciverAccount().get(0);

            Participant targetParticipant = session.getParticipantByUUID(reciverUuid).orElse(null);
            if (targetParticipant == null) {
                throw new BizException(ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            }
            imMsg.setRevicerUserId(targetParticipant.getUserId());
            imMsg.setRevicerUuid(targetParticipant.getUuid());
            imMsg.setRevicerUsername(targetParticipant.getUsername());
            imMsg.setRevicerTerminalType(targetParticipant.getTerminalType().name());
        }
        imMsgMapper.insertSelective(imMsg);
        return imMsg;
    }
}
