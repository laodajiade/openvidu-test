package io.openvidu.server.kurento.mcu;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.CompositeService;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.SafeSleep;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class UnMcuThread extends Thread {

    private CompositeService compositeService;
    private Session session;

    private int counter = 0;

    private final int idle = 2;

    public UnMcuThread(CompositeService compositeService, Session session) {
        this.compositeService = compositeService;
        this.session = session;
    }

    @Override
    public void run() {
        Thread.currentThread().setName(session.getSessionId() + "_UnMcuThread");
        while (true) {
            SafeSleep.sleepMinutes(1);
            if (session.isClosed() || session.isClosing()) {
                return;
            }

            if (stillUsing()) {
                counter = 0;
                continue;
            }
            counter++;
            if (counter > idle) {
                log.info("session {} conferenceModeChange {} -> {}", session.getSessionId(), "MCU", "SFU");
                session.setConferenceMode(ConferenceModeEnum.SFU);
                SessionEventRecord.other(session.getSessionId(), "conferenceModeChanged", ConferenceModeEnum.SFU.name());
                compositeService.conferenceLayoutChangedNotify(ProtocolElements.CONFERENCE_MODE_CHANGED_NOTIFY_METHOD);
                compositeService.closeComposite();
                return;
            }
        }
    }

    private boolean stillUsing() {
        try {
            if (session.getPartSize() > (session.getPresetInfo().getMcuThreshold() / 2)) {
                return true;
            }

            Set<Participant> participants = session.getParticipants();
            for (Participant part : participants) {
                SubscriberEndpoint mixSubscriber = part.getMixSubscriber();
                if (mixSubscriber != null) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("UnMcuThread error", e);
            return true;
        }
    }

}
