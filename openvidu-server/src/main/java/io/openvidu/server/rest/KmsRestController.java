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

package io.openvidu.server.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.kurento.kms.EndpointLoadManager;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.kurento.kms.KmsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

/**
 * @author Pablo Fuente (pablofuenteperez@gmail.com)
 */
@RestController
@CrossOrigin
@RequestMapping("/kms")
public class KmsRestController {

    private static final Logger log = LoggerFactory.getLogger(KmsRestController.class);

    @Autowired
    private KmsManager kmsManager;


    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public ResponseEntity<?> kmsInfo() {
        log.info("REST API: GET /kms/info");

        JsonArray json = new JsonArray();
        Collection<Kms> kmss = kmsManager.getKmss();
        Map<String, Integer> loadMap = EndpointLoadManager.calculateLoad(kmsManager.getAvailableKmss());

        for (Kms kms : kmss) {
            JsonObject jsonKms = new JsonObject();
            jsonKms.addProperty("id", kms.getId());
            jsonKms.addProperty("ip", kms.getIp());
            jsonKms.addProperty("available", kms.isKurentoClientConnected());
            jsonKms.addProperty("load", loadMap.getOrDefault(kms.getId(), 0));
            json.add(jsonKms);
        }

        log.info("GET /kms/info resp content:{}", json.toString());
        return new ResponseEntity<>(json.toString(), getResponseHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/reload", method = RequestMethod.GET)
    public ResponseEntity<?> kmsReload() {
        log.info("REST API: GET /kms/reload");
        try {
            kmsManager.reloadKms();
        } catch (Exception e) {
            log.error("kmsReload error", e);
        }
        return kmsInfo();
    }

    private HttpHeaders getResponseHeaders() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return responseHeaders;
    }

}
