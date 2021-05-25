package io.openvidu.server.common.pojo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FixedRoomExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public FixedRoomExample() {
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

        public Criteria andRoomNameIsNull() {
            addCriterion("room_name is null");
            return (Criteria) this;
        }

        public Criteria andRoomNameIsNotNull() {
            addCriterion("room_name is not null");
            return (Criteria) this;
        }

        public Criteria andRoomNameEqualTo(String value) {
            addCriterion("room_name =", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameNotEqualTo(String value) {
            addCriterion("room_name <>", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameGreaterThan(String value) {
            addCriterion("room_name >", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameGreaterThanOrEqualTo(String value) {
            addCriterion("room_name >=", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameLessThan(String value) {
            addCriterion("room_name <", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameLessThanOrEqualTo(String value) {
            addCriterion("room_name <=", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameLike(String value) {
            addCriterion("room_name like", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameNotLike(String value) {
            addCriterion("room_name not like", value, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameIn(List<String> values) {
            addCriterion("room_name in", values, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameNotIn(List<String> values) {
            addCriterion("room_name not in", values, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameBetween(String value1, String value2) {
            addCriterion("room_name between", value1, value2, "roomName");
            return (Criteria) this;
        }

        public Criteria andRoomNameNotBetween(String value1, String value2) {
            addCriterion("room_name not between", value1, value2, "roomName");
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

        public Criteria andCorpIdIsNull() {
            addCriterion("corp_id is null");
            return (Criteria) this;
        }

        public Criteria andCorpIdIsNotNull() {
            addCriterion("corp_id is not null");
            return (Criteria) this;
        }

        public Criteria andCorpIdEqualTo(Long value) {
            addCriterion("corp_id =", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdNotEqualTo(Long value) {
            addCriterion("corp_id <>", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdGreaterThan(Long value) {
            addCriterion("corp_id >", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdGreaterThanOrEqualTo(Long value) {
            addCriterion("corp_id >=", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdLessThan(Long value) {
            addCriterion("corp_id <", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdLessThanOrEqualTo(Long value) {
            addCriterion("corp_id <=", value, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdIn(List<Long> values) {
            addCriterion("corp_id in", values, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdNotIn(List<Long> values) {
            addCriterion("corp_id not in", values, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdBetween(Long value1, Long value2) {
            addCriterion("corp_id between", value1, value2, "corpId");
            return (Criteria) this;
        }

        public Criteria andCorpIdNotBetween(Long value1, Long value2) {
            addCriterion("corp_id not between", value1, value2, "corpId");
            return (Criteria) this;
        }

        public Criteria andShortIdIsNull() {
            addCriterion("short_id is null");
            return (Criteria) this;
        }

        public Criteria andShortIdIsNotNull() {
            addCriterion("short_id is not null");
            return (Criteria) this;
        }

        public Criteria andShortIdEqualTo(String value) {
            addCriterion("short_id =", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdNotEqualTo(String value) {
            addCriterion("short_id <>", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdGreaterThan(String value) {
            addCriterion("short_id >", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdGreaterThanOrEqualTo(String value) {
            addCriterion("short_id >=", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdLessThan(String value) {
            addCriterion("short_id <", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdLessThanOrEqualTo(String value) {
            addCriterion("short_id <=", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdLike(String value) {
            addCriterion("short_id like", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdNotLike(String value) {
            addCriterion("short_id not like", value, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdIn(List<String> values) {
            addCriterion("short_id in", values, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdNotIn(List<String> values) {
            addCriterion("short_id not in", values, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdBetween(String value1, String value2) {
            addCriterion("short_id between", value1, value2, "shortId");
            return (Criteria) this;
        }

        public Criteria andShortIdNotBetween(String value1, String value2) {
            addCriterion("short_id not between", value1, value2, "shortId");
            return (Criteria) this;
        }

        public Criteria andCardIdIsNull() {
            addCriterion("card_id is null");
            return (Criteria) this;
        }

        public Criteria andCardIdIsNotNull() {
            addCriterion("card_id is not null");
            return (Criteria) this;
        }

        public Criteria andCardIdEqualTo(Long value) {
            addCriterion("card_id =", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdNotEqualTo(Long value) {
            addCriterion("card_id <>", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdGreaterThan(Long value) {
            addCriterion("card_id >", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdGreaterThanOrEqualTo(Long value) {
            addCriterion("card_id >=", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdLessThan(Long value) {
            addCriterion("card_id <", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdLessThanOrEqualTo(Long value) {
            addCriterion("card_id <=", value, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdIn(List<Long> values) {
            addCriterion("card_id in", values, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdNotIn(List<Long> values) {
            addCriterion("card_id not in", values, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdBetween(Long value1, Long value2) {
            addCriterion("card_id between", value1, value2, "cardId");
            return (Criteria) this;
        }

        public Criteria andCardIdNotBetween(Long value1, Long value2) {
            addCriterion("card_id not between", value1, value2, "cardId");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityIsNull() {
            addCriterion("room_capacity is null");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityIsNotNull() {
            addCriterion("room_capacity is not null");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityEqualTo(Integer value) {
            addCriterion("room_capacity =", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityNotEqualTo(Integer value) {
            addCriterion("room_capacity <>", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityGreaterThan(Integer value) {
            addCriterion("room_capacity >", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityGreaterThanOrEqualTo(Integer value) {
            addCriterion("room_capacity >=", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityLessThan(Integer value) {
            addCriterion("room_capacity <", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityLessThanOrEqualTo(Integer value) {
            addCriterion("room_capacity <=", value, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityIn(List<Integer> values) {
            addCriterion("room_capacity in", values, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityNotIn(List<Integer> values) {
            addCriterion("room_capacity not in", values, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityBetween(Integer value1, Integer value2) {
            addCriterion("room_capacity between", value1, value2, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andRoomCapacityNotBetween(Integer value1, Integer value2) {
            addCriterion("room_capacity not between", value1, value2, "roomCapacity");
            return (Criteria) this;
        }

        public Criteria andPasswordIsNull() {
            addCriterion("`password` is null");
            return (Criteria) this;
        }

        public Criteria andPasswordIsNotNull() {
            addCriterion("`password` is not null");
            return (Criteria) this;
        }

        public Criteria andPasswordEqualTo(String value) {
            addCriterion("`password` =", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordNotEqualTo(String value) {
            addCriterion("`password` <>", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordGreaterThan(String value) {
            addCriterion("`password` >", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordGreaterThanOrEqualTo(String value) {
            addCriterion("`password` >=", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordLessThan(String value) {
            addCriterion("`password` <", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordLessThanOrEqualTo(String value) {
            addCriterion("`password` <=", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordLike(String value) {
            addCriterion("`password` like", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordNotLike(String value) {
            addCriterion("`password` not like", value, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordIn(List<String> values) {
            addCriterion("`password` in", values, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordNotIn(List<String> values) {
            addCriterion("`password` not in", values, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordBetween(String value1, String value2) {
            addCriterion("`password` between", value1, value2, "password");
            return (Criteria) this;
        }

        public Criteria andPasswordNotBetween(String value1, String value2) {
            addCriterion("`password` not between", value1, value2, "password");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordIsNull() {
            addCriterion("moderator_password is null");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordIsNotNull() {
            addCriterion("moderator_password is not null");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordEqualTo(String value) {
            addCriterion("moderator_password =", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordNotEqualTo(String value) {
            addCriterion("moderator_password <>", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordGreaterThan(String value) {
            addCriterion("moderator_password >", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordGreaterThanOrEqualTo(String value) {
            addCriterion("moderator_password >=", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordLessThan(String value) {
            addCriterion("moderator_password <", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordLessThanOrEqualTo(String value) {
            addCriterion("moderator_password <=", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordLike(String value) {
            addCriterion("moderator_password like", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordNotLike(String value) {
            addCriterion("moderator_password not like", value, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordIn(List<String> values) {
            addCriterion("moderator_password in", values, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordNotIn(List<String> values) {
            addCriterion("moderator_password not in", values, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordBetween(String value1, String value2) {
            addCriterion("moderator_password between", value1, value2, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andModeratorPasswordNotBetween(String value1, String value2) {
            addCriterion("moderator_password not between", value1, value2, "moderatorPassword");
            return (Criteria) this;
        }

        public Criteria andProjectIsNull() {
            addCriterion("project is null");
            return (Criteria) this;
        }

        public Criteria andProjectIsNotNull() {
            addCriterion("project is not null");
            return (Criteria) this;
        }

        public Criteria andProjectEqualTo(String value) {
            addCriterion("project =", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectNotEqualTo(String value) {
            addCriterion("project <>", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectGreaterThan(String value) {
            addCriterion("project >", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectGreaterThanOrEqualTo(String value) {
            addCriterion("project >=", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectLessThan(String value) {
            addCriterion("project <", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectLessThanOrEqualTo(String value) {
            addCriterion("project <=", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectLike(String value) {
            addCriterion("project like", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectNotLike(String value) {
            addCriterion("project not like", value, "project");
            return (Criteria) this;
        }

        public Criteria andProjectIn(List<String> values) {
            addCriterion("project in", values, "project");
            return (Criteria) this;
        }

        public Criteria andProjectNotIn(List<String> values) {
            addCriterion("project not in", values, "project");
            return (Criteria) this;
        }

        public Criteria andProjectBetween(String value1, String value2) {
            addCriterion("project between", value1, value2, "project");
            return (Criteria) this;
        }

        public Criteria andProjectNotBetween(String value1, String value2) {
            addCriterion("project not between", value1, value2, "project");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeEqualTo(LocalDateTime value) {
            addCriterion("create_time =", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotEqualTo(LocalDateTime value) {
            addCriterion("create_time <>", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThan(LocalDateTime value) {
            addCriterion("create_time >", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("create_time >=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThan(LocalDateTime value) {
            addCriterion("create_time <", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("create_time <=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIn(List<LocalDateTime> values) {
            addCriterion("create_time in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotIn(List<LocalDateTime> values) {
            addCriterion("create_time not in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIsNull() {
            addCriterion("update_time is null");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIsNotNull() {
            addCriterion("update_time is not null");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeEqualTo(LocalDateTime value) {
            addCriterion("update_time =", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotEqualTo(LocalDateTime value) {
            addCriterion("update_time <>", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThan(LocalDateTime value) {
            addCriterion("update_time >", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("update_time >=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThan(LocalDateTime value) {
            addCriterion("update_time <", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("update_time <=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIn(List<LocalDateTime> values) {
            addCriterion("update_time in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotIn(List<LocalDateTime> values) {
            addCriterion("update_time not in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("update_time between", value1, value2, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("update_time not between", value1, value2, "updateTime");
            return (Criteria) this;
        }

        public Criteria andActivationDateIsNull() {
            addCriterion("activation_date is null");
            return (Criteria) this;
        }

        public Criteria andActivationDateIsNotNull() {
            addCriterion("activation_date is not null");
            return (Criteria) this;
        }

        public Criteria andActivationDateEqualTo(LocalDateTime value) {
            addCriterion("activation_date =", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateNotEqualTo(LocalDateTime value) {
            addCriterion("activation_date <>", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateGreaterThan(LocalDateTime value) {
            addCriterion("activation_date >", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("activation_date >=", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateLessThan(LocalDateTime value) {
            addCriterion("activation_date <", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("activation_date <=", value, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateIn(List<LocalDateTime> values) {
            addCriterion("activation_date in", values, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateNotIn(List<LocalDateTime> values) {
            addCriterion("activation_date not in", values, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("activation_date between", value1, value2, "activationDate");
            return (Criteria) this;
        }

        public Criteria andActivationDateNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("activation_date not between", value1, value2, "activationDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateIsNull() {
            addCriterion("expire_date is null");
            return (Criteria) this;
        }

        public Criteria andExpireDateIsNotNull() {
            addCriterion("expire_date is not null");
            return (Criteria) this;
        }

        public Criteria andExpireDateEqualTo(LocalDateTime value) {
            addCriterion("expire_date =", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateNotEqualTo(LocalDateTime value) {
            addCriterion("expire_date <>", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateGreaterThan(LocalDateTime value) {
            addCriterion("expire_date >", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("expire_date >=", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateLessThan(LocalDateTime value) {
            addCriterion("expire_date <", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("expire_date <=", value, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateIn(List<LocalDateTime> values) {
            addCriterion("expire_date in", values, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateNotIn(List<LocalDateTime> values) {
            addCriterion("expire_date not in", values, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("expire_date between", value1, value2, "expireDate");
            return (Criteria) this;
        }

        public Criteria andExpireDateNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("expire_date not between", value1, value2, "expireDate");
            return (Criteria) this;
        }

        public Criteria andAllowPartIsNull() {
            addCriterion("allow_part is null");
            return (Criteria) this;
        }

        public Criteria andAllowPartIsNotNull() {
            addCriterion("allow_part is not null");
            return (Criteria) this;
        }

        public Criteria andAllowPartEqualTo(Integer value) {
            addCriterion("allow_part =", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartNotEqualTo(Integer value) {
            addCriterion("allow_part <>", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartGreaterThan(Integer value) {
            addCriterion("allow_part >", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartGreaterThanOrEqualTo(Integer value) {
            addCriterion("allow_part >=", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartLessThan(Integer value) {
            addCriterion("allow_part <", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartLessThanOrEqualTo(Integer value) {
            addCriterion("allow_part <=", value, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartIn(List<Integer> values) {
            addCriterion("allow_part in", values, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartNotIn(List<Integer> values) {
            addCriterion("allow_part not in", values, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartBetween(Integer value1, Integer value2) {
            addCriterion("allow_part between", value1, value2, "allowPart");
            return (Criteria) this;
        }

        public Criteria andAllowPartNotBetween(Integer value1, Integer value2) {
            addCriterion("allow_part not between", value1, value2, "allowPart");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("`status` is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("`status` is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(Integer value) {
            addCriterion("`status` =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(Integer value) {
            addCriterion("`status` <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(Integer value) {
            addCriterion("`status` >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("`status` >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(Integer value) {
            addCriterion("`status` <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(Integer value) {
            addCriterion("`status` <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<Integer> values) {
            addCriterion("`status` in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<Integer> values) {
            addCriterion("`status` not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(Integer value1, Integer value2) {
            addCriterion("`status` between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("`status` not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andAllowRecordIsNull() {
            addCriterion("allow_record is null");
            return (Criteria) this;
        }

        public Criteria andAllowRecordIsNotNull() {
            addCriterion("allow_record is not null");
            return (Criteria) this;
        }

        public Criteria andAllowRecordEqualTo(Boolean value) {
            addCriterion("allow_record =", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordNotEqualTo(Boolean value) {
            addCriterion("allow_record <>", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordGreaterThan(Boolean value) {
            addCriterion("allow_record >", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordGreaterThanOrEqualTo(Boolean value) {
            addCriterion("allow_record >=", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordLessThan(Boolean value) {
            addCriterion("allow_record <", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordLessThanOrEqualTo(Boolean value) {
            addCriterion("allow_record <=", value, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordIn(List<Boolean> values) {
            addCriterion("allow_record in", values, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordNotIn(List<Boolean> values) {
            addCriterion("allow_record not in", values, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordBetween(Boolean value1, Boolean value2) {
            addCriterion("allow_record between", value1, value2, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andAllowRecordNotBetween(Boolean value1, Boolean value2) {
            addCriterion("allow_record not between", value1, value2, "allowRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordIsNull() {
            addCriterion("support_record is null");
            return (Criteria) this;
        }

        public Criteria andSupportRecordIsNotNull() {
            addCriterion("support_record is not null");
            return (Criteria) this;
        }

        public Criteria andSupportRecordEqualTo(Boolean value) {
            addCriterion("support_record =", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordNotEqualTo(Boolean value) {
            addCriterion("support_record <>", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordGreaterThan(Boolean value) {
            addCriterion("support_record >", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordGreaterThanOrEqualTo(Boolean value) {
            addCriterion("support_record >=", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordLessThan(Boolean value) {
            addCriterion("support_record <", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordLessThanOrEqualTo(Boolean value) {
            addCriterion("support_record <=", value, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordIn(List<Boolean> values) {
            addCriterion("support_record in", values, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordNotIn(List<Boolean> values) {
            addCriterion("support_record not in", values, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordBetween(Boolean value1, Boolean value2) {
            addCriterion("support_record between", value1, value2, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andSupportRecordNotBetween(Boolean value1, Boolean value2) {
            addCriterion("support_record not between", value1, value2, "supportRecord");
            return (Criteria) this;
        }

        public Criteria andDeletedIsNull() {
            addCriterion("deleted is null");
            return (Criteria) this;
        }

        public Criteria andDeletedIsNotNull() {
            addCriterion("deleted is not null");
            return (Criteria) this;
        }

        public Criteria andDeletedEqualTo(Boolean value) {
            addCriterion("deleted =", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedNotEqualTo(Boolean value) {
            addCriterion("deleted <>", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedGreaterThan(Boolean value) {
            addCriterion("deleted >", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedGreaterThanOrEqualTo(Boolean value) {
            addCriterion("deleted >=", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedLessThan(Boolean value) {
            addCriterion("deleted <", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedLessThanOrEqualTo(Boolean value) {
            addCriterion("deleted <=", value, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedIn(List<Boolean> values) {
            addCriterion("deleted in", values, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedNotIn(List<Boolean> values) {
            addCriterion("deleted not in", values, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedBetween(Boolean value1, Boolean value2) {
            addCriterion("deleted between", value1, value2, "deleted");
            return (Criteria) this;
        }

        public Criteria andDeletedNotBetween(Boolean value1, Boolean value2) {
            addCriterion("deleted not between", value1, value2, "deleted");
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