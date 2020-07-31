package io.openvidu.server.rpc.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.common.pojo.Group;
import io.openvidu.server.common.pojo.UserGroup;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author geedow
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetGroupListHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long userId = getLongParam(request, ProtocolElements.GET_GROUP_LIST_USERID_PARAM);

        List<UserGroup> userGroupList = userGroupMapper.selectListByUserId(userId);
        JsonObject resp = new JsonObject();
        JsonArray array = new JsonArray();
        for (int i = 0; i < userGroupList.size(); i++) {
            JsonObject object = new JsonObject();
            UserGroup userGroup = userGroupList.get(i);
            Group group = groupMapper.selectByPrimaryKey(userGroup.getGroupId());

            object.addProperty(ProtocolElements.GET_GROUP_LIST_GROUPID_PARAM, userGroup.getGroupId());
            object.addProperty(ProtocolElements.GET_GROUP_LIST_GROUPNAME_PARAM, group.getGroupName());
            array.add(object);
        }

        resp.add(ProtocolElements.GET_GROUP_LIST_GROUPLIST_PARAM, array);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
