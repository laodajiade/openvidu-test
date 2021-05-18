package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.OftenContactsMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.OftenContactsEnum;
import io.openvidu.server.common.pojo.OftenContacts;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 删除或者添加常用联系人
 *
 * @author Administrator
 */
@Slf4j
@Service
public class SetFrequentContactsHandler extends RpcAbstractHandler {

    @Resource
    private OftenContactsMapper oftenContactsMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String methodType = getStringOptionalParam(request, "operate");
        String uuid = getStringOptionalParam(request, "uuid");

        if (StringUtils.isEmpty(uuid)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }
        Long userId = rpcConnection.getUserId();
        if ("add".equals(methodType)) {
            User userInfo = userMapper.selectByUUID(uuid);
            if (userInfo == null) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.USER_NOT_EXIST);
                return;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", uuid);
            map.put("userId", userId);
            final boolean isOftenContacts = oftenContactsMapper.isOftenContacts(map);
            if (isOftenContacts) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REPEAT_ADD_CONTACTS);
                return;
            }
            OftenContacts oftenContacts = new OftenContacts();
            oftenContacts.setContactsUserId(userInfo.getId());
            oftenContacts.setContactsUuid(uuid);
            oftenContacts.setUserId(userId);
            oftenContactsMapper.addOftenContacts(oftenContacts);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        } else if ("cancel".equals(methodType)) {
            oftenContactsMapper.delOftenContacts(uuid);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
    }
}
