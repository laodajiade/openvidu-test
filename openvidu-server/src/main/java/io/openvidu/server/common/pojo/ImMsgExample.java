package io.openvidu.server.common.pojo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImMsgExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public ImMsgExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdIsNull() {
            addCriterion("client_msg_id is null");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdIsNotNull() {
            addCriterion("client_msg_id is not null");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdEqualTo(String value) {
            addCriterion("client_msg_id =", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdNotEqualTo(String value) {
            addCriterion("client_msg_id <>", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdGreaterThan(String value) {
            addCriterion("client_msg_id >", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdGreaterThanOrEqualTo(String value) {
            addCriterion("client_msg_id >=", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdLessThan(String value) {
            addCriterion("client_msg_id <", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdLessThanOrEqualTo(String value) {
            addCriterion("client_msg_id <=", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdLike(String value) {
            addCriterion("client_msg_id like", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdNotLike(String value) {
            addCriterion("client_msg_id not like", value, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdIn(List<String> values) {
            addCriterion("client_msg_id in", values, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdNotIn(List<String> values) {
            addCriterion("client_msg_id not in", values, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdBetween(String value1, String value2) {
            addCriterion("client_msg_id between", value1, value2, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andClientMsgIdNotBetween(String value1, String value2) {
            addCriterion("client_msg_id not between", value1, value2, "clientMsgId");
            return (Criteria) this;
        }

        public Criteria andRuidIsNull() {
            addCriterion("ruid is null");
            return (Criteria) this;
        }

        public Criteria andRuidIsNotNull() {
            addCriterion("ruid is not null");
            return (Criteria) this;
        }

        public Criteria andRuidEqualTo(String value) {
            addCriterion("ruid =", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidNotEqualTo(String value) {
            addCriterion("ruid <>", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidGreaterThan(String value) {
            addCriterion("ruid >", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidGreaterThanOrEqualTo(String value) {
            addCriterion("ruid >=", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidLessThan(String value) {
            addCriterion("ruid <", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidLessThanOrEqualTo(String value) {
            addCriterion("ruid <=", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidLike(String value) {
            addCriterion("ruid like", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidNotLike(String value) {
            addCriterion("ruid not like", value, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidIn(List<String> values) {
            addCriterion("ruid in", values, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidNotIn(List<String> values) {
            addCriterion("ruid not in", values, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidBetween(String value1, String value2) {
            addCriterion("ruid between", value1, value2, "ruid");
            return (Criteria) this;
        }

        public Criteria andRuidNotBetween(String value1, String value2) {
            addCriterion("ruid not between", value1, value2, "ruid");
            return (Criteria) this;
        }

        public Criteria andRoomIdIsNull() {
            addCriterion("room_id is null");
            return (Criteria) this;
        }

        public Criteria andRoomIdIsNotNull() {
            addCriterion("room_id is not null");
            return (Criteria) this;
        }

        public Criteria andRoomIdEqualTo(String value) {
            addCriterion("room_id =", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdNotEqualTo(String value) {
            addCriterion("room_id <>", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdGreaterThan(String value) {
            addCriterion("room_id >", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdGreaterThanOrEqualTo(String value) {
            addCriterion("room_id >=", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdLessThan(String value) {
            addCriterion("room_id <", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdLessThanOrEqualTo(String value) {
            addCriterion("room_id <=", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdLike(String value) {
            addCriterion("room_id like", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdNotLike(String value) {
            addCriterion("room_id not like", value, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdIn(List<String> values) {
            addCriterion("room_id in", values, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdNotIn(List<String> values) {
            addCriterion("room_id not in", values, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdBetween(String value1, String value2) {
            addCriterion("room_id between", value1, value2, "roomId");
            return (Criteria) this;
        }

        public Criteria andRoomIdNotBetween(String value1, String value2) {
            addCriterion("room_id not between", value1, value2, "roomId");
            return (Criteria) this;
        }

        public Criteria andTimestampIsNull() {
            addCriterion("`timestamp` is null");
            return (Criteria) this;
        }

        public Criteria andTimestampIsNotNull() {
            addCriterion("`timestamp` is not null");
            return (Criteria) this;
        }

        public Criteria andTimestampEqualTo(Date value) {
            addCriterion("`timestamp` =", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampNotEqualTo(Date value) {
            addCriterion("`timestamp` <>", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampGreaterThan(Date value) {
            addCriterion("`timestamp` >", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampGreaterThanOrEqualTo(Date value) {
            addCriterion("`timestamp` >=", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampLessThan(Date value) {
            addCriterion("`timestamp` <", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampLessThanOrEqualTo(Date value) {
            addCriterion("`timestamp` <=", value, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampIn(List<Date> values) {
            addCriterion("`timestamp` in", values, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampNotIn(List<Date> values) {
            addCriterion("`timestamp` not in", values, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampBetween(Date value1, Date value2) {
            addCriterion("`timestamp` between", value1, value2, "timestamp");
            return (Criteria) this;
        }

        public Criteria andTimestampNotBetween(Date value1, Date value2) {
            addCriterion("`timestamp` not between", value1, value2, "timestamp");
            return (Criteria) this;
        }

        public Criteria andContentIsNull() {
            addCriterion("content is null");
            return (Criteria) this;
        }

        public Criteria andContentIsNotNull() {
            addCriterion("content is not null");
            return (Criteria) this;
        }

        public Criteria andContentEqualTo(String value) {
            addCriterion("content =", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentNotEqualTo(String value) {
            addCriterion("content <>", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentGreaterThan(String value) {
            addCriterion("content >", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentGreaterThanOrEqualTo(String value) {
            addCriterion("content >=", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentLessThan(String value) {
            addCriterion("content <", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentLessThanOrEqualTo(String value) {
            addCriterion("content <=", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentLike(String value) {
            addCriterion("content like", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentNotLike(String value) {
            addCriterion("content not like", value, "content");
            return (Criteria) this;
        }

        public Criteria andContentIn(List<String> values) {
            addCriterion("content in", values, "content");
            return (Criteria) this;
        }

        public Criteria andContentNotIn(List<String> values) {
            addCriterion("content not in", values, "content");
            return (Criteria) this;
        }

        public Criteria andContentBetween(String value1, String value2) {
            addCriterion("content between", value1, value2, "content");
            return (Criteria) this;
        }

        public Criteria andContentNotBetween(String value1, String value2) {
            addCriterion("content not between", value1, value2, "content");
            return (Criteria) this;
        }

        public Criteria andMsgTypeIsNull() {
            addCriterion("msg_type is null");
            return (Criteria) this;
        }

        public Criteria andMsgTypeIsNotNull() {
            addCriterion("msg_type is not null");
            return (Criteria) this;
        }

        public Criteria andMsgTypeEqualTo(Integer value) {
            addCriterion("msg_type =", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeNotEqualTo(Integer value) {
            addCriterion("msg_type <>", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeGreaterThan(Integer value) {
            addCriterion("msg_type >", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeGreaterThanOrEqualTo(Integer value) {
            addCriterion("msg_type >=", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeLessThan(Integer value) {
            addCriterion("msg_type <", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeLessThanOrEqualTo(Integer value) {
            addCriterion("msg_type <=", value, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeIn(List<Integer> values) {
            addCriterion("msg_type in", values, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeNotIn(List<Integer> values) {
            addCriterion("msg_type not in", values, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeBetween(Integer value1, Integer value2) {
            addCriterion("msg_type between", value1, value2, "msgType");
            return (Criteria) this;
        }

        public Criteria andMsgTypeNotBetween(Integer value1, Integer value2) {
            addCriterion("msg_type not between", value1, value2, "msgType");
            return (Criteria) this;
        }

        public Criteria andOperateIsNull() {
            addCriterion("operate is null");
            return (Criteria) this;
        }

        public Criteria andOperateIsNotNull() {
            addCriterion("operate is not null");
            return (Criteria) this;
        }

        public Criteria andOperateEqualTo(Integer value) {
            addCriterion("operate =", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateNotEqualTo(Integer value) {
            addCriterion("operate <>", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateGreaterThan(Integer value) {
            addCriterion("operate >", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateGreaterThanOrEqualTo(Integer value) {
            addCriterion("operate >=", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateLessThan(Integer value) {
            addCriterion("operate <", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateLessThanOrEqualTo(Integer value) {
            addCriterion("operate <=", value, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateIn(List<Integer> values) {
            addCriterion("operate in", values, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateNotIn(List<Integer> values) {
            addCriterion("operate not in", values, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateBetween(Integer value1, Integer value2) {
            addCriterion("operate between", value1, value2, "operate");
            return (Criteria) this;
        }

        public Criteria andOperateNotBetween(Integer value1, Integer value2) {
            addCriterion("operate not between", value1, value2, "operate");
            return (Criteria) this;
        }

        public Criteria andAtAccountIsNull() {
            addCriterion("at_account is null");
            return (Criteria) this;
        }

        public Criteria andAtAccountIsNotNull() {
            addCriterion("at_account is not null");
            return (Criteria) this;
        }

        public Criteria andAtAccountEqualTo(String value) {
            addCriterion("at_account =", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountNotEqualTo(String value) {
            addCriterion("at_account <>", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountGreaterThan(String value) {
            addCriterion("at_account >", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountGreaterThanOrEqualTo(String value) {
            addCriterion("at_account >=", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountLessThan(String value) {
            addCriterion("at_account <", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountLessThanOrEqualTo(String value) {
            addCriterion("at_account <=", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountLike(String value) {
            addCriterion("at_account like", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountNotLike(String value) {
            addCriterion("at_account not like", value, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountIn(List<String> values) {
            addCriterion("at_account in", values, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountNotIn(List<String> values) {
            addCriterion("at_account not in", values, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountBetween(String value1, String value2) {
            addCriterion("at_account between", value1, value2, "atAccount");
            return (Criteria) this;
        }

        public Criteria andAtAccountNotBetween(String value1, String value2) {
            addCriterion("at_account not between", value1, value2, "atAccount");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdIsNull() {
            addCriterion("sender_user_id is null");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdIsNotNull() {
            addCriterion("sender_user_id is not null");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdEqualTo(Long value) {
            addCriterion("sender_user_id =", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdNotEqualTo(Long value) {
            addCriterion("sender_user_id <>", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdGreaterThan(Long value) {
            addCriterion("sender_user_id >", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdGreaterThanOrEqualTo(Long value) {
            addCriterion("sender_user_id >=", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdLessThan(Long value) {
            addCriterion("sender_user_id <", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdLessThanOrEqualTo(Long value) {
            addCriterion("sender_user_id <=", value, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdIn(List<Long> values) {
            addCriterion("sender_user_id in", values, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdNotIn(List<Long> values) {
            addCriterion("sender_user_id not in", values, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdBetween(Long value1, Long value2) {
            addCriterion("sender_user_id between", value1, value2, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUserIdNotBetween(Long value1, Long value2) {
            addCriterion("sender_user_id not between", value1, value2, "senderUserId");
            return (Criteria) this;
        }

        public Criteria andSenderUuidIsNull() {
            addCriterion("sender_uuid is null");
            return (Criteria) this;
        }

        public Criteria andSenderUuidIsNotNull() {
            addCriterion("sender_uuid is not null");
            return (Criteria) this;
        }

        public Criteria andSenderUuidEqualTo(String value) {
            addCriterion("sender_uuid =", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidNotEqualTo(String value) {
            addCriterion("sender_uuid <>", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidGreaterThan(String value) {
            addCriterion("sender_uuid >", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidGreaterThanOrEqualTo(String value) {
            addCriterion("sender_uuid >=", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidLessThan(String value) {
            addCriterion("sender_uuid <", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidLessThanOrEqualTo(String value) {
            addCriterion("sender_uuid <=", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidLike(String value) {
            addCriterion("sender_uuid like", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidNotLike(String value) {
            addCriterion("sender_uuid not like", value, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidIn(List<String> values) {
            addCriterion("sender_uuid in", values, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidNotIn(List<String> values) {
            addCriterion("sender_uuid not in", values, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidBetween(String value1, String value2) {
            addCriterion("sender_uuid between", value1, value2, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUuidNotBetween(String value1, String value2) {
            addCriterion("sender_uuid not between", value1, value2, "senderUuid");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameIsNull() {
            addCriterion("sender_username is null");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameIsNotNull() {
            addCriterion("sender_username is not null");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameEqualTo(String value) {
            addCriterion("sender_username =", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameNotEqualTo(String value) {
            addCriterion("sender_username <>", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameGreaterThan(String value) {
            addCriterion("sender_username >", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameGreaterThanOrEqualTo(String value) {
            addCriterion("sender_username >=", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameLessThan(String value) {
            addCriterion("sender_username <", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameLessThanOrEqualTo(String value) {
            addCriterion("sender_username <=", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameLike(String value) {
            addCriterion("sender_username like", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameNotLike(String value) {
            addCriterion("sender_username not like", value, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameIn(List<String> values) {
            addCriterion("sender_username in", values, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameNotIn(List<String> values) {
            addCriterion("sender_username not in", values, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameBetween(String value1, String value2) {
            addCriterion("sender_username between", value1, value2, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderUsernameNotBetween(String value1, String value2) {
            addCriterion("sender_username not between", value1, value2, "senderUsername");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeIsNull() {
            addCriterion("sender_terminal_type is null");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeIsNotNull() {
            addCriterion("sender_terminal_type is not null");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeEqualTo(String value) {
            addCriterion("sender_terminal_type =", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeNotEqualTo(String value) {
            addCriterion("sender_terminal_type <>", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeGreaterThan(String value) {
            addCriterion("sender_terminal_type >", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeGreaterThanOrEqualTo(String value) {
            addCriterion("sender_terminal_type >=", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeLessThan(String value) {
            addCriterion("sender_terminal_type <", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeLessThanOrEqualTo(String value) {
            addCriterion("sender_terminal_type <=", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeLike(String value) {
            addCriterion("sender_terminal_type like", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeNotLike(String value) {
            addCriterion("sender_terminal_type not like", value, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeIn(List<String> values) {
            addCriterion("sender_terminal_type in", values, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeNotIn(List<String> values) {
            addCriterion("sender_terminal_type not in", values, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeBetween(String value1, String value2) {
            addCriterion("sender_terminal_type between", value1, value2, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andSenderTerminalTypeNotBetween(String value1, String value2) {
            addCriterion("sender_terminal_type not between", value1, value2, "senderTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdIsNull() {
            addCriterion("revicer_user_id is null");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdIsNotNull() {
            addCriterion("revicer_user_id is not null");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdEqualTo(Long value) {
            addCriterion("revicer_user_id =", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdNotEqualTo(Long value) {
            addCriterion("revicer_user_id <>", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdGreaterThan(Long value) {
            addCriterion("revicer_user_id >", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdGreaterThanOrEqualTo(Long value) {
            addCriterion("revicer_user_id >=", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdLessThan(Long value) {
            addCriterion("revicer_user_id <", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdLessThanOrEqualTo(Long value) {
            addCriterion("revicer_user_id <=", value, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdIn(List<Long> values) {
            addCriterion("revicer_user_id in", values, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdNotIn(List<Long> values) {
            addCriterion("revicer_user_id not in", values, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdBetween(Long value1, Long value2) {
            addCriterion("revicer_user_id between", value1, value2, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUserIdNotBetween(Long value1, Long value2) {
            addCriterion("revicer_user_id not between", value1, value2, "revicerUserId");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidIsNull() {
            addCriterion("revicer_uuid is null");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidIsNotNull() {
            addCriterion("revicer_uuid is not null");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidEqualTo(String value) {
            addCriterion("revicer_uuid =", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidNotEqualTo(String value) {
            addCriterion("revicer_uuid <>", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidGreaterThan(String value) {
            addCriterion("revicer_uuid >", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidGreaterThanOrEqualTo(String value) {
            addCriterion("revicer_uuid >=", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidLessThan(String value) {
            addCriterion("revicer_uuid <", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidLessThanOrEqualTo(String value) {
            addCriterion("revicer_uuid <=", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidLike(String value) {
            addCriterion("revicer_uuid like", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidNotLike(String value) {
            addCriterion("revicer_uuid not like", value, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidIn(List<String> values) {
            addCriterion("revicer_uuid in", values, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidNotIn(List<String> values) {
            addCriterion("revicer_uuid not in", values, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidBetween(String value1, String value2) {
            addCriterion("revicer_uuid between", value1, value2, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUuidNotBetween(String value1, String value2) {
            addCriterion("revicer_uuid not between", value1, value2, "revicerUuid");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameIsNull() {
            addCriterion("revicer_username is null");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameIsNotNull() {
            addCriterion("revicer_username is not null");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameEqualTo(String value) {
            addCriterion("revicer_username =", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameNotEqualTo(String value) {
            addCriterion("revicer_username <>", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameGreaterThan(String value) {
            addCriterion("revicer_username >", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameGreaterThanOrEqualTo(String value) {
            addCriterion("revicer_username >=", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameLessThan(String value) {
            addCriterion("revicer_username <", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameLessThanOrEqualTo(String value) {
            addCriterion("revicer_username <=", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameLike(String value) {
            addCriterion("revicer_username like", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameNotLike(String value) {
            addCriterion("revicer_username not like", value, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameIn(List<String> values) {
            addCriterion("revicer_username in", values, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameNotIn(List<String> values) {
            addCriterion("revicer_username not in", values, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameBetween(String value1, String value2) {
            addCriterion("revicer_username between", value1, value2, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerUsernameNotBetween(String value1, String value2) {
            addCriterion("revicer_username not between", value1, value2, "revicerUsername");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeIsNull() {
            addCriterion("revicer_terminal_type is null");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeIsNotNull() {
            addCriterion("revicer_terminal_type is not null");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeEqualTo(String value) {
            addCriterion("revicer_terminal_type =", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeNotEqualTo(String value) {
            addCriterion("revicer_terminal_type <>", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeGreaterThan(String value) {
            addCriterion("revicer_terminal_type >", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeGreaterThanOrEqualTo(String value) {
            addCriterion("revicer_terminal_type >=", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeLessThan(String value) {
            addCriterion("revicer_terminal_type <", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeLessThanOrEqualTo(String value) {
            addCriterion("revicer_terminal_type <=", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeLike(String value) {
            addCriterion("revicer_terminal_type like", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeNotLike(String value) {
            addCriterion("revicer_terminal_type not like", value, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeIn(List<String> values) {
            addCriterion("revicer_terminal_type in", values, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeNotIn(List<String> values) {
            addCriterion("revicer_terminal_type not in", values, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeBetween(String value1, String value2) {
            addCriterion("revicer_terminal_type between", value1, value2, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andRevicerTerminalTypeNotBetween(String value1, String value2) {
            addCriterion("revicer_terminal_type not between", value1, value2, "revicerTerminalType");
            return (Criteria) this;
        }

        public Criteria andExtIsNull() {
            addCriterion("ext is null");
            return (Criteria) this;
        }

        public Criteria andExtIsNotNull() {
            addCriterion("ext is not null");
            return (Criteria) this;
        }

        public Criteria andExtEqualTo(String value) {
            addCriterion("ext =", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtNotEqualTo(String value) {
            addCriterion("ext <>", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtGreaterThan(String value) {
            addCriterion("ext >", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtGreaterThanOrEqualTo(String value) {
            addCriterion("ext >=", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtLessThan(String value) {
            addCriterion("ext <", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtLessThanOrEqualTo(String value) {
            addCriterion("ext <=", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtLike(String value) {
            addCriterion("ext like", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtNotLike(String value) {
            addCriterion("ext not like", value, "ext");
            return (Criteria) this;
        }

        public Criteria andExtIn(List<String> values) {
            addCriterion("ext in", values, "ext");
            return (Criteria) this;
        }

        public Criteria andExtNotIn(List<String> values) {
            addCriterion("ext not in", values, "ext");
            return (Criteria) this;
        }

        public Criteria andExtBetween(String value1, String value2) {
            addCriterion("ext between", value1, value2, "ext");
            return (Criteria) this;
        }

        public Criteria andExtNotBetween(String value1, String value2) {
            addCriterion("ext not between", value1, value2, "ext");
            return (Criteria) this;
        }
    }

    /**
     */
    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}