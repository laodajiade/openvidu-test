-- MySQL dump 10.13  Distrib 5.7.27, for Linux (x86_64)
--
-- Host: localhost    Database: sd_main
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
) ENGINE=InnoDB AUTO_INCREMENT=936 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='会议表';
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
  `project` varchar(128) NOT NULL DEFAULT 'Base' COMMENT '项目属性',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_phone` (`phone`) USING BTREE,
  UNIQUE KEY `unique_email` (`email`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sd_department`
--

DROP TABLE IF EXISTS `sd_department`;
CREATE TABLE `sd_department` (
  `id`  bigint(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `parent_id`  bigint(11) UNSIGNED NULL COMMENT '上级部门ID' ,
  `dept_name`  varchar(128) CHARACTER SET utf8 NOT NULL COMMENT '部门名称' ,
  `corp_id`  bigint(11) UNSIGNED NOT NULL COMMENT '企业ID' ,
  `project` varchar(128) CHARACTER SET utf8 DEFAULT 'Base' COMMENT '项目属性',
  `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' ,
  `update_time`  datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' ,
  PRIMARY KEY (`id`),
  INDEX `index_corp_id` (`corp_id`) USING BTREE
)ENGINE=InnoDB DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci ROW_FORMAT=COMPACT COMMENT='部门表';

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-10-15 16:02:30
