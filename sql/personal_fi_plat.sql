-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: localhost    Database: personal_fi_plat
-- ------------------------------------------------------
-- Server version	8.0.22

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `acfun`
--

DROP TABLE IF EXISTS `acfun`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `acfun` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
  `income` decimal(10,2) DEFAULT '0.00' COMMENT '收入',
  `pay` decimal(10,2) DEFAULT '0.00' COMMENT '支出',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='账户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `acfun`
--

LOCK TABLES `acfun` WRITE;
/*!40000 ALTER TABLE `acfun` DISABLE KEYS */;
INSERT INTO `acfun` VALUES (2,1,-2130.46,1308.55,3439.01);
/*!40000 ALTER TABLE `acfun` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员名',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员昵称',
  `pwd` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员密码',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员头像',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员手机号',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员电子邮件地址',
  `perm` int DEFAULT '10' COMMENT '管理员权限',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账户角色',
  `api_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'admin','Super Admin','superadmin','http://localhost:8080/files/1745249158343-0d81afcfee604d3b93b374ac74c2ce2d34a5b63a.gif','13131313132','123456@qq.com',15,NULL,NULL);
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bill`
--

DROP TABLE IF EXISTS `bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `time` datetime DEFAULT NULL COMMENT '交易时间',
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交易分类',
  `payee` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收款方',
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交易说明',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账单类型',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '金额',
  `payment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收付款方式',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交易状态',
  `comment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `user_id` int NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`),
  KEY `ID` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bill`
--

LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES (1,'2025-05-10 15:13:30','工资','余额宝','工资','收入',1000.00,'现金支付','存入成功',NULL,1),(2,'2025-05-10 15:14:12','商户消费','麦当劳中国','麦辣鸡腿堡','支出',25.00,'微信支付','交易成功',NULL,1),(3,'2025-05-10 15:14:43','交通出行','公交集团','1路公交车','支出',1.30,'支付宝','交易成功',NULL,1),(4,'2025-05-10 15:15:45','退款','余额宝','大豆丝被芯','收入',256.00,'支付宝','已全额退款',NULL,1),(5,'2025-03-18 11:24:00','商户消费','饿了么','她锅花雕醉鸡邵阳学院店外卖订单','支出',25.00,'零钱通','支付成功','/',1),(6,'2025-03-17 19:42:00','商户消费','商户_桂启素','商户_桂启素','支出',20.00,'零钱','支付成功','/',1),(7,'2025-03-17 19:22:00','商户消费','教育部教育考试院','第251次CET考试费','支出',28.00,'零钱','支付成功','/',1),(8,'2025-03-17 11:20:00','商户消费','学友购物特价超市','学友特价超市','支出',8.00,'零钱','支付成功','/',1),(9,'2025-03-16 19:14:00','商户消费','美团','美团订单-25031611100300001306378689061706','支出',49.96,'零钱通','支付成功','/',1),(10,'2025-03-16 18:53:16','餐饮美食','e50***@alibaba-inc.com','花雕醉鸡(邵阳学院店)外卖订单','支出',23.30,'工商银行储蓄卡(9525)','交易成功','',1),(11,'2025-03-16 11:23:00','商户消费','美宜佳','美宜佳湘04227店','支出',8.00,'零钱','支付成功','/',1),(12,'2025-03-16 00:09:26','餐饮美食','281***@qq.com','【活动价】裕湘碱水挂面老长沙干挑碱面武汉热干面凉面拌面汤面炸酱面早餐面','支出',19.80,'工商银行储蓄卡(9525)','等待确认收货','',1),(13,'2025-03-14 20:01:00','商户消费','美团','美团订单-25031411100300001305988351342706','支出',53.23,'零钱通','支付成功','/',1),(14,'2025-03-14 11:44:00','商户消费','饿了么','好再来餐馆外卖订单','支出',15.88,'零钱通','支付成功','/',1),(15,'2025-03-14 11:39:00','商户消费','饿了么','超级吃货卡','支出',0.90,'零钱通','支付成功','/',1),(16,'2025-03-13 20:07:00','商户消费','美团','好再来餐馆-美团App-25031311100300001305815490334706','支出',17.88,'零钱通','支付成功','/',1),(17,'2025-03-13 11:30:00','商户消费','饿了么','她锅花雕醉鸡邵阳学院店外卖订单','支出',25.00,'零钱通','支付成功','/',1),(18,'2025-03-12 19:41:00','商户消费','七里坪药房','君信堂大药房','支出',77.80,'零钱通','支付成功','/',1),(19,'2025-03-12 19:34:00','商户消费','智校乐','水控充值','支出',6.00,'零钱通','支付成功','/',1),(20,'2025-03-12 18:07:55','餐饮美食','e50***@alibaba-inc.com','高颜值炒饭外卖订单','支出',18.00,'工商银行储蓄卡(9525)','交易成功','',1),(21,'2025-03-12 13:52:00','商户消费','中国电信','为19118372231充值38.0元，谨防诈骗','支出',37.92,'零钱通','支付成功','/',1),(22,'2025-03-12 11:02:48','餐饮美食','e50***@alibaba-inc.com','阿吉生煎(邵阳学院店)外卖订单','支出',21.90,'余额宝','交易成功','',1),(23,'2025-03-11 18:53:00','商户消费','塔斯汀','消费：邵阳市大祥邵阳学院店','支出',32.00,'零钱通','支付成功','/',1),(24,'2025-03-11 12:16:00','商户消费','饿了么','花雕醉鸡邵阳学院店外卖订单','支出',23.80,'零钱通','支付成功','/',1),(25,'2025-03-11 01:54:00','商户消费','淘宝平台商户','/','支出',28.30,'零钱通','支付成功','/',1),(26,'2025-03-10 20:25:00','扫二维码付款','鸡蛋灌饼，烤冷面','收款方备注:二维码收款','支出',6.00,'零钱通','已转账','/',1),(27,'2025-03-10 20:15:00','商户消费','阿梅18230646662','/','支出',5.00,'零钱通','支付成功','/',1),(28,'2025-03-10 19:12:00','其他','团app短视频-现金激励-微信零钱','/','收入',0.10,'/','已到账','团app短视频-现金激励-微信零钱',1),(29,'2025-03-10 19:12:00','其他','团app短视频-现金激励-微信零钱','/','收入',0.10,'/','已到账','团app短视频-现金激励-微信零钱',1),(30,'2025-03-10 19:11:00','其他','团app短视频-现金激励-微信零钱','/','收入',0.10,'/','已到账','团app短视频-现金激励-微信零钱',1),(31,'2025-03-10 19:11:00','其他','团app短视频-现金激励-微信零钱','/','收入',0.10,'/','已到账','团app短视频-现金激励-微信零钱',1),(32,'2025-03-10 19:11:00','其他','团app短视频-现金激励-微信零钱','/','收入',0.11,'/','已到账','团app短视频-现金激励-微信零钱',1),(33,'2025-03-10 12:44:00','群收款','.','/','支出',7.00,'零钱','支付成功','/',1),(34,'2025-03-10 12:09:00','商户消费','饿了么','高颜值炒饭外卖订单','支出',19.00,'零钱','支付成功','/',1),(35,'2025-03-09 18:32:00','商户消费','商户_桂启素','商户_桂启素','支出',20.00,'零钱','支付成功','/',1),(36,'2025-03-09 18:05:00','商户消费','美团','花雕醉鸡(邵阳学院七里坪店)-美团App-25030911100300001305123683871706','支出',23.80,'零钱','支付成功','/',1),(37,'2025-03-07 18:54:37','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(38,'2025-03-07 18:54:35','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(39,'2025-03-07 18:54:33','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(40,'2025-03-07 18:54:31','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(41,'2025-03-07 18:54:29','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(42,'2025-03-07 18:54:27','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(43,'2025-03-07 18:54:25','收入','/','网易BUFF出售-印花 | TYLOO | 2020 RMR','收入',1.95,'','交易成功','',1),(44,'2025-03-07 18:04:26','收入','/','网易BUFF出售-印花 | TYLOO（全息）| 2020 RMR','收入',9.75,'','交易成功','',1),(45,'2025-03-07 18:04:24','收入','/','网易BUFF出售-印花 | TYLOO（全息）| 2020 RMR','收入',9.75,'','交易成功','',1),(46,'2025-03-07 14:06:00','商户消费','湖南省财政事务中心','邵阳学院收取重修费等费用-0020934209','支出',525.00,'零钱通','支付成功','/',1),(47,'2025-03-06 20:30:00','商户消费','美团','美团订单-25030611100300001304522296282706','支出',54.45,'零钱通','支付成功','/',1),(48,'2025-03-06 18:30:00','商户消费','乐山公寓便利店','授权码销售','支出',1.50,'零钱','支付成功','/',1),(49,'2025-03-06 17:40:00','商户消费','微信转账到QQ','向QQ账号[1********4]转账','支出',5.69,'零钱通','支付成功','/',1),(50,'2025-03-06 11:43:00','商户消费','美团','高颜值炒饭-美团App-25030611100300001304427040150706','支出',20.00,'零钱通','支付成功','/',1),(51,'2025-03-05 10:59:00','商户消费','智校乐','水控充值','支出',3.00,'零钱','支付成功','/',1),(52,'2025-03-04 19:08:00','商户消费','美团','美团订单-25030411100300001304182978507706','支出',23.35,'零钱通','支付成功','/',1),(53,'2025-03-04 18:56:00','商户消费','美团','高颜值炒饭-美团App-25030411100300001304179758254706','支出',21.00,'零钱通','支付成功','/',1),(54,'2025-03-04 11:49:00','商户消费','美团','好再来餐馆-美团App-25030411100300001304109830094706','支出',16.88,'零钱通','支付成功','/',1),(55,'2025-03-02 21:45:00','扫二维码付款','阿民','收款方备注:二维码收款','支出',25.00,'零钱','已转账','/',1),(56,'2025-03-02 20:20:00','商户消费','美团','美团订单-25030211100300001303872445622706','支出',39.74,'零钱通','支付成功','/',1),(57,'2025-03-02 12:15:00','群收款','尽歡','/','支出',4.29,'零钱','支付成功','/',1),(58,'2025-03-02 11:52:00','商户消费','美团','高颜值炒饭-美团App-25030211100300001303767093207706','支出',16.00,'零钱','支付成功','/',1),(59,'2025-03-02 10:12:00','群收款','.','/','支出',2.86,'零钱','支付成功','/',1),(60,'2025-03-01 11:58:00','商户消费','美团','她锅·花雕醉鸡（七里坪邵阳学院店）-美团App-25030111100300001303568618044706','支出',24.00,'零钱','支付成功','/',1),(61,'2025-02-27 10:53:00','商户消费','智校乐','水控充值','支出',3.00,'零钱通','支付成功','/',1),(62,'2025-02-26 19:40:00','商户消费','美团','美团订单-25022611100300001303168227159706','支出',41.24,'零钱通','支付成功','/',1),(63,'2025-02-26 19:21:00','商户消费','美团','好再来餐馆-美团App-25022611100300001303161903698706','支出',15.08,'零钱通','支付成功','/',1),(64,'2025-02-26 11:56:00','群收款','.','/','支出',2.15,'零钱','支付成功','/',1),(65,'2025-02-26 11:32:00','商户消费','饿了么','花雕醉鸡邵阳学院店外卖订单','支出',23.80,'零钱通','支付成功','/',1),(66,'2025-02-25 19:39:00','商户消费','友宝昂莱','康师傅喝开水550ml','支出',2.00,'零钱','支付成功','/',1),(67,'2025-02-25 19:28:00','扫二维码付款','鸡蛋灌饼，烤冷面','收款方备注:二维码收款','支出',6.00,'零钱通','已转账','/',1),(68,'2025-02-25 19:25:00','商户消费','阿梅18230646662','/','支出',5.00,'零钱通','支付成功','/',1),(69,'2025-02-25 19:22:00','商户消费','商户_桂启素','商户_桂启素','支出',20.00,'零钱通','支付成功','/',1),(70,'2025-02-25 11:11:00','商户消费','塔斯汀','消费：邵阳市大祥邵阳学院店','支出',22.00,'零钱通','支付成功','/',1),(71,'2025-02-24 18:27:00','扫二维码付款','邵阳市大祥区九十九号浏阳菜馆','收款方备注:二维码收款','支出',13.00,'零钱通','已转账','/',1),(72,'2025-02-24 11:40:00','商户消费','饿了么','好再来餐馆外卖订单','支出',22.00,'零钱通','支付成功','/',1),(73,'2025-02-23 12:16:00','商户消费','美团','花雕醉鸡(邵阳学院七里坪店)-美团App-25022311100300001302586803229706','支出',23.80,'零钱通','支付成功','/',1),(74,'2025-02-21 21:29:00','商户消费','智校乐','水控充值','支出',3.00,'零钱','支付成功','/',1),(75,'2025-02-21 19:22:00','商户消费','美团','美团订单-25022111100300001302274113114706','支出',24.19,'零钱通','支付成功','/',1),(76,'2025-02-21 19:10:00','商户消费','饿了么','花雕醉鸡邵阳学院店外卖订单','支出',23.80,'零钱','支付成功','/',1),(77,'2025-02-21 12:31:00','商户消费','饿了么','好再来餐馆外卖订单','支出',19.00,'零钱','支付成功','/',1),(78,'2025-02-20 12:12:00','商户消费','饿了么','高颜值炒饭外卖订单','支出',16.50,'零钱通','支付成功','/',1),(79,'2025-02-20 08:03:35','文化休闲','led***@service.netease.com','网易云音乐-会员自动续费','支出',11.00,'工商银行储蓄卡(9525)','交易成功','',1),(80,'2025-02-19 19:19:08','餐饮美食','e50***@alibaba-inc.com','正新鸡排·炸鸡烧烤(邵阳学院店)外卖订单','支出',19.20,'农业银行储蓄卡(1078)','交易成功','',1),(81,'2025-02-19 11:52:26','餐饮美食','e50***@alibaba-inc.com','高颜值炒饭外卖订单','支出',17.00,'农业银行储蓄卡(1078)','交易成功','',1),(82,'2025-02-18 15:49:59','交通出行','220***@qq.com','公交-17路线-车牌【6-165】 2025-02-18 15:49:06','支出',1.60,'农业银行储蓄卡(1078)','交易成功','',1),(83,'2025-02-18 15:43:00','扫二维码付款','小米之家宝庆东路店','收款方备注:二维码收款','支出',303.90,'零钱通','已转账','/',1),(84,'2025-02-18 14:08:24','交通出行','220***@qq.com','公交-17路线-车牌【6-164】 2025-02-18 14:08:01','支出',1.59,'农业银行储蓄卡(1078)&余额宝&红包','交易成功','',1),(85,'2025-02-18 02:04:00','商户消费','岳阳楼速食旗舰店','抖音电商-订单编号6939602524794001114','支出',14.90,'零钱通','支付成功','/',1),(86,'2025-02-17 20:28:00','商户消费','美团','美团订单-25021711100300001301627102584706','支出',5.99,'零钱通','支付成功','/',1),(87,'2025-02-17 20:26:00','商户消费','美团','美团订单-25021711100300001301628132760706','支出',29.17,'零钱通','支付成功','/',1),(88,'2025-02-17 20:01:00','扫二维码付款','正宗纯香油红嘴巴麻辣烫','收款方备注:二维码收款','支出',7.00,'零钱通','已转账','/',1),(89,'2025-02-17 19:55:00','商户消费','阿梅18230646662','/','支出',5.00,'零钱通','支付成功','/',1),(90,'2025-02-17 11:58:00','商户消费','饿了么','她锅花雕醉鸡邵阳学院店外卖订单','支出',21.00,'零钱通','支付成功','/',1),(91,'2025-02-16 20:36:00','商户消费','乐山公寓便利店','授权码销售','支出',8.00,'零钱通','支付成功','/',1),(92,'2025-02-16 17:28:00','商户消费','商户_桂启素','商户_桂启素','支出',20.00,'零钱通','支付成功','/',1),(93,'2025-02-16 17:00:23','交通出行','220***@qq.com','公交-17路线-车牌【6-173】 2025-02-16 16:59:37','支出',1.60,'余额宝','交易成功','',1),(94,'2025-02-16 17:00:00','商户消费','中国电信','为17373129812充值20.0元，谨防诈骗','支出',19.96,'零钱通','支付成功','/',1),(95,'2025-02-16 15:39:30','交通出行','220***@qq.com','公交-17路线-车牌【6-173】 2025-02-16 15:38:23','支出',1.60,'余额宝','交易成功','',1),(96,'2025-02-16 11:21:00','商户消费','饿了么','好再来餐馆外卖订单','支出',13.88,'零钱通','支付成功','/',1),(97,'2025-02-15 22:07:08','交通出行','aut***@autonavi.com','高德打车订单','支出',14.16,'农业银行储蓄卡(1078)','交易成功','',1),(98,'2025-02-11 14:53:00','商户消费','中铁网络','12306消费','支出',67.50,'零钱通','支付成功','/',1),(99,'2025-02-10 11:58:00','商户消费','WARGAMING','wot','支出',6.02,'零钱通','支付成功','/',1),(100,'2025-02-06 21:29:00','商户消费','淘宝平台商户','/','支出',390.00,'零钱通','支付成功','/',1),(101,'2025-02-06 17:04:00','商户消费','微信转账到QQ','向QQ账号[1********4]转账','支出',5.00,'零钱通','支付成功','/',1),(102,'2025-02-04 00:21:00','扫二维码付款','吴金香','收款方备注:二维码收款','支出',10.00,'零钱通','已转账','/',1),(103,'2025-02-02 16:00:00','商户消费','微信转账到QQ','向QQ账号[1********4]转账','支出',20.78,'零钱通','支付成功','/',1),(104,'2025-02-02 10:18:00','美团平台商户-退款','美团平台商户','美团平台商户','收入',2.80,'零钱','已全额退款','/',1),(105,'2025-02-02 10:18:00','商户消费','美团平台商户','美团订单-25020211100300001308922534863706','支出',2.80,'零钱','已全额退款','/',1),(106,'2025-02-02 10:17:00','商户消费','美团','先骑后付','支出',2.80,'零钱','支付成功','/',1),(107,'2025-02-01 14:32:00','商户消费','中国电信','为17373129812充值10.0元，谨防诈骗','支出',9.98,'零钱','支付成功','/',1),(108,'2025-02-01 14:31:00','商户消费','中国电信','为19118372231充值38.0元，谨防诈骗','支出',37.92,'零钱','支付成功','/',1),(109,'2025-01-29 12:28:00','微信红包','风花雪月','/','收入',15.92,'/','已存入零钱','/',1),(110,'2025-01-27 13:10:00','商户消费','暖常','收银单号：2025012713100618220','支出',47.52,'零钱','支付成功','/',1),(111,'2025-01-24 14:11:29','文化休闲','181***@163.com','余额交易】steam土耳其 不是充值卡美金土区充钱卡代充土耳其美刀','支出',54.40,'农业银行储蓄卡(1078)','交易成功','',1),(112,'2025-01-21 23:11:00','扫二维码付款','张卓','收款方备注:二维码收款','支出',40.00,'零钱','已转账','/',1),(113,'2025-01-20 08:03:27','文化休闲','led***@service.netease.com','网易云音乐-会员自动续费','支出',11.00,'农业银行储蓄卡(1078)','交易成功','',1),(114,'2024-12-31 23:03:10','转账红包','xia***@126.com','收到口令红包','收入',0.17,'账户余额','交易成功','',1),(115,'2024-12-27 11:38:47','生活服务','ttw***@service.aliyun.com','淘宝退货-寄件费_3978838842_LP00703457642817','支出',2.43,'余额宝','交易成功','',1),(116,'2024-12-24 10:44:28','交通出行','yun***@163.com','长沙地铁消费','支出',10.00,'农业银行储蓄卡(1078)','交易成功','',1),(117,'2024-12-24 08:04:33','交通出行','aut***@autonavi.com','高德打车订单','支出',10.71,'余额宝','交易成功','',1),(118,'2024-12-23 10:39:20','交通出行','chi***@alipay.com','火车票','支出',67.50,'余额宝','交易成功','',1),(119,'2024-12-22 19:39:34','文化休闲','181***@163.com','余额交易】steam土耳其 不是充值卡美金土区充钱卡代充土耳其美刀','支出',252.00,'余额宝','交易成功','',1),(120,'2024-12-22 11:16:34','餐饮美食','e50***@alibaba-inc.com','她锅花雕醉鸡(邵阳学院店)外卖订单','支出',23.00,'余额宝','交易成功','',1),(121,'2024-12-20 19:34:15','餐饮美食','/','独一灶铜炉火锅鸡邵阳学院店','支出',98.00,'余额宝&碰一下餐饮立减优惠','交易成功','',1),(122,'2024-12-20 08:03:26','文化休闲','led***@service.netease.com','网易云音乐-会员自动续费','支出',11.00,'余额宝','交易成功','',1),(123,'2024-12-19 11:27:27','餐饮美食','e50***@alibaba-inc.com','高颜值炒饭外卖订单','支出',20.00,'余额宝','交易成功','',1),(124,'2024-12-18 10:14:02','文化休闲','sup***@adyen.com','178cb3dd3df444a88da5cafbb55f1571','支出',6.01,'余额宝','交易成功','',1);
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类名称',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='账单分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'交通出行','支出'),(2,'商户消费','支出'),(3,'文化休闲','支出'),(4,'生活服务','支出'),(5,'转账','支出'),(6,'餐饮美食','支出'),(7,'工资','收入'),(8,'红包','收入'),(9,'退款','收入'),(10,'其他','收入'),(11,'其他','支出');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notebook`
--

DROP TABLE IF EXISTS `notebook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='记账日记';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notebook`
--

LOCK TABLES `notebook` WRITE;
/*!40000 ALTER TABLE `notebook` DISABLE KEYS */;
INSERT INTO `notebook` VALUES (4,'2','http://localhost:8080/files/1745249125856-0d81afcfee604d3b93b374ac74c2ce2d34a5b63a.gif','<h1>啊实打实大师大萨</h1><h2>阿达阿达是的</h2><p>啊<u>实打</u>实<s>大师啊</s>实<strong>打</strong>实大<em>的</em><span style=\"font-family: 黑体;\"><em>大大</em></span><em>大</em></p>',1,'2025-04-21'),(5,'Long cover','http://localhost:8080/files/1745250256619-B0FFE48C930A6B30AD0B2DB0A496CAC1.jpg','<p>111111</p>',1,'2025-04-21'),(6,'User Notebook','http://localhost:8080/files/1745379949047-c08adf2bbc6e59df097227ae4beaa02e396336762.jpg','<p>这官当多大才算大</p>',1,'2025-04-23'),(7,'Second','http://localhost:8080/files/1745380413823-微信图片_20220122194627.jpg','<p>Second</p>',1,'2025-04-23');
/*!40000 ALTER TABLE `notebook` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',
  `time` datetime DEFAULT NULL COMMENT '发布时间',
  `user` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发布者',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='公告信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
INSERT INTO `notice` VALUES (1,'test','This is a test notice.','2025-03-29 00:00:00','Zirconium'),(2,'Test2','Test2','2025-04-01 18:41:54','Zirconium'),(3,'Test3','Admin\nSecond Line\nThird Line\nMannerheim Line','2025-04-01 18:42:14','admin'),(4,'Placeholder Title','Just a test for placeholder','2025-04-04 00:34:22','Zirconium');
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plan`
--

DROP TABLE IF EXISTS `plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划名称',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面',
  `money` decimal(10,2) DEFAULT NULL COMMENT '总金额',
  `start` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始日期',
  `end` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束日期',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='存钱计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan`
--

LOCK TABLES `plan` WRITE;
/*!40000 ALTER TABLE `plan` DISABLE KEYS */;
INSERT INTO `plan` VALUES (3,'Plan','http://localhost:8080/files/1745389950611-c08adf2bbc6e59df097227ae4beaa02e396336762.jpg',1000.00,'2025-04-24','2025-04-30',1,'2025-04-23'),(4,'测试','http://localhost:8080/files/1745907366009-0d81afcfee604d3b93b374ac74c2ce2d34a5b63a.gif',2000.00,'2025-04-30','2025-05-30',1,'2025-04-29');
/*!40000 ALTER TABLE `plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plan_detail`
--

DROP TABLE IF EXISTS `plan_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan_detail` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `money` decimal(10,2) DEFAULT '0.00' COMMENT '金额',
  `date` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日期',
  `plan_id` int DEFAULT NULL COMMENT '存钱计划ID',
  `sum` decimal(10,2) DEFAULT '0.00' COMMENT '总金额',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='计划明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan_detail`
--

LOCK TABLES `plan_detail` WRITE;
/*!40000 ALTER TABLE `plan_detail` DISABLE KEYS */;
INSERT INTO `plan_detail` VALUES (10,50.00,'2025-04-26',3,50.00),(11,124.00,'2025-04-26',3,174.00),(12,666.00,'2025-04-26',3,840.00),(13,100.00,'2025-04-29',4,100.00);
/*!40000 ALTER TABLE `plan_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `site_config`
--

DROP TABLE IF EXISTS `site_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `site_config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL COMMENT '配置项键名',
  `config_value` varchar(1024) DEFAULT NULL COMMENT '配置项值(敏感值AES加密)',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `updated_time` varchar(50) DEFAULT NULL COMMENT '更新时间',
  `updated_by` varchar(100) DEFAULT NULL COMMENT '最后更新管理员',
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `site_config`
--

LOCK TABLES `site_config` WRITE;
/*!40000 ALTER TABLE `site_config` DISABLE KEYS */;
INSERT INTO `site_config` VALUES (1,'finexly_api_key',NULL,'Finexly Real-time Exchange Rate API Key (https://api.finexly.com/v1/rate)','2026-10-28 20:46:30','system'),(2,'deepseek_public_api_key',NULL,'Public Fallback DeepSeek API Key','2026-10-28 20:46:30','system'),(3,'deepseek_base_url','https://api.deepseek.com','DeepSeek API Base URL','2026-10-28 20:46:30','system');
/*!40000 ALTER TABLE `site_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `sex` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户性别',
  `pwd` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户密码',
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户手机号',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户电子邮件地址',
  `birth` date DEFAULT NULL COMMENT '用户生日',
  `descriptiontext` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户描述',
  `perm` int DEFAULT '1' COMMENT '用户权限',
  `role` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户角色',
  `api_key` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'Zirconium','WT auf Zirconium','男','12345','http://localhost:8080/files/1745594076171-4fd8a06b61d271c4eb71c85df79268429de46d63.gif','13113131313','demo@starrealm.com','2001-01-01','First account.',1,NULL,NULL),(2,'Testuser','Test','女','123456','http://localhost:8080/files/1745305721968-0d81afcfee604d3b93b374ac74c2ce2d34a5b63a.gif','12112121212','123@qq.com','2025-03-20','test',1,NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-28 21:01:34
