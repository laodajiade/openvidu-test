package io.openvidu.java.client;

import io.openvidu.java.client.Recording.OutputMode;


public class LivingProperties {

    private String name;
    private OutputMode outputMode;
    private LivingLayout livingLayout;
    private String customLayout;
    private String resolution;
    private boolean hasAudio;
    private boolean hasVideo;

    /**
     * Builder for {@link LivingProperties}
     */
    public static class Builder {

        private String name = "";
        private OutputMode outputMode = OutputMode.COMPOSED;
        private LivingLayout livingLayout;
        private String customLayout;
        private String resolution;
        private boolean hasAudio = true;
        private boolean hasVideo = true;

        /**
         * Builder for {@link LivingProperties}
         */
        public LivingProperties build() {
            if (OutputMode.COMPOSED.equals(this.outputMode)) {
                this.livingLayout = this.livingLayout != null ? this.livingLayout : LivingLayout.BEST_FIT;
                this.resolution = this.resolution != null ? this.resolution : "1920x1080";
                if (RecordingLayout.CUSTOM.equals(this.livingLayout)) {
                    this.customLayout = this.customLayout != null ? this.customLayout : "";
                }
            }
            return new LivingProperties(this.name, this.outputMode, this.livingLayout, this.customLayout,
                    this.resolution, this.hasAudio, this.hasVideo);
        }

        /**
         * Call this method to set the name of the video file. You can access this same
         * value in your clients on recording events (<code>recordingStarted</code>,
         * <code>recordingStopped</code>)
         */
        public LivingProperties.Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Call this method to set the mode of recording: COMPOSED for a single archive
         * in a grid layout or INDIVIDUAL for one archive for each stream
         */
        public LivingProperties.Builder outputMode(OutputMode outputMode) {
            this.outputMode = outputMode;
            return this;
        }

        /**
         * Call this method to set the layout to be used in the recording. Will only
         * have effect if
         * {@link LivingProperties.Builder#outputMode(OutputMode)}
         * has been called with value
         * {@link OutputMode#COMPOSED}
         */
        public LivingProperties.Builder livingLayout(LivingLayout layout) {
            this.livingLayout = layout;
            return this;
        }

        /**
         * If setting
         * {@link LivingProperties.Builder#recordingLayout(RecordingLayout)}
         * to {@link RecordingLayout#CUSTOM} you can call this
         * method to set the relative path to the specific custom layout you want to
         * use.<br>
         * Will only have effect if
         * {@link LivingProperties.Builder#outputMode(OutputMode)}
         * has been called with value
         * {@link OutputMode#COMPOSED}.<br>
         * See <a href=
         * "https://openvidu.io/docs/advanced-features/recording#custom-recording-layouts"
         * target="_blank">Custom recording layouts</a> to learn more
         */
        public LivingProperties.Builder customLayout(String path) {
            this.customLayout = path;
            return this;
        }

        /**
         * Call this method to specify the recording resolution. Must be a string with
         * format "WIDTHxHEIGHT", being both WIDTH and HEIGHT the number of pixels
         * between 100 and 1999.<br>
         * Will only have effect if
         * {@link LivingProperties.Builder#outputMode(OutputMode)}
         * has been called with value
         * {@link OutputMode#COMPOSED}. For
         * {@link OutputMode#INDIVIDUAL} all
         * individual video files will have the native resolution of the published
         * stream
         */
        public LivingProperties.Builder resolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        /**
         * Call this method to specify whether to record audio or not. Cannot be set to
         * false at the same time as {@link hasVideo(boolean)}
         */
        public LivingProperties.Builder hasAudio(boolean hasAudio) {
            this.hasAudio = hasAudio;
            return this;
        }

        /**
         * Call this method to specify whether to record video or not. Cannot be set to
         * false at the same time as {@link hasAudio(boolean)}
         */
        public LivingProperties.Builder hasVideo(boolean hasVideo) {
            this.hasVideo = hasVideo;
            return this;
        }

    }

    protected LivingProperties(String name, OutputMode outputMode, LivingLayout layout,
                               String customLayout, String resolution, boolean hasAudio, boolean hasVideo) {
        this.name = name;
        this.outputMode = outputMode;
        this.livingLayout = layout;
        this.customLayout = customLayout;
        this.resolution = resolution;
        this.hasAudio = hasAudio;
        this.hasVideo = hasVideo;
    }

    /**
     * Defines the name you want to give to the video file. You can access this same
     * value in your clients on recording events (<code>recordingStarted</code>,
     * <code>recordingStopped</code>)
     */
    public String name() {
        return this.name;
    }

    /**
     * Defines the mode of recording: {@link OutputMode#COMPOSED} for a
     * single archive in a grid layout or {@link OutputMode#INDIVIDUAL}
     * for one archive for each stream.<br>
     * <br>
     * <p>
     * Default to {@link OutputMode#COMPOSED}
     */
    public OutputMode outputMode() {
        return this.outputMode;
    }

    /**
     * Defines the layout to be used in the recording.<br>
     * Will only have effect if
     * {@link LivingProperties.Builder#outputMode(OutputMode)}
     * has been called with value {@link OutputMode#COMPOSED}.<br>
     * <br>
     * <p>
     * Default to {@link RecordingLayout#BEST_FIT}
     */
    public LivingLayout livingLayout() {
        return this.livingLayout;
    }

    /**
     * If {@link LivingProperties#recordingLayout()} is
     * set to {@link RecordingLayout#CUSTOM}, this property
     * defines the relative path to the specific custom layout you want to use.<br>
     * See <a href=
     * "https://openvidu.io/docs/advanced-features/recording#custom-recording-layouts"
     * target="_blank">Custom recording layouts</a> to learn more
     */
    public String customLayout() {
        return this.customLayout;
    }

    /**
     * Defines the resolution of the recorded video.<br>
     * Will only have effect if
     * {@link LivingProperties.Builder#outputMode(OutputMode)}
     * has been called with value
     * {@link OutputMode#COMPOSED}. For
     * {@link OutputMode#INDIVIDUAL} all
     * individual video files will have the native resolution of the published
     * stream.<br>
     * <br>
     * <p>
     * Default to "1920x1080"
     */
    public String resolution() {
        return this.resolution;
    }

    /**
     * Defines whether to record audio or not. Cannot be set to false at the same
     * time as {@link hasVideo()}.<br>
     * <br>
     * <p>
     * Default to true
     */
    public boolean hasAudio() {
        return this.hasAudio;
    }

    /**
     * Defines whether to record video or not. Cannot be set to false at the same
     * time as {@link hasAudio()}.<br>
     * <br>
     * <p>
     * Default to true
     */
    public boolean hasVideo() {
        return this.hasVideo;
    }

}

