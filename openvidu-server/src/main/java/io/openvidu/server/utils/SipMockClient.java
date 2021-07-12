package io.openvidu.server.utils;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class SipMockClient {


    public static void subscriber2publisher(Participant participant){
        if (participant.getTerminalType() == TerminalTypeEnum.S) {
            log.info("sip {} role subscriber to publisher", participant.getUuid());
            new Thread(() -> {
                SafeSleep.sleepMilliSeconds(200);
                if (participant.getRole() == OpenViduRole.SUBSCRIBER) {
                    log.warn("subscriber2publisher sip is subscriber");
                    return;
                }
                KurentoParticipant kParticipant = (KurentoParticipant) participant;
                log.info("sip {} role subscriber to publisher notify", participant.getUuid());
                kParticipant.notifyPublishChannelPass(kParticipant.getPublisher());
            }).start();
        }
    }

    public static void publisher2subscriber(Participant participant, RpcNotificationService rpcNotificationService){

    }

}
