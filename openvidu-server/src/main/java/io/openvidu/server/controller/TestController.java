package io.openvidu.server.controller;

import com.alibaba.fastjson.JSONObject;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Api(value = "TestController", tags = {"提供个QA人员使用"})
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    CorporationMapper corporationMapper;

    private static final String robot = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=80356c9d-697c-4787-aafa-f0f2c6061d13";
    private static final String robot_dev = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=8e67beda-cba1-475a-bdb1-541016b34a01";

    @Autowired
    RestTemplate restTemplate;

    @Value("${openvidu.url}")
    private String openviduUrl;

    @ApiOperation("查询每个企业MCU临界值")
    @GetMapping("/queryCorpMcu")
    private JSONObject queryCorpMcu(@RequestParam(value = "企业名称", defaultValue = "", required = false) String corpName) {
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        JSONObject jsonObject = new JSONObject();
        for (Corporation corporation : corporations) {
            if (StringUtils.isBlank(corpName)) {
                jsonObject.put(corporation.getCorpName(), corporation.getMcuThreshold());
            }
            if (corporation.getCorpName().contains(corpName)) {
                jsonObject.put(corporation.getCorpName(), corporation.getMcuThreshold());
            }
        }
        return jsonObject;
    }

    @ApiOperation("直接修改企业的MCU值")
    @GetMapping("/updateCorpMcu")
    private JSONObject updateCorpMcu(@RequestParam("企业名称") String corpName, @RequestParam("mcuThreshold") Integer mcuThreshold) {
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        JSONObject jsonObject = new JSONObject();
        for (Corporation corporation : corporations) {
            if (corporation.getCorpName().equals(corpName)) {
                corporation.setMcuThreshold(mcuThreshold);
                corporationMapper.updateOtherByPrimaryKey(corporation);
                jsonObject.put(corporation.getCorpName(), corporation.getMcuThreshold());

                JSONObject robotParam = new JSONObject();
                robotParam.put("msgtype", "text");
                JSONObject connect = new JSONObject();
                robotParam.put("text", connect);
                connect.put("content", openviduUrl + " 的【" + corporation.getCorpName() + "】MCU值修改为 " + mcuThreshold);

                if (openviduUrl.contains(".200")) {
                    System.out.println(restTemplate.postForObject(robot_dev, robotParam.toString(), String.class));
                } else {
                    System.out.println(restTemplate.postForObject(robot, robotParam.toString(), String.class));
                }

                return jsonObject;
            }
        }


        jsonObject.put("错误", "企业名称不存在");
        return jsonObject;
    }


    @ApiOperation("查询每个企业墙上人数临界值")
    @GetMapping("/queryCorpPublisherThreshold")
    private JSONObject queryCorpPublisherThreshold(@RequestParam(value = "企业名称", defaultValue = "", required = false) String corpName) {
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        JSONObject jsonObject = new JSONObject();
        for (Corporation corporation : corporations) {
            if (StringUtils.isBlank(corpName)) {
                jsonObject.put(corporation.getCorpName(), corporation.getSfuPublisherThreshold());
            }
            if (corporation.getCorpName().contains(corpName)) {
                jsonObject.put(corporation.getCorpName(), corporation.getSfuPublisherThreshold());
            }
        }
        return jsonObject;
    }

    @ApiOperation("直接修改企业的墙上人数临界值")
    @GetMapping("/updateCorpPublisherThreshold")
    private JSONObject updateCorpPublisherThreshold(@RequestParam("企业名称") String corpName, @RequestParam("publisherThreshold") Integer publisherThreshold) {
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        JSONObject jsonObject = new JSONObject();
        for (Corporation corporation : corporations) {
            if (corporation.getCorpName().equals(corpName)) {
                corporation.setSfuPublisherThreshold(publisherThreshold);
                corporationMapper.updateOtherByPrimaryKey(corporation);
                jsonObject.put(corporation.getCorpName(), corporation.getSfuPublisherThreshold());

                JSONObject robotParam = new JSONObject();
                robotParam.put("msgtype", "text");
                JSONObject connect = new JSONObject();
                robotParam.put("text", connect);
                connect.put("content", openviduUrl + " 的【" + corporation.getCorpName() + "】墙上人数修改为 " + publisherThreshold);

                if (openviduUrl.contains(".200")) {
                    System.out.println(restTemplate.postForObject(robot_dev, robotParam.toString(), String.class));
                } else {
                    System.out.println(restTemplate.postForObject(robot, robotParam.toString(), String.class));
                }
                return jsonObject;
            }
        }


        jsonObject.put("错误", "企业名称不存在");
        return jsonObject;
    }
}
