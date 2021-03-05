package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.DeliveryKmsManager;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.kurento.kms.EndpointLoadManager;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class TestStartConferenceRecordHandler extends RpcAbstractHandler {

    @Autowired
    KmsManager kmsManager;
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = rpcConnection.getSessionId();
        Session session = sessionManager.getSession(sessionId);
        KurentoSession kSession = (KurentoSession) session;

        Kms kms = kSession.getKms();
        JsonObject result = new JsonObject();

        if (true) {
            EndpointLoadManager.getLessKms(kmsManager.getKmss());
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);
            return;
        }


        Collection<KurentoSession> kurentoSessions = kms.getKurentoSessions();

        result.addProperty("sessionSize", kurentoSessions.size());
        Map<String, Integer> publishMap = new HashMap<>();
        Map<String, Integer> subscriberMap = new HashMap<>();
        for (KurentoSession kurentoSession : kurentoSessions) {
            Set<Participant> participants = kurentoSession.getParticipants();

            for (Participant participant : participants) {
                KurentoParticipant kp = (KurentoParticipant) participant;
                if (kp.isPublisherStreaming()) {
                    String id = kp.getPipeline().getId();
                    Integer cnt = publishMap.getOrDefault(id, 0);
                    publishMap.put(id, ++cnt);
                }

                log.info("kp sub size " + kp.getSubscribers().size());
                for (SubscriberEndpoint value : kp.getSubscribers().values()) {
                    if (value.getStreamId() != null) {
                        String id = value.getPipeline().getId();
                        Integer cnt = subscriberMap.getOrDefault(id, 0);
                        subscriberMap.put(id, ++cnt);
                    }
                }
            }
        }

        log.info("publishMap " + publishMap);
        log.info("subscriberMap " + subscriberMap);

        List<DeliveryKmsManager> deliveryKmsManagers = kSession.getDeliveryKmsManagers();
        result.addProperty("deliverySize",deliveryKmsManagers.size());

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);
    }

}