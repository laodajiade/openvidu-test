package io.openvidu.server.common.pojo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HiddenUserGroupExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public HiddenUserGroupExample() {
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

        public Criteria andHiddenTypeIsNull() {
            addCriterion("hidden_type is null");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeIsNotNull() {
            addCriterion("hidden_type is not null");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeEqualTo(Integer value) {
            addCriterion("hidden_type =", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeNotEqualTo(Integer value) {
            addCriterion("hidden_type <>", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeGreaterThan(Integer value) {
            addCriterion("hidden_type >", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeGreaterThanOrEqualTo(Integer value) {
            addCriterion("hidden_type >=", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeLessThan(Integer value) {
            addCriterion("hidden_type <", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeLessThanOrEqualTo(Integer value) {
            addCriterion("hidden_type <=", value, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeIn(List<Integer> values) {
            addCriterion("hidden_type in", values, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeNotIn(List<Integer> values) {
            addCriterion("hidden_type not in", values, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeBetween(Integer value1, Integer value2) {
            addCriterion("hidden_type between", value1, value2, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andHiddenTypeNotBetween(Integer value1, Integer value2) {
            addCriterion("hidden_type not between", value1, value2, "hiddenType");
            return (Criteria) this;
        }

        public Criteria andGtmCreateIsNull() {
            addCriterion("gtm_create is null");
            return (Criteria) this;
        }

        public Criteria andGtmCreateIsNotNull() {
            addCriterion("gtm_create is not null");
            return (Criteria) this;
        }

        public Criteria andGtmCreateEqualTo(LocalDateTime value) {
            addCriterion("gtm_create =", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateNotEqualTo(LocalDateTime value) {
            addCriterion("gtm_create <>", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateGreaterThan(LocalDateTime value) {
            addCriterion("gtm_create >", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("gtm_create >=", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateLessThan(LocalDateTime value) {
            addCriterion("gtm_create <", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("gtm_create <=", value, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateIn(List<LocalDateTime> values) {
            addCriterion("gtm_create in", values, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateNotIn(List<LocalDateTime> values) {
            addCriterion("gtm_create not in", values, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("gtm_create between", value1, value2, "gtmCreate");
            return (Criteria) this;
        }

        public Criteria andGtmCreateNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("gtm_create not between", value1, value2, "gtmCreate");
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