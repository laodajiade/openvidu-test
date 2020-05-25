package io.openvidu.java.client;

/**
 * See
 * {@link SessionProperties.Builder#livingMode(LivingMode)}
 */
public enum LivingMode {

    /**
     * The session is lived automatically as soon as the first client publishes a
     * stream to the session. It is automatically stopped after last user leaves the
     * session (or until you call
     * {@link OpenVidu#stopRecording(String)}).
     */
    ALWAYS,

    /**
     * The session is not lived automatically.
     */
    MANUAL
}

