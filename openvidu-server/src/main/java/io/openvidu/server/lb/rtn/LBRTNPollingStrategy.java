package io.openvidu.server.lb.rtn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openvidu.server.common.pojo.KmsRegistration;
import io.openvidu.server.config.OpenviduConfig;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LBRTNPollingStrategy extends LBRTNStrategy {
    private List<String> rtnList;
//    private AtomicInteger counter;
    private int counter;
    private static final String SEPARATOR = ",";

    public LBRTNPollingStrategy(OpenviduConfig openviduConfig) {
        super(openviduConfig);

        try {
            String configlbRTNs = openviduConfig.getLBRTNsStrings();
            initiateRTNs(configlbRTNs);
        } catch (Exception e) {
            // TODO.
        }
    }

    @Override
    public RTNObject getRTNObject() {
        if (rtnList.isEmpty()) {
            return null;
        }

//        int index = counter.incrementAndGet() % rtnList.size();
        int index = (++counter) % rtnList.size();
        RTNObject rtnObject = new RTNObject(rtnList.get(index));
        return rtnObject;
    }

    @Override
    public List<RTNObject> getRTNObjectList() {
//        counter.incrementAndGet();
        ++counter;
        return null;
    }

    private void initiateRTNs(String rtns) throws Exception {
        rtns = eraseIllegalCharacter(rtns);
        this.rtnList = rtns.startsWith("[") && rtns.endsWith("]") ?
                JsonUtils.toStringList(new Gson().fromJson(rtns, JsonArray.class)) : Arrays.asList(rtns.split(SEPARATOR));
    }

    private static String eraseIllegalCharacter(String originalStr) {
        return originalStr.replaceAll("\\s", "") // Remove all white spaces
                .replaceAll("\\\\", ""); // Remove previous escapes
    }
}
