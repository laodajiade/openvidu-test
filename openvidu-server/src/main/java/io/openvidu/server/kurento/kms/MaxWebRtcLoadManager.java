/*
 * (C) Copyright 2017-2018 OpenVidu (https://openvidu.io/)
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxWebRtcLoadManager implements LoadManager {
    private int maxWebRtcPerKms;
    public MaxWebRtcLoadManager(int maxWebRtcPerKms) {
      this.maxWebRtcPerKms = maxWebRtcPerKms;
    }

    @Override
    public int calculateLoad(Kms kms) {
        return countWebRtcEndpoints(kms);
        /*int numWebRtcs = countWebRtcEndpoints(kms);
        if (numWebRtcs > maxWebRtcPerKms) {
            return 1;
        } else {
            return numWebRtcs / maxWebRtcPerKms;
        }*/
  }

    private synchronized int countWebRtcEndpoints(Kms kms) {
        try {
            int size = kms.getKurentoClient().getServerManager().getPipelines().size();
            log.info("kms:{}, pipeline size:{}", kms.getId(), size);
            return size;
        } catch (Throwable e) {
            log.warn("Error counting KurentoClient pipelines", e);
            return 0;
        }
    }
}
