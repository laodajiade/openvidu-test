package io.openvidu.server.kurento.core;

import io.openvidu.server.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

@Slf4j
public class CompositeService {

    private Session session;
    private MediaPipeline pipeline;

    private Composite composite;
    private final Object compositeCreateLock = new Object();
    private final Object compositeReleaseLock = new Object();

    private HubPort hubPortOut = null;
    private ListenerSubscription hubPortOutSubscription = null;


    private boolean existSharing;

    private String mixMajorShareStreamId;
    private String shareStreamId;

    public CompositeService(Session session) {
        this.session = session;


    }

    void createComposite(MediaPipeline pipeline) {
        if (composite == null) {
            synchronized (compositeCreateLock) {
                if (composite != null) {
                    return;
                }

                this.pipeline = pipeline;
                log.info("SESSION {}: Creating Composite", session.getSessionId());
                composite = new Composite.Builder(this.pipeline).build();
                createHubPort();
            }
            composite.setName(session.getSessionId());
        }

//        majorShareHubPortOut.setName(session.getSessionId() + "_mix_out");
    }

    void closeComposite() {
        releaseHubPortOut();
        synchronized (compositeReleaseLock) {
            if (Objects.isNull(composite)) {
                log.warn("majorShareComposite already released.");
                return;
            }
            composite.release();
            composite = null;
            log.warn("Release MajorShareComposite");
        }
    }

    public void createHubPort() {
        hubPortOut = new HubPort.Builder(getMajorShareComposite()).build();
        hubPortOutSubscription = registerElemErrListener(hubPortOut);
        log.info("Sub EP create majorShareHubPortOut.");
    }

    public HubPort getHubPortOut() {
        return this.hubPortOut;
    }

    public void releaseHubPortOut() {
        log.info("Release MajorShareHubPortOut");
        unregisterErrorListeners(hubPortOut, hubPortOutSubscription);
        if (!Objects.isNull(hubPortOut)) {
            releaseElement(hubPortOut);
        }
    }

    private void releaseElement(final MediaElement element) {
        final String eid = element.getId();
        try {
            element.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.debug("Released successfully media element #{} ",
                            eid);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("Could not release media element #{}",
                            eid, cause);
                }
            });
        } catch (Exception e) {
            log.error("Error calling release on elem #{} for {}", eid, e);
        }
    }

    private synchronized void unregisterErrorListeners(HubPort hubPort, ListenerSubscription listenerSubscription) {
        if (!Objects.isNull(hubPort)) {
            unregisterElementErrListener(hubPort, listenerSubscription);
        }
    }

    protected ListenerSubscription registerElemErrListener(MediaElement element) {
        return element.addErrorListener(new EventListener<ErrorEvent>() {
            @Override
            public void onEvent(ErrorEvent event) {
                // owner.sendMediaError(event);
                log.warn("ListenerSubscription error.{}", event.getDescription());
            }
        });
    }

    protected void unregisterElementErrListener(MediaElement element, final ListenerSubscription subscription) {
        if (element == null || subscription == null) {
            return;
        }
        element.removeErrorListener(subscription);
    }

    public Composite getMajorShareComposite() {
        return composite;
    }

    public boolean isExistSharing() {
        return existSharing;
    }

    public void setExistSharing(boolean existSharing) {
        this.existSharing = existSharing;
    }

    public void setMixMajorShareStreamId(String mixMajorShareStreamId) {
        this.mixMajorShareStreamId = mixMajorShareStreamId;
    }

    public String getMixMajorShareStreamId() {
        return mixMajorShareStreamId;
    }

    public String getShareStreamId() {
        return shareStreamId;
    }

    public void setShareStreamId(String shareStreamId) {
        this.shareStreamId = shareStreamId;
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }
}
