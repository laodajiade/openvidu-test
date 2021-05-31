package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.FixedRoomManagerMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询用户权限
 *
 * @author Administrator
 */
@Service
public class QueryOperationPermissionHandler extends RpcAbstractHandler {

    @Resource
    private FixedRoomManagerMapper fixedRoomManagerMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, "uuid");
        if (uuid == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }
        User user = userMapper.selectByUUID(uuid);

        if (user == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }

        JSONObject jsonObject = new JSONObject();
        //是否有充值过并发会议
        final boolean isRechargeConcurrent = corporationMapper.selectIsRechargeConcurrent(user.getProject());
        //查询用户是否是固定会议室管理员
        final boolean isFixedRoomAdmin = fixedRoomManagerMapper.selectIsFixedRoomAdmin(uuid);

        //创建加入录制权限
        List<Integer> permission = new ArrayList<>();
        //并发服务，固定会议室管理权限
        List<Integer> abilities = new ArrayList<>();

        switch (user.getType()) {
            case 1:
                permission = getHardTerminalUser(user.getProject());
                break;
            case 0:
                permission = getSoftTerminalUser(uuid);
                break;
            default:
                break;
        }

        if (isFixedRoomAdmin) {
            abilities.add(1);
        }
        if (isRechargeConcurrent) {
            abilities.add(0);
        }

        jsonObject.put("permission", permission);
        jsonObject.put("abilities", abilities);
        jsonObject.put("list", new int[]{0,1,2,3});
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }

    /**
     * 获取软终端用户权限
     *
     * @param uuid
     * @return
     */
    public List<Integer> getSoftTerminalUser(String uuid) {
        List<Integer> list = new ArrayList<>();
        Role role = roleMapper.selectUserOperationPermission(uuid);
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
                    default:
                        break;
                }
            }
        }
        return list;
    }

    /**
     * 查询硬终端用户权限
     *
     * @return
     */
    public List<Integer> getHardTerminalUser(String project) {
        boolean isRechargeConcurrent = corporationMapper.selectIsRechargeConcurrent(project);
        return isRechargeConcurrent ? Lists.newArrayList(0, 1, 2) : Lists.newArrayList(0);
    }
}
