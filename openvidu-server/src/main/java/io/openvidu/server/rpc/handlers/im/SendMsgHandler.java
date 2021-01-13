package io.openvidu.server.rpc.handlers.im;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.ImMsgMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ImMsg;
import io.openvidu.server.common.pojo.ImMsgExample;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Notification;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
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

import java.util.Date;
import java.util.Objects;

@Slf4j
@Service(ProtocolElements.SENDMESSAGE_ROOM_METHOD)
public class SendMsgHandler extends ExRpcAbstractHandler<SendMsgVO> {

    @Autowired
    private ImMsgMapper imMsgMapper;

    @Override
    public RespResult<SendMsgResp> doProcess(RpcConnection rpcConnection, Request<SendMsgVO> request, SendMsgVO params) {
        BindValidate.notEmpty(params::getClientMsgId, params::getRuid, params::getRoomId, params::getMsgType,
                params::getOperate, params::getContent);

        String roomId = params.getRoomId();
        Session session = sessionManager.getSession(roomId);
        if (session == null || !session.getRuid().equals(params.getRuid())) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        Participant participant = sessionManager.getParticipant(roomId, rpcConnection.getParticipantPrivateId());
        if (participant == null) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

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

        Notification notification = buildNotification(params, imMsg, participant);

        return RespResult.ok(resp, notification);
    }

    private ImMsg checkDuplicate(RpcConnection rpcConnection, SendMsgVO params) {
        ImMsgExample example = new ImMsgExample();
        example.createCriteria().andSenderUserIdEqualTo(rpcConnection.getUserId()).andClientMsgIdEqualTo(params.getClientMsgId())
                .andRuidEqualTo(params.getRuid());
        return imMsgMapper.selectOne(example);
    }

    private Notification buildNotification(SendMsgVO params, ImMsg imMsg, Participant participant) {
        SendMsgNotify sendMsgNotify = new SendMsgNotify();
        BeanUtils.copyProperties(params, sendMsgNotify);
        sendMsgNotify.setMsgId(imMsg.getId());
        sendMsgNotify.setSenderUsername(participant.getUsername());
        sendMsgNotify.setTimestamp(imMsg.getTimestamp().getTime());

        Notification notification = new Notification("", new Object());

        if (params.getOperate() == 1) {
            notification.setParticipantIds(params.getReciverAccount());
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
        imMsg.setContent(params.getContent());
        imMsg.setExt(params.getExt());

        if (params.getOperate() == 1) {
            BindValidate.notEmpty(params::getReciverAccount);
            String reciverUuid = params.getReciverAccount().get(0);
            User user = userManage.queryByUuid(reciverUuid);
            if (user == null) {
                throw new BizException(ErrorCodeEnum.USER_NOT_EXIST);
            }
            imMsg.setRevicerUserId(user.getId());
        }
        imMsgMapper.insertSelective(imMsg);
        return imMsg;
    }
}
