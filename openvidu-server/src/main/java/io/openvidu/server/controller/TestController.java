package io.openvidu.server.controller;

import com.alibaba.fastjson.JSONObject;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(value = "TestController", tags = {"提供个QA人员使用"})
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    CorporationMapper corporationMapper;

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
                return jsonObject;
            }
        }


        jsonObject.put("错误", "企业名称不存在");
        return jsonObject;
    }

}
