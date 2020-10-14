package io.openvidu.server.utils;

import com.google.gson.JsonObject;
import com.sensegigit.cockcrow.enums.NotifyHandler;
import com.sensegigit.cockcrow.pojo.CrowOnceInfo;

import java.util.Date;
import java.util.Objects;

public class CrowOnceInfoManager {

    public static CrowOnceInfo createCrowOnceInfo(Long jobGroup, NotifyHandler notifyHandler, Date executeTime, String jobDesc, JsonObject param) {
        CrowOnceInfo crowOnceInfo = new CrowOnceInfo();
        crowOnceInfo.setJobGroup(jobGroup);
        crowOnceInfo.setNotifyHandler(notifyHandler);
        crowOnceInfo.setJobOnceExecuteTime(executeTime);
        crowOnceInfo.setJobDesc(jobDesc);
        crowOnceInfo.setExecutorParam(Objects.isNull(param) ? new JsonObject().toString() : param.toString());
        return crowOnceInfo;
    }
}
