package io.openvidu.server.common.manage.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openvidu.server.common.dao.KmsRegistrationMapper;
import io.openvidu.server.common.manage.KmsRegistrationManage;
import io.openvidu.server.common.pojo.KmsRegistration;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author chosongi
 * @date 2020/6/3 10:32
 */
@Slf4j
@Service
public class KmsRegistrationManageImpl implements KmsRegistrationManage {

    private static final String URI_SUFFIX = "kurento";

    private static final String SEPARATOR = ",";

    private List<String> kmsUrisList;

    private Set<String> kmsUrisSet = new HashSet<>();


    @Value("${kms.uris}")
    private String kmsUris;

    @Resource
    private KmsRegistrationMapper kmsRegistrationMapper;

    @Override
    public List<String> getAllRegisterKms() throws Exception {
        String configKmsUris;
        List<KmsRegistration> kmsRegistrations = kmsRegistrationMapper.selectAllRegisterKms();
        if (!CollectionUtils.isEmpty(kmsRegistrations)) {
            String uri;
            StringBuilder stringBuilder = new StringBuilder();
            for (KmsRegistration registration : kmsRegistrations) {
                if (!StringUtils.isEmpty(uri = registration.getKmsUri())) {
                    uri = uri.endsWith("/") ? uri : uri + "/";
                    stringBuilder.append(uri).append(URI_SUFFIX).append(SEPARATOR);
                }
            }

            if (stringBuilder.length() > 0 && stringBuilder.toString().endsWith(SEPARATOR)) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            configKmsUris = stringBuilder.toString();
        } else {
            configKmsUris = kmsUris;
        }

        this.kmsUrisList = initiateKmsUris(configKmsUris);
        return kmsUrisList;
    }


    @Override
    public List<String> getRecentRegisterKms() {
        List<String> recentRegisterKms = new ArrayList<>();
        List<KmsRegistration> kmsRegistrations = kmsRegistrationMapper.selectAllRegisterKms();
        if (!CollectionUtils.isEmpty(kmsRegistrations)) {
            kmsRegistrations.forEach(kmsRegistration -> {
                String uri = kmsRegistration.getKmsUri().endsWith("/") ?
                        kmsRegistration.getKmsUri() + URI_SUFFIX : kmsRegistration.getKmsUri() + "/" + URI_SUFFIX;
                if (!kmsUrisSet.contains(uri = eraseIllegalCharacter(uri))) {
                    recentRegisterKms.add(uri);
                    kmsUrisSet.add(uri);
                }
            });
        }
        return recentRegisterKms;
    }

    private List<String> initiateKmsUris(String kmsUris) throws Exception {
        kmsUris = eraseIllegalCharacter(kmsUris);
        List<String> kmsList = kmsUris.startsWith("[") && kmsUris.endsWith("]") ?
                JsonUtils.toStringList(new Gson().fromJson(kmsUris, JsonArray.class)) : Arrays.asList(kmsUris.split(SEPARATOR));
        for (String uri : kmsList) {
            this.checkWebsocketUri(uri);
        }
        return kmsList;
    }

    private void checkWebsocketUri(String uri) throws MalformedURLException {
        try {
            String parsedUri = format2WSUri(uri);
            kmsUrisSet.add(parsedUri);
            new URL(parsedUri);
        } catch (MalformedURLException e) {
            log.error("URI {} is not a valid WebSocket endpoint", uri);
            throw e;
        }
    }

    private static String eraseIllegalCharacter(String originalStr) {
        return originalStr.replaceAll("\\s", "") // Remove all white spaces
                .replaceAll("\\\\", ""); // Remove previous escapes
    }

    private static String format2WSUri(String uri) {
        return uri.replaceAll("^ws://", "http://")
                .replaceAll("^wss://", "https://");
    }

}
