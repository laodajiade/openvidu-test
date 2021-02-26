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

import org.kurento.client.KurentoClient;
import org.kurento.commons.exception.KurentoException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ElasticKmsManager extends KmsManager {


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
        for (Kms kms : getKmss()) {
            if (kms.getIp().contains("201")) {
                log.info("return 201 kms");
                return kms;
            }
        }
        return super.getLessLoadedKms();
    }

    @Override
    public synchronized Kms getLessLoadedKms(Kms excludeKms) {
        for (Kms kms : kmss.values()) {
            if (kms.getId().equals(excludeKms.getId())) {
                continue;
            }
            log.info("return other kms by ip {} id {}", kms.getIp(), kms.getId());
            return kms;
        }
        throw new RuntimeException("没有可用的其他kms");
    }
}
