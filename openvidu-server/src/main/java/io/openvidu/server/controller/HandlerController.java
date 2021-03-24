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
import io.openvidu.server.domain.RequestDTO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcExHandler;
import io.openvidu.server.rpc.RpcHandlerFactory;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping(value = "handler", produces = "application/json")
    public @ResponseBody
    String handler(@RequestBody RequestDTO requestDTO) {

        try {
            Request<JsonObject> request = new Request<>();
            request.setId(requestDTO.getId());
            request.setMethod(requestDTO.getMethod());
            request.setParams(new Gson().fromJson(requestDTO.getParams(), JsonObject.class));

            RpcConnection rpcConnection = rtcUserClient.getRpcConnection(requestDTO.getParticipantPrivateId());

            if (rpcConnection == null) {
                // return errorCode:11007("please access in first") if any method is called before 'accessIn'
                log.warn(
                        "No connection found for participant with privateId {} when trying to execute method '{}'. " +
                                "Method 'Session.connect()' must be the first operation called in any session", request.getId(), request.getMethod());
                //notificationService.sendRespWithConnTransaction(requestDTO.getParticipantPrivateId(), request.getId(), ErrorCodeEnum.ACCESS_IN_NEEDED);
                return ErrorCodeEnum.ACCESS_IN_NEEDED.name();
            }

            rpcExHandler.handleRequest(rpcConnection, request);
//            SendResponseDTO respDTO = new SendResponseDTO();
//            respDTO.setParticipantPrivateId(requestDTO.getParticipantPrivateId());
//            respDTO.setId(requestDTO.getId());
            //respDTO.setResult(result.getResult());
            //RpcConnection rpcConnection = notificationService.getRpcConnection(requestDTO.getParticipantPrivateId());
            //respDTO.setUuid(rpcConnection.getUserUuid());

            return "{}";
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException(e);
        }

    }


    @PostMapping(value = "accessIn", produces = "application/json")
    public @ResponseBody
    String accessIn(@RequestBody RequestDTO requestDTO) {

        try {
            Request<JsonObject> request = new Request<>();
            request.setId(requestDTO.getId());
            request.setMethod(requestDTO.getMethod());
            request.setParams(new Gson().fromJson(requestDTO.getParams(), JsonObject.class));

            rtcUserClient.getRpcConnection(requestDTO.getParticipantPrivateId());
            RpcConnection rpcConnection = rtcUserClient.newRpcConnection(requestDTO.getParticipantPrivateId(), requestDTO.getOrigin());
            rpcExHandler.handleRequest(rpcConnection, request);

//            SendResponseDTO respDTO = new SendResponseDTO();
//            respDTO.setParticipantPrivateId(requestDTO.getParticipantPrivateId());
//            respDTO.setId(requestDTO.getId());
//            //respDTO.setResult(result.getResult());
//            respDTO.setUuid(rpcConnection.getUserUuid());
            //       return new GsonBuilder().setPrettyPrinting().create().toJson(respDTO);
            return "{}";
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new RuntimeException(e);
        }

    }

}
