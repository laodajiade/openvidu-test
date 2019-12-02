package io.openvidu.server.kurento.core;

import io.openvidu.server.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;

import java.util.Objects;

@Slf4j
public class CompositeService {

    private Session session;
    private MediaPipeline pipeline;

//    private Composite majorComposite;
//    private final Object majorCompositeCreateLock = new Object();
//    private final Object majorCompositeReleaseLock = new Object();

    private Composite majorShareComposite;
    private final Object majorShareCompositeCreateLock = new Object();
    private final Object majorShareCompositeReleaseLock = new Object();

//    private HubPort majorHubPortOut;
//    private ListenerSubscription majorHubPortOutSubscription = null;

    private HubPort majorShareHubPortOut = null;
    private ListenerSubscription majorShareHubPortOutSubscription = null;


    private boolean existSharing;

//    private String mixMajorStreamId;
    private String mixMajorShareStreamId;
    private String shareStreamId;

    public CompositeService(Session session) {
        this.session = session;
    }


    /*void createMajorComposite() {
        synchronized (majorCompositeCreateLock) {
            if (pipeline == null || majorComposite != null) {
                log.warn("create major composite: majorComposite already exists or pipeline is null.");
                return;
            }
            log.info("SESSION {}: Creating Major Composite", session.getSessionId());
            majorComposite = new Composite.Builder(this.pipeline).build();
            createMajorHubPortOut();
        }
    }

    void closeMajorComposite() {
        releaseMajorHubPortOut();
        synchronized (majorCompositeReleaseLock) {
            if (Objects.isNull(majorComposite)) {
                log.warn("majorComposite already released.");
                return;
            }
            majorComposite.release();
        }
    }*/

    void createMajorShareComposite() {
        synchronized (majorShareCompositeCreateLock) {
            if (pipeline == null || majorShareComposite != null) {
                log.warn("create major share composite: majorShareComposite already exists or pipeline is null.");
                return;
            }
            log.info("SESSION {}: Creating Major Share Composite", session.getSessionId());
            majorShareComposite = new Composite.Builder(this.pipeline).build();
            createMajorShareHubPortOut();
        }
    }

    void closeMajorShareComposite() {
        releaseMajorShareHubPortOut();
        synchronized (majorShareCompositeReleaseLock) {
            if (Objects.isNull(majorShareComposite)) {
                log.warn("majorShareComposite already released.");
                return;
            }
            majorShareComposite.release();
        }
    }

    /*public void createMajorHubPortOut() {
        majorHubPortOut = new HubPort.Builder(getMajorComposite()).build();
        majorHubPortOutSubscription = registerElemErrListener(majorHubPortOut);
        log.info("Sub EP create majorHubPortOut.");
    }*/

//    public HubPort getMajorHubPortOut() {
//        return this.majorHubPortOut;
//    }

    public void createMajorShareHubPortOut() {
        majorShareHubPortOut = new HubPort.Builder(getMajorShareComposite()).build();
        majorShareHubPortOutSubscription = registerElemErrListener(majorShareHubPortOut);
        log.info("Sub EP create majorShareHubPortOut.");
    }

    public HubPort getMajorShareHubPortOut() {
        return this.majorShareHubPortOut;
    }

    /*public void releaseMajorHubPortOut() {
        unregisterErrorListeners(majorHubPortOut, majorHubPortOutSubscription);
        if (!Objects.isNull(majorHubPortOut)) {
            releaseElement(majorHubPortOut);
        }
    }*/

    public void releaseMajorShareHubPortOut() {
        unregisterErrorListeners(majorShareHubPortOut, majorShareHubPortOutSubscription);
        if (!Objects.isNull(majorShareHubPortOut)) {
            releaseElement(majorShareHubPortOut);
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


//    public Composite getMajorComposite() {
//        return majorComposite;
//    }

    public Composite getMajorShareComposite() {
        return majorShareComposite;
    }

    public boolean isExistSharing() {
        return existSharing;
    }

    public void setExistSharing(boolean existSharing) {
        this.existSharing = existSharing;
    }

//    public synchronized String getMixMajorStreamId() {
//        return mixMajorStreamId;
//    }

//    public synchronized void setMixMajorStreamId(String mixMajorStreamId) {
//        this.mixMajorStreamId = mixMajorStreamId;
//    }

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

    public void setPipeline(MediaPipeline pipeline) {
        this.pipeline = pipeline;
    }
}
