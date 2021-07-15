package io.openvidu.server.rpc;

import cn.suditech.access.client.AccessClient;
import cn.suditech.access.core.ErrorBean;
import cn.suditech.access.domain.AccessBean;
import cn.suditech.access.domain.AccessErrorResp;
import cn.suditech.access.domain.AccessNotification;
import cn.suditech.access.domain.AccessResp;
import cn.suditech.access.domain.enums.AccessCode;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.NotSupportedException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RpcNotificationServiceAccess implements RpcNotificationService {

    @Autowired
    private IRpcConnection rpcConnections;

    private final Cache<String, RpcConnection> transactions = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

    @Autowired
    private AccessClient accessClient;

    private static final ThreadPoolExecutor NOTIFY_THREAD_POOL = new ThreadPoolExecutor(
            Math.max(Runtime.getRuntime().availableProcessors() * 10 + 4, 20), Math.max(Runtime.getRuntime().availableProcessors() * 40, 200),
            120L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000),
            new ThreadFactoryBuilder().setNameFormat("notify_thread_pool-").setDaemon(true).build());

    /**
     * newRpcConnection 由access-server完成，信令不再负责创建
     */
    @Override
    public RpcConnection newRpcConnection(Transaction t, Request<JsonObject> request) {
        throw new NotSupportedException("newRpcConnection 由 access-server 完成，信令不再负责创建");
    }

    @Override
    public RpcConnection addTransaction(Transaction t, Request<JsonObject> request) {
        throw new NotSupportedException("please used #addTransaction(RpcConnection rc, Request<JsonObject> request)");
    }

    @Override
    public RpcConnection addTransaction(RpcConnection rc, Request<JsonObject> request) {
        transactions.put(getTransactionId(rc.getParticipantPrivateId(), request.getId()), rc);
        return rc;
    }

    @Override
    public void sendResponse(String participantPrivateId, Integer transactionId, Object result) {
        RpcConnection rpcConnection = getAndRemoveTransaction(participantPrivateId, transactionId);
        if (rpcConnection == null) {
            log.error("No transaction {} found for paticipant with private id {}, unable to send result {}",
                    transactionId, participantPrivateId, new GsonBuilder().setPrettyPrinting().create().toJson(result));
            return;
        }
        try {
            AccessResp accessResp = new AccessResp(transactionId, participantPrivateId, rpcConnection.getAccessInfo().getOrigin(),
                    JSONObject.parseObject(new GsonBuilder().create().toJson(result)));

            AccessCode accessCode = accessClient.sendResponse(accessResp);
            if (accessCode == AccessCode.SUCCESS) {
                log.info("WebSocket response session #{} - Response transaction id:{} and result: {}", participantPrivateId, transactionId, result);
            } else {
                log.error("Exception responding to participant ({}), accessCode {}", participantPrivateId, accessCode);
            }
        } catch (Exception e) {
            log.error("Exception responding to participant ({})", participantPrivateId, e);
        }
    }

    @Override
    public void sendErrorResponse(String participantPrivateId, Integer transactionId, Object data, OpenViduException error) {
        RpcConnection rpcConnection = getAndRemoveTransaction(participantPrivateId, transactionId);
        if (rpcConnection == null) {
            log.error("No transaction {} found for paticipant with private id {}, unable to send result {}",
                    transactionId, participantPrivateId, data);
            return;
        }
        try {
            AccessErrorResp accessErrorResp = new AccessErrorResp(participantPrivateId, rpcConnection.getAccessInfo().getOrigin());
            accessErrorResp.setData(data == null ? null : JSONObject.parseObject(new GsonBuilder().create().toJson(data)));
            accessErrorResp.setId(transactionId);
            accessErrorResp.setError(new ErrorBean(error.getCodeValue(), error.getMessage(), participantPrivateId));

            AccessCode accessCode = accessClient.sendErrorResponse(accessErrorResp);
            if (accessCode != AccessCode.SUCCESS) {
                log.error("Exception sending error response to use ({}), accessCode {}", participantPrivateId, accessCode);
            }
        } catch (Exception e) {
            log.error("Exception sending error response to user ({})", transactionId, e);
        }


    }

    @Override
    public void sendErrorResponseWithDesc(String participantPrivateId, Integer transactionId, Object data, ErrorCodeEnum errorCodeEnum) {
        RpcConnection rpcConnection = getAndRemoveTransaction(participantPrivateId, transactionId);
        if (rpcConnection == null) {
            log.error("No transaction {} found for participant with private id {}, unable to send result {}",
                    transactionId, participantPrivateId, data);
            return;
        }
        try {
            AccessErrorResp accessErrorResp = new AccessErrorResp(participantPrivateId, rpcConnection.getAccessInfo().getOrigin());
            accessErrorResp.setData(data == null ? null : JSONObject.parseObject(new GsonBuilder().create().toJson(data)));
            accessErrorResp.setId(transactionId);
            accessErrorResp.setError(new ErrorBean(errorCodeEnum.getCode(), errorCodeEnum.getMessage(), participantPrivateId));

            AccessCode accessCode = accessClient.sendErrorResponse(accessErrorResp);
            if (accessCode == AccessCode.SUCCESS) {
                log.info("WebSocket error response session #{} - Response transaction id:{} and error:{} {}",
                        participantPrivateId, transactionId, errorCodeEnum.getCode(), errorCodeEnum.getMessage());
            } else {
                log.error("Exception sending error response to use ({}), accessCode {}", participantPrivateId, accessCode);
            }

        } catch (Exception e) {
            log.error("Exception sending error response to user ({})", transactionId, e);
        }
    }

    private AccessCode sendNotification0(final String participantPrivateId, final String method, final Object params) {
        RpcConnection rpcConnection = rpcConnections.get(participantPrivateId);
        if (rpcConnection == null) {
            log.error("No rpc session found for private id {}, unable to send notification {}: {}",
                    participantPrivateId, method, params);
            return AccessCode.FAIL;
        }

        AccessNotification dto = new AccessNotification(participantPrivateId, rpcConnection.getAccessInfo().getOrigin());
        dto.setMethod(method);
        dto.setParams(JSONObject.parseObject(new Gson().toJson(params)));
        return accessClient.sendNotification(dto);
    }

    @Override
    public void sendNotification(String participantPrivateId, String method, Object params) {
        try {
            sendNotification0(participantPrivateId, method, params);
            log.info("WebSocket notification session #{} - Notification method:{} and params: {}", participantPrivateId, method, params);
        } catch (Exception e) {
            log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
                    participantPrivateId, e);
        }
    }

    @Override
    public void sendNotificationWithoutLog(String participantPrivateId, String method, Object params) {
        try {
            sendNotification0(participantPrivateId, method, params);
        } catch (Exception e) {
            log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
                    participantPrivateId, e);
        }
    }

    @Override
    public RpcConnection closeRpcSession(String participantPrivateId) {
        RpcConnection rpcConnection = rpcConnections.remove(participantPrivateId);
        if (rpcConnection == null) {
            log.error("No session found for private id {}, unable to cleanup", participantPrivateId);
            return null;
        }

        try {
            AccessBean dto = new AccessBean(participantPrivateId, rpcConnection.getAccessInfo().getOrigin());
            accessClient.close(dto);
        } catch (Exception e) {
            log.error("Error closing session for participant with private id {}", participantPrivateId, e);
        }
        return null;
    }

    @Override
    public void sendBatchNotification(Set<Participant> participants, String method, Object params) {
        if (CollectionUtils.isEmpty(participants)) {
            return;
        }
        List<String> participantPrivateIds = participants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
        sendBatchNotification(participantPrivateIds, method, params);
    }

    @Override
    public void sendBatchNotification(List<String> participantPrivateIds, String method, Object params) {
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        for (String participantPrivateId : participantPrivateIds) {
            RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
            if (rpcSession == null) {
                log.error("No rpc session found for private id {}, unable to send notification {}: {}",
                        participantPrivateId, method, params);
                failList.add(participantPrivateId);
                continue;
            }
            try {
                sendNotification0(participantPrivateId, method, params);
                successList.add(participantPrivateId);
            } catch (Exception e) {
                failList.add(participantPrivateId);
                log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
                        participantPrivateId, e);
            }
        }
        log.info("batch WebSocket notification- Notification method:{} and params: {}" +
                "successList:{}  failList:{}", method, params, successList, failList);
    }

    @Override
    public void sendBatchNotificationConcurrent(Set<Participant> participants, String method, Object params) {
        List<String> collect = participants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
        sendBatchNotificationConcurrent(collect, method, params);
    }

    @Override
    public void sendBatchNotificationConcurrent(List<String> participantPrivateIds, String method, Object params) {
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();
        Set<String> prepares = new HashSet<>(participantPrivateIds);
        Set<String> waitingSends = new HashSet<>();
        int size = participantPrivateIds.size();
        final String sendThreadName = Thread.currentThread().getName();

        List<RpcConnection> list = rpcConnections.gets(participantPrivateIds);
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        for (RpcConnection rpcConnection : list) {
            prepares.remove(rpcConnection.getParticipantPrivateId());
            waitingSends.add(rpcConnection.getParticipantPrivateId());
            NOTIFY_THREAD_POOL.submit(() -> {
                String participantPrivateId = rpcConnection.getParticipantPrivateId();
                try {
                    this.sendNotification0(participantPrivateId, method, params);
                    successList.add(participantPrivateId);
                } catch (Exception e) {
                    failList.add(participantPrivateId);
                    log.error("{} Exception sending notification '{}': {} to participant with private id {}", sendThreadName, method, params,
                            participantPrivateId, e);
                } finally {
                    waitingSends.remove(participantPrivateId);
                    countDownLatch.countDown();
                }
            });
        }

        try {
            if (!countDownLatch.await((size * 2) + 100, TimeUnit.MILLISECONDS)) {
                log.warn("{} sendBatchNotificationConcurrent timeout method={},partSize = {}", sendThreadName, method, size);
            }
        } catch (InterruptedException e) {
            log.warn("{} sendBatchNotificationConcurrent error method={},partSize = {}", sendThreadName, method, size);
        }
        log.info("{} sendBatchNotificationConcurrent - Notification method:{} and params: {}" +
                "successList:{}, failList:{}, waitingSends:{}, prepares:{}", sendThreadName, method, params, successList, failList, waitingSends, prepares);
    }

    @Override
    public void sendBatchNotificationUuidConcurrent(Collection<String> uuids, String method, Object params) {
        final Set<String> uuidSet = new HashSet<>(uuids);
        List<String> privateIdList = rpcConnections.values().stream().filter(rpcConnection -> uuidSet.contains(rpcConnection.getUserUuid()))
                .map(RpcConnection::getParticipantPrivateId).collect(Collectors.toList());
        sendBatchNotificationConcurrent(privateIdList, method, params);
    }

    @Override
    public void showRpcConnections() {
        log.info("online rpcConnections: {}", this.rpcConnections.size());
    }

    @Override
    public RpcConnection getRpcConnection(String participantPrivateId) {
        return this.rpcConnections.get(participantPrivateId);
    }

    @Override
    public Collection<RpcConnection> getRpcConnections() {
        return this.rpcConnections.values();
    }

    @Override
    public void sendRespWithConnTransaction(Transaction t, Integer requestId, ErrorCodeEnum errorCode) {
        throw new NotSupportedException("no support");
    }

    private RpcConnection getAndRemoveTransaction(String participantPrivateId, Integer transactionId) {
        RpcConnection rpcSession = transactions.getIfPresent(getTransactionId(participantPrivateId, transactionId));
        if (rpcSession == null) {
            log.warn("Invalid WebSocket session id {}, tid{}", participantPrivateId, transactionId);
            return null;
        }
        return rpcSession;
    }

    private String getTransactionId(String privateId, Integer transactionId) {
        return privateId + "-&-" + transactionId;
    }

}
