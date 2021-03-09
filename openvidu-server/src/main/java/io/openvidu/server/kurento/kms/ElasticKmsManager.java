/*
 * (C) Copyright 2017-2019 OpenVidu (https://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.server.kurento.kms;

import io.openvidu.server.exception.NoSuchKmsException;
import org.kurento.client.KurentoClient;
import org.kurento.commons.exception.KurentoException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ElasticKmsManager extends KmsManager {

    private final Map<String, Kms> availableKmss = new ConcurrentHashMap<>();

    private final Map<String, Kms> unavailableKmss = new ConcurrentHashMap<>();

    @Override
    public List<Kms> initializeKurentoClients(List<String> kmsUris) {
        List<Kms> kmsList = new ArrayList<>(4);
        for (String kmsUri : kmsUris) {
            Kms kms = initializeClient(kmsUri);
            if (Objects.nonNull(kms)) {
                kmsList.add(kms);
            }
        }
        return kmsList;
    }

    @Override
    public Kms initializeClient(String kmsUri) {
        KurentoClient kClient;
        Kms kms = new Kms(kmsUri, loadManager);
        this.addKms(kms);
        try {
            kClient = KurentoClient.create(kmsUri, this.generateKurentoConnectionListener(kms.getId()));
        } catch (KurentoException e) {
            log.error("KMS in {} is not reachable by OpenVidu Server", kmsUri);
            return null;
        }

        kms.setKurentoClient(kClient);
        this.addKms(kms);
        return kms;
    }

    @Override
    public synchronized Kms getLessLoadedKms() throws NoSuchElementException {
        Kms lessKms = EndpointLoadManager.getLessKms(getAvailableKmss());
        if (Objects.isNull(lessKms)) {
            log.error("没有可用的kms服务");
            throw new NoSuchKmsException();
        }
        log.info("get less loaded kms with id {}, ip {}", lessKms.getId(), lessKms.getIp());
        return lessKms;
    }

    @Override
    public synchronized Kms getLessLoadedKms(Kms excludeKms) {
        Collection<Kms> availableKmss = getAvailableKmss();
        availableKmss.remove(excludeKms);

        Kms lessKms = EndpointLoadManager.getLessKms(getAvailableKmss());
        if (lessKms == null) {
            throw new RuntimeException("没有可用的其他kms");
        }
        return lessKms;
    }

    @Override
    protected void connected(String kmsId) {
        super.connected(kmsId);
        unavailableKmss.remove(kmsId);
        availableKmss.put(kmsId, kmss.get(kmsId));
    }

    @Override
    protected void connectionFailed(String kmsId) {
        super.connectionFailed(kmsId);
    }

    @Override
    protected void disconnected(String kmsId) {
        super.disconnected(kmsId);
    }

    @Override
    protected void reconnected(String kmsId, boolean sameServer) {
        super.reconnected(kmsId, sameServer);
    }
}
