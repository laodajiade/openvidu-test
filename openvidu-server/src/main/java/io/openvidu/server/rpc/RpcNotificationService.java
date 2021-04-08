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

package io.openvidu.server.rpc;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RpcNotificationService {


    RpcConnection newRpcConnection(Transaction t, Request<JsonObject> request);

    RpcConnection addTransaction(Transaction t, Request<JsonObject> request);
    RpcConnection addTransaction(RpcConnection rc, Request<JsonObject> request);

    void sendResponse(String participantPrivateId, Integer transactionId, Object result);

    void sendErrorResponse(String participantPrivateId, Integer transactionId, Object data,
                           OpenViduException error);


    void sendErrorResponseWithDesc(String participantPrivateId, Integer transactionId, Object data,
                                   ErrorCodeEnum errorCodeEnum);

    void sendNotification(final String participantPrivateId, final String method, final Object params);

    void sendNotificationWithoutLog(final String participantPrivateId, final String method, final Object params);

    //private Session sanityGetRpcSession(final String participantPrivateId, final String method, final Object params);

    RpcConnection closeRpcSession(String participantPrivateId);

    void sendBatchNotification(Set<Participant> participants, final String method, final Object params);

    void sendBatchNotification(List<String> participantPrivateIds, final String method, final Object params);

    /**
     * sendBatchNotification 的优化版本，使用多线程并发通知，同步接口
     */
    void sendBatchNotificationConcurrent(List<String> participantPrivateIds, final String method, final Object params);

    //private Transaction getAndRemoveTransaction(String participantPrivateId, Integer transactionId);

    void showRpcConnections();

    RpcConnection getRpcConnection(String participantPrivateId);

    Collection<RpcConnection> getRpcConnections();

    void sendRespWithConnTransaction(Transaction t, Integer requestId, ErrorCodeEnum errorCode);
}
