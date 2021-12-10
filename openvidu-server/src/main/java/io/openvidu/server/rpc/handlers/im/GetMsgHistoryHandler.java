package io.openvidu.server.rpc.handlers.im;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.ImMsgMapper;
import io.openvidu.server.common.pojo.ImMsg;
import io.openvidu.server.core.PageResult;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.ImUser;
import io.openvidu.server.domain.SendMsgNotify;
import io.openvidu.server.domain.vo.GetMsgHistoryVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service(ProtocolElements.GET_MSG_HISTORY_METHOD)
public class GetMsgHistoryHandler extends AbstractIMHandler<GetMsgHistoryVO> {

    @Autowired
    private ImMsgMapper imMsgMapper;

    @Override
    public RespResult<PageResult<SendMsgNotify>> doProcess(RpcConnection rpcConnection, Request<GetMsgHistoryVO> request, GetMsgHistoryVO params) {
        BindValidate.notEmpty(params::getRuid);

        Page<Object> page = PageHelper.startPage(1, params.getLimit());
        Date date = params.getTime() == 0 ? null : new Date(params.getTime());

        List<ImMsg> imgHistory = imMsgMapper.getImgHistory(params.getRuid(), rpcConnection.getUserId(), date, params.getId(), params.getReverse(), params.getKeyword());

        List<SendMsgNotify> resultList = imgHistory.stream().map(imMsg -> {
            SendMsgNotify sendMsgNotify = new SendMsgNotify();
            BeanUtils.copyProperties(imMsg, sendMsgNotify);
            sendMsgNotify.setMsgId(imMsg.getId());
            sendMsgNotify.setResendFlag(0);
            sendMsgNotify.setSenderAccount(imMsg.getSenderUuid());
            sendMsgNotify.setSender(new ImUser(imMsg.getSenderUuid(), getRecentUsername(imMsg.getSenderUuid(), imMsg.getSenderUsername()), imMsg.getSenderTerminalType()));
            sendMsgNotify.setTimestamp(imMsg.getTimestamp().getTime());

            if (sendMsgNotify.getOperate() == 0) {
                sendMsgNotify.setRecivers(Collections.singletonList(
                        new ImUser(imMsg.getRevicerUuid(), getRecentUsername(imMsg.getRevicerUuid(), imMsg.getRevicerUsername()), imMsg.getRevicerTerminalType())));
            }

            sendMsgNotify.setClientMsgId(null);
            return sendMsgNotify;
        }).collect(Collectors.toList());

        return RespResult.ok(new PageResult<>(resultList, page));
    }
}
