/*
 * (C) Copyright 2017-2019 OpenVidu (https://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.openvidu.server.client.RtcUserClient;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.RequestDTO;
import io.openvidu.server.domain.RequestEx;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcExHandler;
import io.openvidu.server.rpc.RpcHandlerFactory;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Pablo Fuente (pablofuenteperez@gmail.com)
 */
@RestController
@CrossOrigin
@RequestMapping("/signal")
@Slf4j
public class HandlerController {

    @Autowired
    RpcHandlerFactory rpcHandlerFactory;

    @Autowired
    RpcNotificationService notificationService;

    @Autowired
    RpcExHandler rpcExHandler;

    @Autowired
    private RtcUserClient rtcUserClient;

    @PostMapping(value = "handler", produces = "application/json")
    public @ResponseBody
    RespResult<?> handler(@RequestBody RequestDTO requestDTO) {

        try {
            RequestEx<JsonObject> request = new RequestEx<>();
            request.setId(requestDTO.getId());
            request.setMethod(requestDTO.getMethod());
            request.setParams(new Gson().fromJson(requestDTO.getParams(), JsonObject.class));
            request.setTrackId(requestDTO.getTrackId());

            RpcConnection rpcConnection = rtcUserClient.getRpcConnection(requestDTO.getParticipantPrivateId());

            if (rpcConnection == null) {
                log.warn(
                        "No connection found for participant with privateId {} when trying to execute method '{}'. " +
                                "Method 'Session.connect()' must be the first operation called in any session", request.getId(), request.getMethod());
                return RespResult.fail(ErrorCodeEnum.ACCESS_IN_NEEDED);
            }
            int hashCode = rpcConnection.hashCode();
            rpcExHandler.handleRequest(rpcConnection, request);

            if (hashCode != rpcConnection.hashCode()) {
                rtcUserClient.updateRpcConnection(rpcConnection);
            }
            return RespResult.ok();
        } catch (Exception e) {
            log.error(e.toString(), e);
            return RespResult.fail(ErrorCodeEnum.FAIL);
        }
    }


    @PostMapping(value = "accessIn", produces = "application/json")
    public @ResponseBody
    RespResult<RpcConnection> accessIn(@RequestBody RequestDTO requestDTO) {
        try {
            Request<JsonObject> request = new Request<>();
            request.setId(requestDTO.getId());
            request.setMethod(requestDTO.getMethod());
            request.setParams(new Gson().fromJson(requestDTO.getParams(), JsonObject.class));

            //rtcUserClient.getRpcConnection(requestDTO.getParticipantPrivateId());
            RpcConnection rpcConnection = rtcUserClient.getRpcConnection(requestDTO.getParticipantPrivateId());
            rpcExHandler.handleRequest(rpcConnection, request);
            rtcUserClient.updateRpcConnection(rpcConnection);

            if (rpcConnection.getLoginTime() == null || rpcConnection.getLoginTime() == 0) {
                return RespResult.fail(ErrorCodeEnum.FAIL);
            }
            return RespResult.ok(rpcConnection);
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

}
