package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.StatisticsDurationInfo;
import io.openvidu.server.common.pojo.UserGroupVo;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author even
 * @date 2020/11/9 18:08
 */
@Slf4j
@Service
public class StatisticsDurationHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        int pageNum = getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = getIntParam(request, ProtocolElements.PAGESIZE);
        String project  = rpcConnection.getProject();
        String uuid = rpcConnection.getUserUuid();
        PageHelper.startPage(pageNum, pageSize);
        List<StatisticsDurationInfo> statisticsDurationInfos = conferencePartHistoryMapper.selectStatisticsDuration(project,uuid);
        PageInfo<StatisticsDurationInfo> pageInfo = new PageInfo<>(statisticsDurationInfos);
        JSONObject resp = new JSONObject();
        resp.put(ProtocolElements.PAGENUM, pageNum);
        resp.put(ProtocolElements.PAGESIZE, pageSize);
        resp.put(ProtocolElements.TOTAL, pageInfo.getTotal());
        resp.put(ProtocolElements.PAGES, pageInfo.getPages());
        resp.put(ProtocolElements.PAGELIST,pageInfo.getList());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
