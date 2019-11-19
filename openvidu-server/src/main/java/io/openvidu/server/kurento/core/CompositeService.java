package io.openvidu.server.kurento.core;

import io.openvidu.server.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Composite;
import org.kurento.client.MediaPipeline;

import java.util.Objects;

@Slf4j
public class CompositeService {

    private Session session;
    private MediaPipeline pipeline;

    private Composite majorComposite;
    private final Object majorCompositeCreateLock = new Object();
    private final Object majorCompositeReleaseLock = new Object();

    private Composite majorShareComposite;
    private final Object majorShareCompositeCreateLock = new Object();
    private final Object majorShareCompositeReleaseLock = new Object();


    private boolean existSharing;

    private String mixMajorStreamId;
    private String mixMajorShareStreamId;
    private String shareStreamId;

    public CompositeService(Session session) {
        this.session = session;
    }


    void createMajorComposite() {
        synchronized (majorCompositeCreateLock) {
            if (pipeline == null || majorComposite != null) {
                log.warn("create major composite: majorComposite already exists or pipeline is null.");
                return;
            }
            log.info("SESSION {}: Creating Major Composite", session.getSessionId());
            majorComposite = new Composite.Builder(this.pipeline).build();
        }
    }

    void closeMajorComposite() {
        synchronized (majorCompositeReleaseLock) {
            if (Objects.isNull(majorComposite)) {
                log.warn("majorComposite already released.");
                return;
            }
            majorComposite.release();
        }
    }

    void createMajorShareComposite() {
        synchronized (majorShareCompositeCreateLock) {
            if (pipeline == null || majorShareComposite != null) {
                log.warn("create major share composite: majorShareComposite already exists or pipeline is null.");
                return;
            }
            log.info("SESSION {}: Creating Major Share Composite", session.getSessionId());
            majorShareComposite = new Composite.Builder(this.pipeline).build();
        }
    }

    void closeMajorShareComposite() {
        synchronized (majorShareCompositeReleaseLock) {
            if (Objects.isNull(majorShareComposite)) {
                log.warn("majorShareComposite already released.");
                return;
            }
            majorShareComposite.release();
        }
    }


    public Composite getMajorComposite() {
        return majorComposite;
    }

    public Composite getMajorShareComposite() {
        return majorShareComposite;
    }

    public boolean isExistSharing() {
        return existSharing;
    }

    public void setExistSharing(boolean existSharing) {
        this.existSharing = existSharing;
    }

    public synchronized String getMixMajorStreamId() {
        return mixMajorStreamId;
    }

    public synchronized void setMixMajorStreamId(String mixMajorStreamId) {
        this.mixMajorStreamId = mixMajorStreamId;
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

    public void setPipeline(MediaPipeline pipeline) {
        this.pipeline = pipeline;
    }
}
