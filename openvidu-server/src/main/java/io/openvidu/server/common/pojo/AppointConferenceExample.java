package io.openvidu.server.common.pojo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppointConferenceExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public AppointConferenceExample() {
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

        public Criteria andConferenceSubjectIsNull() {
            addCriterion("conference_subject is null");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectIsNotNull() {
            addCriterion("conference_subject is not null");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectEqualTo(String value) {
            addCriterion("conference_subject =", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectNotEqualTo(String value) {
            addCriterion("conference_subject <>", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectGreaterThan(String value) {
            addCriterion("conference_subject >", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectGreaterThanOrEqualTo(String value) {
            addCriterion("conference_subject >=", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectLessThan(String value) {
            addCriterion("conference_subject <", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectLessThanOrEqualTo(String value) {
            addCriterion("conference_subject <=", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectLike(String value) {
            addCriterion("conference_subject like", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectNotLike(String value) {
            addCriterion("conference_subject not like", value, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectIn(List<String> values) {
            addCriterion("conference_subject in", values, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectNotIn(List<String> values) {
            addCriterion("conference_subject not in", values, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectBetween(String value1, String value2) {
            addCriterion("conference_subject between", value1, value2, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceSubjectNotBetween(String value1, String value2) {
            addCriterion("conference_subject not between", value1, value2, "conferenceSubject");
            return (Criteria) this;
        }

        public Criteria andConferenceDescIsNull() {
            addCriterion("conference_desc is null");
            return (Criteria) this;
        }

        public Criteria andConferenceDescIsNotNull() {
            addCriterion("conference_desc is not null");
            return (Criteria) this;
        }

        public Criteria andConferenceDescEqualTo(String value) {
            addCriterion("conference_desc =", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescNotEqualTo(String value) {
            addCriterion("conference_desc <>", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescGreaterThan(String value) {
            addCriterion("conference_desc >", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescGreaterThanOrEqualTo(String value) {
            addCriterion("conference_desc >=", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescLessThan(String value) {
            addCriterion("conference_desc <", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescLessThanOrEqualTo(String value) {
            addCriterion("conference_desc <=", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescLike(String value) {
            addCriterion("conference_desc like", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescNotLike(String value) {
            addCriterion("conference_desc not like", value, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescIn(List<String> values) {
            addCriterion("conference_desc in", values, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescNotIn(List<String> values) {
            addCriterion("conference_desc not in", values, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescBetween(String value1, String value2) {
            addCriterion("conference_desc between", value1, value2, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andConferenceDescNotBetween(String value1, String value2) {
            addCriterion("conference_desc not between", value1, value2, "conferenceDesc");
            return (Criteria) this;
        }

        public Criteria andUserIdIsNull() {
            addCriterion("user_id is null");
            return (Criteria) this;
        }

        public Criteria andUserIdIsNotNull() {
            addCriterion("user_id is not null");
            return (Criteria) this;
        }

        public Criteria andUserIdEqualTo(Long value) {
            addCriterion("user_id =", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdNotEqualTo(Long value) {
            addCriterion("user_id <>", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdGreaterThan(Long value) {
            addCriterion("user_id >", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdGreaterThanOrEqualTo(Long value) {
            addCriterion("user_id >=", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdLessThan(Long value) {
            addCriterion("user_id <", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdLessThanOrEqualTo(Long value) {
            addCriterion("user_id <=", value, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdIn(List<Long> values) {
            addCriterion("user_id in", values, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdNotIn(List<Long> values) {
            addCriterion("user_id not in", values, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdBetween(Long value1, Long value2) {
            addCriterion("user_id between", value1, value2, "userId");
            return (Criteria) this;
        }

        public Criteria andUserIdNotBetween(Long value1, Long value2) {
            addCriterion("user_id not between", value1, value2, "userId");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidIsNull() {
            addCriterion("moderator_uuid is null");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidIsNotNull() {
            addCriterion("moderator_uuid is not null");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidEqualTo(String value) {
            addCriterion("moderator_uuid =", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidNotEqualTo(String value) {
            addCriterion("moderator_uuid <>", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidGreaterThan(String value) {
            addCriterion("moderator_uuid >", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidGreaterThanOrEqualTo(String value) {
            addCriterion("moderator_uuid >=", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidLessThan(String value) {
            addCriterion("moderator_uuid <", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidLessThanOrEqualTo(String value) {
            addCriterion("moderator_uuid <=", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidLike(String value) {
            addCriterion("moderator_uuid like", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidNotLike(String value) {
            addCriterion("moderator_uuid not like", value, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidIn(List<String> values) {
            addCriterion("moderator_uuid in", values, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidNotIn(List<String> values) {
            addCriterion("moderator_uuid not in", values, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidBetween(String value1, String value2) {
            addCriterion("moderator_uuid between", value1, value2, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andModeratorUuidNotBetween(String value1, String value2) {
            addCriterion("moderator_uuid not between", value1, value2, "moderatorUuid");
            return (Criteria) this;
        }

        public Criteria andStartTimeIsNull() {
            addCriterion("start_time is null");
            return (Criteria) this;
        }

        public Criteria andStartTimeIsNotNull() {
            addCriterion("start_time is not null");
            return (Criteria) this;
        }

        public Criteria andStartTimeEqualTo(Date value) {
            addCriterion("start_time =", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeNotEqualTo(Date value) {
            addCriterion("start_time <>", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeGreaterThan(Date value) {
            addCriterion("start_time >", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("start_time >=", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeLessThan(Date value) {
            addCriterion("start_time <", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeLessThanOrEqualTo(Date value) {
            addCriterion("start_time <=", value, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeIn(List<Date> values) {
            addCriterion("start_time in", values, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeNotIn(List<Date> values) {
            addCriterion("start_time not in", values, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeBetween(Date value1, Date value2) {
            addCriterion("start_time between", value1, value2, "startTime");
            return (Criteria) this;
        }

        public Criteria andStartTimeNotBetween(Date value1, Date value2) {
            addCriterion("start_time not between", value1, value2, "startTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeIsNull() {
            addCriterion("end_time is null");
            return (Criteria) this;
        }

        public Criteria andEndTimeIsNotNull() {
            addCriterion("end_time is not null");
            return (Criteria) this;
        }

        public Criteria andEndTimeEqualTo(Date value) {
            addCriterion("end_time =", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeNotEqualTo(Date value) {
            addCriterion("end_time <>", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeGreaterThan(Date value) {
            addCriterion("end_time >", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("end_time >=", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeLessThan(Date value) {
            addCriterion("end_time <", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeLessThanOrEqualTo(Date value) {
            addCriterion("end_time <=", value, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeIn(List<Date> values) {
            addCriterion("end_time in", values, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeNotIn(List<Date> values) {
            addCriterion("end_time not in", values, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeBetween(Date value1, Date value2) {
            addCriterion("end_time between", value1, value2, "endTime");
            return (Criteria) this;
        }

        public Criteria andEndTimeNotBetween(Date value1, Date value2) {
            addCriterion("end_time not between", value1, value2, "endTime");
            return (Criteria) this;
        }

        public Criteria andDurationIsNull() {
            addCriterion("duration is null");
            return (Criteria) this;
        }

        public Criteria andDurationIsNotNull() {
            addCriterion("duration is not null");
            return (Criteria) this;
        }

        public Criteria andDurationEqualTo(Integer value) {
            addCriterion("duration =", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationNotEqualTo(Integer value) {
            addCriterion("duration <>", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationGreaterThan(Integer value) {
            addCriterion("duration >", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationGreaterThanOrEqualTo(Integer value) {
            addCriterion("duration >=", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationLessThan(Integer value) {
            addCriterion("duration <", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationLessThanOrEqualTo(Integer value) {
            addCriterion("duration <=", value, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationIn(List<Integer> values) {
            addCriterion("duration in", values, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationNotIn(List<Integer> values) {
            addCriterion("duration not in", values, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationBetween(Integer value1, Integer value2) {
            addCriterion("duration between", value1, value2, "duration");
            return (Criteria) this;
        }

        public Criteria andDurationNotBetween(Integer value1, Integer value2) {
            addCriterion("duration not between", value1, value2, "duration");
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

        public Criteria andAutoInviteIsNull() {
            addCriterion("auto_invite is null");
            return (Criteria) this;
        }

        public Criteria andAutoInviteIsNotNull() {
            addCriterion("auto_invite is not null");
            return (Criteria) this;
        }

        public Criteria andAutoInviteEqualTo(Integer value) {
            addCriterion("auto_invite =", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteNotEqualTo(Integer value) {
            addCriterion("auto_invite <>", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteGreaterThan(Integer value) {
            addCriterion("auto_invite >", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteGreaterThanOrEqualTo(Integer value) {
            addCriterion("auto_invite >=", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteLessThan(Integer value) {
            addCriterion("auto_invite <", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteLessThanOrEqualTo(Integer value) {
            addCriterion("auto_invite <=", value, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteIn(List<Integer> values) {
            addCriterion("auto_invite in", values, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteNotIn(List<Integer> values) {
            addCriterion("auto_invite not in", values, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteBetween(Integer value1, Integer value2) {
            addCriterion("auto_invite between", value1, value2, "autoInvite");
            return (Criteria) this;
        }

        public Criteria andAutoInviteNotBetween(Integer value1, Integer value2) {
            addCriterion("auto_invite not between", value1, value2, "autoInvite");
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

        public Criteria andTypeIsNull() {
            addCriterion("`type` is null");
            return (Criteria) this;
        }

        public Criteria andTypeIsNotNull() {
            addCriterion("`type` is not null");
            return (Criteria) this;
        }

        public Criteria andTypeEqualTo(String value) {
            addCriterion("`type` =", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeNotEqualTo(String value) {
            addCriterion("`type` <>", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeGreaterThan(String value) {
            addCriterion("`type` >", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeGreaterThanOrEqualTo(String value) {
            addCriterion("`type` >=", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeLessThan(String value) {
            addCriterion("`type` <", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeLessThanOrEqualTo(String value) {
            addCriterion("`type` <=", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeLike(String value) {
            addCriterion("`type` like", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeNotLike(String value) {
            addCriterion("`type` not like", value, "type");
            return (Criteria) this;
        }

        public Criteria andTypeIn(List<String> values) {
            addCriterion("`type` in", values, "type");
            return (Criteria) this;
        }

        public Criteria andTypeNotIn(List<String> values) {
            addCriterion("`type` not in", values, "type");
            return (Criteria) this;
        }

        public Criteria andTypeBetween(String value1, String value2) {
            addCriterion("`type` between", value1, value2, "type");
            return (Criteria) this;
        }

        public Criteria andTypeNotBetween(String value1, String value2) {
            addCriterion("`type` not between", value1, value2, "type");
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

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
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

        public Criteria andUpdateTimeEqualTo(Date value) {
            addCriterion("update_time =", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotEqualTo(Date value) {
            addCriterion("update_time <>", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThan(Date value) {
            addCriterion("update_time >", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("update_time >=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThan(Date value) {
            addCriterion("update_time <", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThanOrEqualTo(Date value) {
            addCriterion("update_time <=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIn(List<Date> values) {
            addCriterion("update_time in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotIn(List<Date> values) {
            addCriterion("update_time not in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeBetween(Date value1, Date value2) {
            addCriterion("update_time between", value1, value2, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotBetween(Date value1, Date value2) {
            addCriterion("update_time not between", value1, value2, "updateTime");
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