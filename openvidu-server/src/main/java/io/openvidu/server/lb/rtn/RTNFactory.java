package io.openvidu.server.lb.rtn;

import io.openvidu.server.config.OpenviduConfig;

import java.util.ArrayList;
import java.util.List;

public class RTNFactory {
    private static volatile RTNFactory intsance;
    private OpenviduConfig openviduConfig;
    private LBRTNStrategy lbRTNStrategyObject = null;
    private String lbRTNStrategy;

    public static RTNFactory getInstance(OpenviduConfig openviduConfig) {
        if (intsance == null) {
            synchronized (RTNFactory.class) {
                if (intsance == null) {
                    intsance = new RTNFactory(openviduConfig);
                }
            }
        }
        return intsance;
    }

    public RTNFactory(OpenviduConfig openviduConfig) {
        this.openviduConfig = openviduConfig;

        this.lbRTNStrategy = this.openviduConfig.getLBRTNStrategy();
        if (this.lbRTNStrategy.equals("polling")) {
            lbRTNStrategyObject = new LBRTNPollingStrategy(openviduConfig);
        } else {
            // TODO.
        }
    }

    public RTNObject getRTNObject()
    {
        if (lbRTNStrategyObject == null) {
            return null;
        }

        return lbRTNStrategyObject.getRTNObject();
    }

    public List<RTNObject> getRTNObjectList()
    {
        if (lbRTNStrategyObject == null) {
            return null;
        }

        return lbRTNStrategyObject.getRTNObjectList();
    }
}
