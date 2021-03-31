package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.vo.JpushMessageVo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author even
 * @date 2021/1/14 11:06
 */
@Slf4j
@Service
public class GetJpushMessageHandler extends RpcAbstractHandler {


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        int pageNum = getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = getIntParam(request, ProtocolElements.PAGESIZE);
        String uuid = getStringParam(request, ProtocolElements.GET_JPUSH_MESSAGE_UUID_PARAM);
        PageHelper.startPage(pageNum, pageSize);

        List<JpushMessageVo> jpushMessages = jpushMessageMapper.getJpushMsgList(uuid);
        PageInfo<JpushMessageVo> pageInfo = new PageInfo<>(jpushMessages);
        JSONObject respJson = new JSONObject();
        respJson.put("list", pageInfo.getList());
        respJson.put("pageSize", pageSize);
        respJson.put("total", pageInfo.getTotal());
        respJson.put("pageNum", pageNum);
        respJson.put("pages", pageInfo.getPages());

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }

}
