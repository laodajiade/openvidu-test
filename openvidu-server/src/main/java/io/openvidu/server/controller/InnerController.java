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

package io.openvidu.server.controller;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.rpc.handlers.GetNotFinishedRoomHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@CrossOrigin
@RequestMapping("/inner")
@Slf4j
public class InnerController {

    @Autowired
    private GetNotFinishedRoomHandler getNotFinishedRoomHandler;

    @Resource
    protected CacheManage cacheManage;

    @GetMapping(value = "getNotFinishedRoom")
    public @ResponseBody
    String handler(@RequestParam("uuid") String uuid) {
        return getNotFinishedRoomHandler.getInfo(uuid, false).toString();
    }


}
