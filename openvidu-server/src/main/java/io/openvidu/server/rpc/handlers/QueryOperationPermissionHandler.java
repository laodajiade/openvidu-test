package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询用户权限
 *
 * @author Administrator
 */
@Service
public class QueryOperationPermissionHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, "uuid");

        if (uuid == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }

        Role role = roleMapper.selectUserOperationPermission(uuid);
        JSONObject jsonObject = new JSONObject();
        List<Integer> list = new ArrayList<>();
        if (!StringUtils.isEmpty(role)) {

            for (String split : role.getPrivilege().split(",")) {
                switch (split) {
                    case "join_room_allowed":
                        list.add(0);
                        break;
                    case "create_book_room_allowed":
                        list.add(1);
                        break;
                    case "recording_conference_room_allowed":
                        list.add(2);
                        break;
                    case "fixedMeetingRoom":
                        list.add(3);
                        break;
                    default:
                        break;
                }
            }
        }
        jsonObject.put("list",list);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
