package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Group;
import io.openvidu.server.common.pojo.RootDept;
import io.openvidu.server.common.pojo.UserGroupVo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetGroupListHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        int pageNum = getIntParam(request,ProtocolElements.PAGENUM);
        int pageSize = getIntParam(request,ProtocolElements.PAGESIZE);
        String uuid = rpcConnection.getUserUuid();
        RootDept rootDept = userDeptMapper.selectRootDeptByUuid(uuid);
        if (Objects.isNull(rootDept)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
        }

        PageHelper.startPage(pageNum,pageSize);
        List<Group> groups = userGroupMapper.selectByCorpIds(rootDept.getCorpId());
        JsonObject resp = new JsonObject();
        JsonArray array = new JsonArray();
        if (!CollectionUtils.isEmpty(groups)) {
            for (Group group : groups) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty(ProtocolElements.GET_GROUP_LIST_GROUPNAME_PARAM, group.getGroupName());
                jsonObject.addProperty(ProtocolElements.GET_GROUP_LIST_GROUPID_PARAM, group.getId());
                jsonObject.addProperty(ProtocolElements.GET_GROUP_LIST_NUMOFPEOPLE_PARAM,group.getNumOfPeople());
                array.add(jsonObject);
            }
        }
        PageInfo<Group> pageInfo = new PageInfo<>(groups);
        resp.addProperty(ProtocolElements.PAGENUM,pageNum);
        resp.addProperty(ProtocolElements.PAGESIZE,pageSize);
        resp.addProperty(ProtocolElements.TOTAL,pageInfo.getTotal());
        resp.addProperty(ProtocolElements.PAGES,pageInfo.getPages());
        resp.add(ProtocolElements.GET_GROUP_LIST_GROUPLIST_PARAM, array);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
