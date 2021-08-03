package io.openvidu.server.common.constants;

/**
 * @author chosongi
 * @date 2020/3/6 18:23
 */
public class CommonConstants {
    public static final String DEFAULT_PROJECT = "Base";
    public static final String DEVICE_ABILITY_MULTICASTPALY = "MultiCastPlay";

    public static final String MQ_METHOD_DEL_RECORDING_FILE = "deleteRecordingFiles";
    public static final String RECORD_STOP_BY_MODERATOR = "closeByModerator";
    public static final String RECORD_STOP_BY_FILL_IN_STORAGE = "storageOverLimit";
    public static final String RECORD_STORAGE_LESS_THAN_TEN_PERCENT = "storageLessThan10Percent";
    public static final String RECORD_REBUILD_TASK = "rebuildRecordTask";
    public static final String SERVER_INTERNAL_ERROR = "serverInternalError";

    /**
     * 混流的streamId的特征
     */
    public static final String MIX_STREAM_ID_TRAIT = "_MIX_";
}
