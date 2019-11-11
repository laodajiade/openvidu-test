-- MySQL dump 10.13  Distrib 5.7.27, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: sd_main
-- ------------------------------------------------------
-- Server version	5.7.27-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `sd_conference`
--

DROP TABLE IF EXISTS `sd_conference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_conference` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `room_id` varchar(128) NOT NULL COMMENT '会议室名称',
  `conference_subject` varchar(256) DEFAULT NULL COMMENT '主题',
  `conference_desc` varchar(1024) DEFAULT NULL COMMENT '会议描述',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `room_capacity` int(6) unsigned NOT NULL DEFAULT '50' COMMENT '容量',
  `status` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '会议状态（0：未开始，1：进行中，2：已结束）',
  `password` varchar(256) DEFAULT NULL COMMENT '入会密码',
  `invite_limit` tinyint(2) unsigned DEFAULT '1' COMMENT '会议邀请（0：不允许，1：允许）',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `index_room_id` (`room_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='会议表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sd_corporation`
--

DROP TABLE IF EXISTS `sd_corporation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_corporation` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `corp_name` varchar(256) NOT NULL COMMENT '企业名称',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `industry_id` bigint(11) unsigned NOT NULL COMMENT '行业ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_project` (`project`) USING BTREE,
  KEY `index_industry` (`industry_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='企业表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sd_corporation`
--

LOCK TABLES `sd_corporation` WRITE;
/*!40000 ALTER TABLE `sd_corporation` DISABLE KEYS */;
INSERT INTO `sd_corporation` VALUES (1,'杭州速递科技有限公司','Base',1,'2019-10-18 16:45:06','2019-10-22 10:47:00');
/*!40000 ALTER TABLE `sd_corporation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sd_department`
--

DROP TABLE IF EXISTS `sd_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_department` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `dept_name` varchar(128) NOT NULL COMMENT '部门名称',
  `parent_id` bigint(11) unsigned DEFAULT NULL COMMENT '上级部门ID',
  `corp_id` bigint(11) unsigned NOT NULL COMMENT '企业ID',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `index_corp_id` (`corp_id`) USING BTREE,
  KEY `index_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sd_department`
--

LOCK TABLES `sd_department` WRITE;
/*!40000 ALTER TABLE `sd_department` DISABLE KEYS */;
INSERT INTO `sd_department` VALUES (1,'杭州速递',null,1,'Base','2019-10-19 15:16:27','2019-10-23 11:36:35');
/*!40000 ALTER TABLE `sd_department` ENABLE KEYS */;
UNLOCK TABLES;

--
--
-- Table structure for table `sd_device`
--

DROP TABLE IF EXISTS `sd_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_device` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `serial_number` varchar(128) NOT NULL COMMENT '序列号',
  `device_id` varchar(128) DEFAULT NULL COMMENT '设备编码',
  `device_name` varchar(256) DEFAULT NULL COMMENT '设备名称',
  `device_mac` varchar(128) DEFAULT NULL COMMENT '设备mac地址',
  `device_type` varchar(128) DEFAULT NULL COMMENT '设备类型',
  `device_model` varchar(128) DEFAULT NULL COMMENT '设备型号',
  `ability` varchar(512) DEFAULT NULL COMMENT '设备能力集',
  `version` varchar(128) DEFAULT NULL COMMENT '设备程序版本',
  `manufacturer` varchar(128) DEFAULT NULL COMMENT '设备厂商',
  `access_type` tinyint(5) unsigned DEFAULT '0' COMMENT '设备接入协议类型，0：私有协议',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_serial_number` (`serial_number`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sd_device_dept`
--

DROP TABLE IF EXISTS `sd_device_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_device_dept` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `serial_number` varchar(255) NOT NULL COMMENT '设备序列号',
  `dept_id` bigint(11) unsigned NOT NULL COMMENT '部门ID',
  `corp_id` bigint(11) unsigned NOT NULL COMMENT '企业ID',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `index_serial_number` (`serial_number`) USING BTREE,
  UNIQUE KEY `unique_dept_device` (`dept_id`,`serial_number`) USING BTREE,
  KEY `index_corp_id` (`corp_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='设备部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sd_industry`
--

DROP TABLE IF EXISTS `sd_industry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_industry` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `industry_name` varchar(256) NOT NULL COMMENT '行业名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='行业表';
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `sd_industry` WRITE;
/*!40000 ALTER TABLE `sd_industry` DISABLE KEYS */;
INSERT INTO `sd_industry` VALUES (1, 'Customer', now(), now());
INSERT INTO `sd_industry` VALUES (2, 'Business', now(), now());
/*!40000 ALTER TABLE `sd_industry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sd_role`
--

DROP TABLE IF EXISTS `sd_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_role` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `role_name` varchar(128) NOT NULL COMMENT '角色名称',
  `role_desc` varchar(1024) NOT NULL COMMENT '角色描述',
  `privilege` varchar(512) NOT NULL COMMENT '角色权限,英文逗号分隔枚举值如下:createConference,conferenceManager,conferenceControl,organizationManager,userManager,deviceManager,roleManager,participantOnly  其中participantOnly为默认权限',
  `corp_id` bigint(11) unsigned NOT NULL COMMENT '企业ID',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `index_corp_id` (`corp_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sd_user`
--

DROP TABLE IF EXISTS `sd_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_user` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `uuid` varchar(128) NOT NULL COMMENT 'UUID',
  `username` varchar(64) DEFAULT NULL COMMENT '用户名',
  `phone` varchar(64) DEFAULT NULL COMMENT '手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `password` varchar(256) NOT NULL COMMENT '密码',
  `title` varchar(128) DEFAULT NULL COMMENT '职衔',
  `project` varchar(128) NOT NULL DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_uuid` (`uuid`) USING BTREE,
  UNIQUE KEY `unique_phone` (`phone`,`project`) USING BTREE,
  UNIQUE KEY `unique_email` (`email`,`project`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sd_user`
--

LOCK TABLES `sd_user` WRITE;
/*!40000 ALTER TABLE `sd_user` DISABLE KEYS */;
INSERT INTO `sd_user` VALUES (1,'administrator','超级管理员',NULL,NULL,'e10adc3949ba59abbe56e057f20f883e',NULL,'Base','2019-10-22 10:46:38','2019-10-22 10:46:38');
/*!40000 ALTER TABLE `sd_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sd_user_dept`
--

DROP TABLE IF EXISTS `sd_user_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_user_dept` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint(11) unsigned NOT NULL COMMENT '用户ID',
  `dept_id` bigint(11) unsigned NOT NULL COMMENT '部门ID',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unique_user_dept` (`user_id`,`dept_id`) USING BTREE,
  KEY `index_dept` (`dept_id`) USING BTREE,
  KEY `index_project` (`project`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sd_user_dept`
--

LOCK TABLES `sd_user_dept` WRITE;
/*!40000 ALTER TABLE `sd_user_dept` DISABLE KEYS */;
INSERT INTO `sd_user_dept` VALUES (1,1,1,'Base','2019-10-22 10:50:10','2019-10-22 10:50:10');
/*!40000 ALTER TABLE `sd_user_dept` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sd_user_role`
--

DROP TABLE IF EXISTS `sd_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sd_user_role` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id` bigint(11) unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint(11) unsigned NOT NULL COMMENT '角色ID',
  `project` varchar(128) DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unique_user_role` (`user_id`,`role_id`) USING BTREE,
  KEY `index_role` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-11-01 22:21:46
