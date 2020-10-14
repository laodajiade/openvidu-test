package io.openvidu.server.lb.rtn;

import io.openvidu.server.config.OpenviduConfig;

import java.util.ArrayList;
import java.util.List;

public abstract class LBRTNStrategy {
    public LBRTNStrategy(OpenviduConfig openviduConfig){}

    public abstract RTNObject getRTNObject();

    public abstract List<RTNObject> getRTNObjectList();
}
