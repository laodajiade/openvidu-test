package io.openvidu.server.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.*;

import static io.openvidu.client.internal.ProtocolElements.*;

@Configuration
public class FlowRulesConfig {

    private final List<FlowRule> rules = new ArrayList<>();
    private static final Map<String, String> groupMap = new HashMap<>();

    @Value("${flow.rule.modulus:1}")
    private double modulus = 1;

    private static final String CONTACT_LIST_GROUP = "CONTACT_LIST_GROUP";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    @PostConstruct
    public void init() {
        initGroup();

        //
        addRule(ACCESS_IN_METHOD, 50, null);
        addRule(PUBLISHVIDEO_METHOD, 50, null);
        addRule(RECEIVEVIDEO_METHOD, 50, null);
        addRule(GET_PARTICIPANTS_METHOD, 50, null);
        addRule("updateParticipantsOrder", 50, null);

        // 通讯录
        addRule("getDepartmentTree", 50, CONTACT_LIST_GROUP);
        addRule("getSubDevOrUser", 50, CONTACT_LIST_GROUP);
        addRule("getSubDevOrUserByDeptIds", 50, CONTACT_LIST_GROUP);
        addRule("getGroupList", 50, CONTACT_LIST_GROUP);
        addRule("getGroupMember", 50, CONTACT_LIST_GROUP);
        addRule("getSpecificPageOfDept", 50, CONTACT_LIST_GROUP);
        addRule("getSpecificPageOfMember", 50, CONTACT_LIST_GROUP);
        addRule("getMemberDetails", 50, CONTACT_LIST_GROUP);
        addRule("recursiveQueryUser", 50, CONTACT_LIST_GROUP);


        FlowRuleManager.loadRules(rules);
    }

    public void initGroup() {
        FlowRule rule = new FlowRule();
        rule.setResource(CONTACT_LIST_GROUP);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(300 * modulus);
        rules.add(rule);
    }


    public FlowRulesConfig addRule(String method, double cnt, String groupName) {
        FlowRule rule = new FlowRule();
        rule.setResource(method);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to cnt.
        rule.setCount(cnt * modulus);

        rules.add(rule);
        groupMap.put(method, Optional.ofNullable(groupName).orElse(DEFAULT_GROUP));
        return this;
    }

    public String getGroup(String method) {
        return groupMap.getOrDefault(method, DEFAULT_GROUP);
    }
}
