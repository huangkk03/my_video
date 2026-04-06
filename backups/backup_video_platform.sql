-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: video_platform
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `slug` varchar(100) NOT NULL COMMENT 'åˆ†ç±»åˆ«å',
  `description` varchar(500) DEFAULT NULL COMMENT 'åˆ†ç±»æè¿°',
  `parent_id` bigint DEFAULT NULL COMMENT 'çˆ¶åˆ†ç±»ID',
  `sort_order` int DEFAULT '0' COMMENT 'æŽ’åº',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`),
  KEY `idx_slug` (`slug`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category` DISABLE KEYS */;
INSERT INTO `category` VALUES (1,'电影','movies','电影分类',NULL,1,'2026-04-05 14:51:54','2026-04-05 14:51:54'),(2,'电视剧','tv','电视剧分类',NULL,2,'2026-04-05 14:51:54','2026-04-05 14:51:54'),(3,'动漫','anime','动漫分类',NULL,3,'2026-04-05 14:51:54','2026-04-05 14:51:54'),(4,'纪录片','documentary','纪录片分类',NULL,4,'2026-04-05 14:51:54','2026-04-05 14:51:54'),(5,'综艺','variety','综艺分类',NULL,5,'2026-04-05 14:51:54','2026-04-05 14:51:54');
/*!40000 ALTER TABLE `category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `import_tasks`
--

DROP TABLE IF EXISTS `import_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `import_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(36) NOT NULL COMMENT 'Unique task ID',
  `source_name` varchar(500) DEFAULT NULL COMMENT 'Source file name',
  `source_path` varchar(1000) DEFAULT NULL COMMENT 'AList file path',
  `source_size` bigint DEFAULT '0' COMMENT 'Source file size',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending, downloading, scraping, transcoding, completed, failed',
  `progress` int DEFAULT '0' COMMENT 'Progress 0-100',
  `message` varchar(500) DEFAULT NULL COMMENT 'Status message',
  `video_uuid` varchar(36) DEFAULT NULL COMMENT 'Associated video UUID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_id` (`task_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_video_uuid` (`video_uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `import_tasks`
--

LOCK TABLES `import_tasks` WRITE;
/*!40000 ALTER TABLE `import_tasks` DISABLE KEYS */;
INSERT INTO `import_tasks` VALUES (1,'468468bb-97fb-4437-bc2d-d5a3bcd78f3b','The.Jungle.Book.2016.BD1080P.X264.AAC.Mandarin&English.CHS-ENG.Adans.mp4','/local/opt/The.Jungle.Book.2016.BD1080P.X264.AAC.Mandarin&English.CHS-ENG.Adans.mp4',5380001689,'completed',100,'Import complete','22eb4c7f-5887-42aa-aa15-28c9d2ff98cc','2026-04-04 03:11:52','2026-04-04 03:11:54'),(2,'41bf5bfe-174d-4ec9-8168-6367b776b122','425 闪闪王国（上）.mp4','/local/opt/425 闪闪王国（上）.mp4',344751819,'completed',100,'Import complete','c1bf3349-20b6-41af-91b8-821fa4fbc0cd','2026-04-04 14:16:18','2026-04-04 14:16:22'),(3,'818d5de5-8aee-4d9f-9970-782bf5b93f57','425 闪闪王国（上）.mp4','/local/425 闪闪王国（上）.mp4',344751819,'completed',100,'Import complete','504e383d-9726-43dd-93c5-89777208c418','2026-04-04 23:36:08','2026-04-04 23:36:10'),(4,'b455bc69-ce61-4a57-b757-90e2eba62ac2','801 立校多磨（上）.mp4','/local/801 立校多磨（上）.mp4',339066530,'completed',100,'Import complete','4b36164e-d31d-4661-845f-8a421d28c488','2026-04-06 07:43:12','2026-04-06 07:43:14'),(5,'0b4677f3-c55a-4c02-8a37-cca3c7248b63','801 立校多磨（上）.mp4','/local/801 立校多磨（上）.mp4',339066530,'completed',100,'Import complete','141487aa-6fb0-4e6d-90e1-7875b253c71e','2026-04-06 08:30:06','2026-04-06 08:30:08'),(6,'e21ba71d-c4e6-452b-9f00-785331a4523a','802 立校多磨（下）.mp4','/local/802 立校多磨（下）.mp4',343821983,'completed',100,'Import complete','3fb8ad4b-ce59-40bc-9bab-00734bc5b781','2026-04-06 10:56:20','2026-04-06 10:56:22'),(7,'84ff63fa-5a6d-42bc-8ea7-23b67c730619','803 地设一对.mp4','/local/803 地设一对.mp4',341760713,'completed',100,'Import complete','db462f70-46ef-43a9-b624-72274e4bb450','2026-04-06 11:43:02','2026-04-06 11:43:05');
/*!40000 ALTER TABLE `import_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `playback_progress`
--

DROP TABLE IF EXISTS `playback_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `playback_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `video_id` bigint NOT NULL,
  `user_identifier` varchar(100) DEFAULT NULL COMMENT 'User identifier',
  `position` bigint DEFAULT '0' COMMENT 'Playback position',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_video_user` (`video_id`,`user_identifier`),
  CONSTRAINT `playback_progress_ibfk_1` FOREIGN KEY (`video_id`) REFERENCES `video_metadata` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `playback_progress`
--

LOCK TABLES `playback_progress` WRITE;
/*!40000 ALTER TABLE `playback_progress` DISABLE KEYS */;
/*!40000 ALTER TABLE `playback_progress` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `season`
--

DROP TABLE IF EXISTS `season`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `season` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `series_id` bigint NOT NULL COMMENT 'Series ID',
  `season_number` int NOT NULL COMMENT 'Season number',
  `name` varchar(255) DEFAULT NULL COMMENT 'Season name (e.g. Season 1)',
  `poster_path` varchar(1000) DEFAULT NULL COMMENT 'Season poster path',
  `overview` text COMMENT 'Season overview',
  `tmdb_id` bigint DEFAULT NULL COMMENT 'TMDB season ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_series_season` (`series_id`,`season_number`),
  UNIQUE KEY `UKhmfspftj2yn91j8qi4w20ku9y` (`series_id`,`season_number`),
  KEY `idx_series_id` (`series_id`),
  CONSTRAINT `season_ibfk_1` FOREIGN KEY (`series_id`) REFERENCES `series` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `season`
--

LOCK TABLES `season` WRITE;
/*!40000 ALTER TABLE `season` DISABLE KEYS */;
INSERT INTO `season` VALUES (2,4,4,'第 4 季','https://image.tmdb.org/t/p/w500/aLCOjgWzqkXvEvJJImz7SBTRWKg.jpg','用丰富饱满、绚丽明亮的色彩、精致唯美的卡通画风打造出一上萌动温馨的童话世界，符合孩子的审美观，激发孩子的好奇心。发生在小马谷的趣事层出不穷，欢乐闹腾的小马们以别开生面的美式幽默淋漓尽致地为孩子们展现出一个个性格迥异却真实可爱的自我，让孩子们体会到不一样的成长教育。温馨美好的情商小故事讲述友情的“魔法”，让孩子学会珍惜朋友间的友情，关心爱护身边的人和事物，塑造孩子积极开朗友爱的性格。',44636,'2026-04-04 15:05:46','2026-04-06 07:32:27'),(3,4,8,'第 8 季','https://image.tmdb.org/t/p/w500/5FhbpHAqacpVOfEKbEOjoyZKPev.jpg','',96687,'2026-04-05 15:11:34','2026-04-06 07:32:31'),(4,4,3,'第 3 季','https://image.tmdb.org/t/p/w500/i5utQbMEkpFo6TEdu7UoC5oUOEH.jpg','故事叙述一只叫Twilight Sparkle(暮光闪闪）的小马为了执行导师Princess Celestia（塞拉斯蒂亚公主）给她的任务，与助手Spike（斯派克）一起来到Ponyville（小马镇/小马谷）学习有关友谊的魔法的知识，其中她认识了Applejack（苹果杰克、阿杰）、Rarity（瑞瑞、珍奇）、Fluttershy（小蝶）、Rainbow Dash（云宝黛西/云宝）与Pinkie Pie（萍琪派）五位好朋友，每只小马都代表着友谊的每个元素。',44635,'2026-04-06 04:59:15','2026-04-06 07:32:13'),(5,4,1,'第 1 季','https://image.tmdb.org/t/p/w500/wsnFydEEprBxfm3GFdl8uD247fM.jpg','一只叫紫悦的小马为了执行导师宇宙公主给她的任务,与助手穗龙一起来到小马谷学习有关友谊的知识.在小马谷中,她邂逅了几位好朋友,每只小马都代表着友谊的每个元素,并在和谐水晶中,各扮演重要的关键元素.此后,紫悦便与她认识的新朋友们开始了有趣的日常生活。',44632,'2026-04-06 07:32:24','2026-04-06 07:32:24'),(6,4,2,'第 2 季','https://image.tmdb.org/t/p/w500/iGKtRmNDUG71e43LuThQQXQy4Vc.jpg','故事叙述一只叫暮光闪闪(Twilight Sparkle)的小马为了执行导师塞拉斯提娅公主（Princess Celestia）给她的任务，与助手斯穗龙（Spike）一起来到小马镇学习有关友谊的魔法的知识，其中她认识了苹果杰克（Applejack）、瑞瑞（Rarity）、小蝶（Fluttershy）、云宝黛西（Rainbow Dash）与碧琪（Pinkie Pie）五位好朋友，每只小马都代表着友谊的每个元素，并在和谐水晶（Elements of Harmony）中，各扮演重要的关键元素。',44633,'2026-04-06 07:32:25','2026-04-06 07:32:25'),(7,4,5,'第 5 季','https://image.tmdb.org/t/p/w500/kbUuZ1GcJCKwaT7TDSP1SezNlSP.jpg','',65673,'2026-04-06 07:32:28','2026-04-06 07:32:28'),(8,4,6,'第 6 季','https://image.tmdb.org/t/p/w500/eXkjIy4wSdQ0T8nVKv6b0VP3jYr.jpg','',75134,'2026-04-06 07:32:29','2026-04-06 07:32:29'),(9,4,7,'第 7 季','https://image.tmdb.org/t/p/w500/s0txgbtgNP1vCXihBNWblT3YOMY.jpg','',86384,'2026-04-06 07:32:30','2026-04-06 07:32:30'),(10,4,9,'第 9 季','https://image.tmdb.org/t/p/w500/esotWrxcMsmoCAh9LAGlmGYbBF3.jpg','',120055,'2026-04-06 07:32:32','2026-04-06 07:32:32');
/*!40000 ALTER TABLE `season` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `series`
--

DROP TABLE IF EXISTS `series`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `series` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL COMMENT 'Series name',
  `slug` varchar(255) DEFAULT NULL COMMENT 'URL friendly name',
  `poster_path` varchar(1000) DEFAULT NULL COMMENT 'Poster image path',
  `backdrop_path` varchar(1000) DEFAULT NULL COMMENT 'Backdrop image path',
  `overview` text COMMENT 'Series overview',
  `tmdb_id` bigint DEFAULT NULL COMMENT 'TMDB series ID',
  `category_id` bigint DEFAULT NULL COMMENT 'Category ID',
  `sort_order` int DEFAULT '0' COMMENT 'Sort order',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `slug` (`slug`),
  KEY `idx_name` (`name`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_tmdb_id` (`tmdb_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `series`
--

LOCK TABLES `series` WRITE;
/*!40000 ALTER TABLE `series` DISABLE KEYS */;
INSERT INTO `series` VALUES (4,'小马宝莉','小马宝莉','https://image.tmdb.org/t/p/w500/e7iRrVn64fSKw7l6KBUpgswEpV6.jpg',NULL,'小马紫悦为了执行导师宇宙公主给她的任务，与助手穗龙一起来到小马谷学习有关友谊魔法的知识，在学习和生活的过程中她结识了五位好朋友，并获得了真正的友谊，与朋友们共同守护小马利亚。',33765,3,0,'2026-04-04 15:04:18','2026-04-05 15:14:11');
/*!40000 ALTER TABLE `series` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) NOT NULL COMMENT 'Config key',
  `config_value` text COMMENT 'Config value',
  `description` varchar(500) DEFAULT NULL COMMENT 'Description',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_config`
--

LOCK TABLES `system_config` WRITE;
/*!40000 ALTER TABLE `system_config` DISABLE KEYS */;
INSERT INTO `system_config` VALUES (1,'alist.storage_password','','AList å­˜å‚¨å¯†ç ','2026-04-04 00:37:03'),(2,'tmdb.api_key','','TMDB API Key','2026-04-04 00:37:03'),(3,'tmdb.language','zh-CN','TMDB API Language','2026-04-04 00:37:03'),(4,'aliyundrive.refresh_token','','é˜¿é‡Œäº‘ç›˜ Refresh Token','2026-04-04 00:37:03'),(5,'aliyundrive.root_folder_id','root','é˜¿é‡Œäº‘ç›˜æ ¹ç›®å½• ID','2026-04-04 00:37:03'),(6,'transcode_max_concurrent','2','æœ€å¤§å¹¶å‘è½¬ç æ•°','2026-04-04 03:04:45'),(7,'transcode_quality','23','è§†é¢‘è´¨é‡ CRF å€¼','2026-04-04 03:04:45'),(8,'alist_url','http://video-alist:5244',NULL,'2026-04-04 03:04:45'),(9,'alist_username','admin',NULL,'2026-04-04 03:04:45'),(10,'alist_password','admin123',NULL,'2026-04-04 03:04:45'),(11,'alist_token','',NULL,'2026-04-04 03:04:45'),(12,'tmdb_api_key','72c6d2372b0d91e52737b8cbe8c55773',NULL,'2026-04-04 03:04:45'),(13,'tmdb_language','zh-CN',NULL,'2026-04-04 03:04:45');
/*!40000 ALTER TABLE `system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transcode_tasks`
--

DROP TABLE IF EXISTS `transcode_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transcode_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `video_id` bigint NOT NULL,
  `video_uuid` varchar(36) NOT NULL,
  `progress` int DEFAULT '0' COMMENT 'Transcode progress 0-100',
  `ffmpeg_command` text COMMENT 'FFmpeg command',
  `error_message` text COMMENT 'Error message',
  `started_at` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `retry_count` int DEFAULT '0',
  `max_retries` int DEFAULT '3',
  `status` varchar(20) DEFAULT 'queued' COMMENT 'Status: queued, processing, completed, failed',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `video_id` (`video_id`),
  KEY `idx_video_uuid` (`video_uuid`),
  KEY `idx_status` (`status`),
  CONSTRAINT `transcode_tasks_ibfk_1` FOREIGN KEY (`video_id`) REFERENCES `video_metadata` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transcode_tasks`
--

LOCK TABLES `transcode_tasks` WRITE;
/*!40000 ALTER TABLE `transcode_tasks` DISABLE KEYS */;
INSERT INTO `transcode_tasks` VALUES (1,1,'22eb4c7f-5887-42aa-aa15-28c9d2ff98cc',100,'ffmpeg -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5 -i http://video-alist:5244/p/local/opt/The.Jungle.Book.2016.BD1080P.X264.AAC.Mandarin&English.CHS-ENG.Adans.mp4?sign=3YYMxEcGHDQyPgCJYG98YD6-NHBWYzCZ8KCSGesG4Qg=:0 -codec:v libx264 -preset fast -crf 23 -codec:a aac -b:a 128k -f hls -hls_time 10 -hls_list_size 0 -http_seekable 1 /data/videos/22eb4c7f-5887-42aa-aa15-28c9d2ff98cc/index.m3u8 -ss 00:00:05 -vframes 1 -vf scale=320:-1 /data/videos/22eb4c7f-5887-42aa-aa15-28c9d2ff98cc/thumbnail.jpg',NULL,'2026-04-04 03:11:54','2026-04-04 03:56:42',0,3,'completed','2026-04-04 03:11:54','2026-04-04 03:56:42'),(5,265,'141487aa-6fb0-4e6d-90e1-7875b253c71e',100,'ffmpeg -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5 -i http://video-alist:5244/p/local/801%20%E7%AB%8B%E6%A0%A1%E5%A4%9A%E7%A3%A8%EF%BC%88%E4%B8%8A%EF%BC%89.mp4?sign=tn0Pobp9k95wyArPDwtYG7cfiZRKr6jpSKcS7qo2wR4=:0 -codec:v libx264 -preset fast -crf 23 -codec:a aac -b:a 128k -f hls -hls_time 10 -hls_list_size 0 -http_seekable 1 /data/videos/141487aa-6fb0-4e6d-90e1-7875b253c71e/index.m3u8',NULL,'2026-04-06 08:30:08','2026-04-06 08:37:41',0,3,'completed','2026-04-06 08:30:08','2026-04-06 08:37:41'),(6,907,'3fb8ad4b-ce59-40bc-9bab-00734bc5b781',100,'ffmpeg -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5 -i http://video-alist:5244/p/local/802%20%E7%AB%8B%E6%A0%A1%E5%A4%9A%E7%A3%A8%EF%BC%88%E4%B8%8B%EF%BC%89.mp4?sign=8AYxDnthdz7BhWRIDb8Xw23QLZV9XIzayObRSXrAH60=:0 -codec:v libx264 -preset fast -crf 23 -codec:a aac -b:a 128k -f hls -hls_time 10 -hls_list_size 0 -http_seekable 1 /data/videos/3fb8ad4b-ce59-40bc-9bab-00734bc5b781/index.m3u8',NULL,'2026-04-06 10:56:23','2026-04-06 11:05:55',0,3,'completed','2026-04-06 10:56:22','2026-04-06 11:05:55'),(7,932,'db462f70-46ef-43a9-b624-72274e4bb450',100,'ffmpeg -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5 -i http://video-alist:5244/p/local/803%20%E5%9C%B0%E8%AE%BE%E4%B8%80%E5%AF%B9.mp4?sign=PbCZbkuxob3oJ0X9G3tn-XG0XxLWUFx-nJ7ROYCNLKk=:0 -codec:v libx264 -preset fast -crf 23 -codec:a aac -b:a 128k -f hls -hls_time 10 -hls_list_size 0 -http_seekable 1 /data/videos/db462f70-46ef-43a9-b624-72274e4bb450/index.m3u8',NULL,'2026-04-06 11:43:05','2026-04-06 11:51:58',0,3,'completed','2026-04-06 11:43:05','2026-04-06 11:51:58');
/*!40000 ALTER TABLE `transcode_tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT 'ç”¨æˆ·å',
  `password` varchar(255) NOT NULL COMMENT 'å¯†ç ',
  `nickname` varchar(100) DEFAULT NULL COMMENT 'æ˜µç§°',
  `avatar` varchar(500) DEFAULT NULL COMMENT 'å¤´åƒURL',
  `role` varchar(20) NOT NULL DEFAULT 'user' COMMENT 'è§’è‰²: admin, user',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT 'çŠ¶æ€: active, disabled',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi','ç®¡ç†å‘˜',NULL,'admin','active','2026-04-04 00:37:03','2026-04-04 00:37:03');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `video_metadata`
--

DROP TABLE IF EXISTS `video_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL COMMENT 'Video unique identifier',
  `title` varchar(255) NOT NULL COMMENT 'Video title',
  `original_filename` varchar(500) DEFAULT NULL COMMENT 'Original filename',
  `original_path` varchar(1000) DEFAULT NULL COMMENT 'Original video storage path',
  `hls_path` varchar(1000) DEFAULT NULL COMMENT 'HLS output path',
  `thumbnail_path` varchar(1000) DEFAULT NULL COMMENT 'Thumbnail path',
  `duration` bigint DEFAULT '0' COMMENT 'Duration in milliseconds',
  `file_size` bigint DEFAULT '0' COMMENT 'File size in bytes',
  `width` int DEFAULT '0' COMMENT 'Video width',
  `height` int DEFAULT '0' COMMENT 'Video height',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending, transcoding, completed, failed',
  `current_position` bigint DEFAULT '0' COMMENT 'Current playback position',
  `category_id` bigint DEFAULT NULL COMMENT 'Category ID',
  `overview` text COMMENT 'Overview/description',
  `poster_path` varchar(1000) DEFAULT NULL COMMENT 'Poster image path',
  `backdrop_path` varchar(1000) DEFAULT NULL COMMENT 'Backdrop image path',
  `tmdb_id` bigint DEFAULT NULL COMMENT 'TMDB ID',
  `imdb_id` varchar(20) DEFAULT NULL COMMENT 'IMDB ID',
  `douban_id` varchar(20) DEFAULT NULL COMMENT 'Douban ID',
  `rating` double DEFAULT NULL COMMENT 'Rating',
  `release_year` int DEFAULT NULL COMMENT 'Release year',
  `genres` varchar(500) DEFAULT NULL COMMENT 'Genres (comma separated)',
  `actors` text COMMENT 'Actors (JSON array)',
  `director` varchar(200) DEFAULT NULL COMMENT 'Director',
  `scraping_status` varchar(20) DEFAULT 'pending' COMMENT 'Scraping status: pending, success, failed',
  `series_id` bigint DEFAULT NULL COMMENT 'Series ID',
  `season_id` bigint DEFAULT NULL COMMENT 'Season ID',
  `episode_number` int DEFAULT NULL COMMENT 'Episode number',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `alist_path` longtext,
  `source_type` varchar(20) DEFAULT NULL,
  `actor_list_json` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_uuid` (`uuid`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_series_id` (`series_id`),
  KEY `idx_season_id` (`season_id`)
) ENGINE=InnoDB AUTO_INCREMENT=933 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `video_metadata`
--

LOCK TABLES `video_metadata` WRITE;
/*!40000 ALTER TABLE `video_metadata` DISABLE KEYS */;
INSERT INTO `video_metadata` VALUES (1,'22eb4c7f-5887-42aa-aa15-28c9d2ff98cc','奇幻森林','The.Jungle.Book.2016.BD1080P.X264.AAC.Mandarin&English.CHS-ENG.Adans.mp4',NULL,'/data/videos/22eb4c7f-5887-42aa-aa15-28c9d2ff98cc/index.m3u8','https://image.tmdb.org/t/p/w500/hBXm5f96oxm23eAiDTwyHos6cvP.jpg',0,5380001689,0,0,'completed',0,NULL,'毛克利（尼尔·塞西 Neel Sethi 饰）是一个由狼群养大的人类男孩，影片围绕他的森林冒险徐徐展开。谢利·可汗，一只受过人类伤害的老虎（伊德里斯·艾尔巴 Idris Elba 配音），发誓要将毛克利铲除。为了逃脱追捕，毛克利跟随严厉的导师黑豹巴希拉（本·金斯利 Ben Kingsley 配音）和自由自在的好友棕熊巴鲁（比尔·默瑞 Bill Murray 配音），踏上了一场精彩纷呈的自我探索旅程。在这趟旅途中，毛克利遇到了一些对他居心叵测的丛林生物，包括巨蟒卡奥（斯嘉丽·约翰逊 Scarlett Johansson 配音），她用迷人的声音和眼神迷惑毛克利；还有花言巧语的猩猩路易王（克里斯托弗·沃肯 Christopher Walken 配音），他试图通过威逼利诱让毛克利交出迷幻而致命的“红花”——火焰的秘密。当森林不再是安全的家园，毛克利该何去何从？而毕生难忘的伟大冒险，才刚刚拉开序幕……','https://image.tmdb.org/t/p/w500/hBXm5f96oxm23eAiDTwyHos6cvP.jpg',NULL,NULL,NULL,NULL,6.9,2016,NULL,'尼尔·塞西, 比尔·默瑞, 本·金斯利, 伊德瑞斯·艾尔巴, 斯嘉丽·约翰逊, 克里斯托弗·沃肯, 露皮塔·尼永奥, 吉安卡罗·埃斯波西托, 盖瑞·山德林, 安杰·安东尼','乔恩·费儒','success',NULL,NULL,NULL,'2026-04-04 03:11:54','2026-04-06 12:23:54','/local/opt/The.Jungle.Book.2016.BD1080P.X264.AAC.Mandarin&English.CHS-ENG.Adans.mp4','remote_alist','[{\"name\":\"尼尔·塞西\",\"character\":\"Mowgli\",\"profilePath\":\"/aPAStca2iW94rfiosXWvGi6xRGM.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/aPAStca2iW94rfiosXWvGi6xRGM.jpg\"},{\"name\":\"比尔·默瑞\",\"character\":\"Baloo (voice)\",\"profilePath\":\"/nnCsJc9x3ZiG3AFyiyc3FPehppy.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/nnCsJc9x3ZiG3AFyiyc3FPehppy.jpg\"},{\"name\":\"本·金斯利\",\"character\":\"Bagheera (voice)\",\"profilePath\":\"/vQtBqpF2HDdzbfXHDzR4u37i1Ac.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/vQtBqpF2HDdzbfXHDzR4u37i1Ac.jpg\"},{\"name\":\"伊德瑞斯·艾尔巴\",\"character\":\"Shere Khan (voice)\",\"profilePath\":\"/be1bVF7qGX91a6c5WeRPs5pKXln.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/be1bVF7qGX91a6c5WeRPs5pKXln.jpg\"},{\"name\":\"斯嘉丽·约翰逊\",\"character\":\"Kaa (voice)\",\"profilePath\":\"/mjReG6rR7NPMEIWb1T4YWtV11ty.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/mjReG6rR7NPMEIWb1T4YWtV11ty.jpg\"},{\"name\":\"克里斯托弗·沃肯\",\"character\":\"King Louie (voice)\",\"profilePath\":\"/ApgDL7nudR9T2GpjCG4vESgymO2.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/ApgDL7nudR9T2GpjCG4vESgymO2.jpg\"},{\"name\":\"露皮塔·尼永奥\",\"character\":\"Raksha (voice)\",\"profilePath\":\"/y40Wu1T742kynOqtwXASc5Qgm49.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/y40Wu1T742kynOqtwXASc5Qgm49.jpg\"},{\"name\":\"吉安卡罗·埃斯波西托\",\"character\":\"Akela (voice)\",\"profilePath\":\"/rcXnr82TwDzU4ZGdBeNXfG0ZQnZ.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/rcXnr82TwDzU4ZGdBeNXfG0ZQnZ.jpg\"},{\"name\":\"盖瑞·山德林\",\"character\":\"Ikki (voice)\",\"profilePath\":\"/zGjPMqSqtZtP3npd5fhm7MYqxIU.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/zGjPMqSqtZtP3npd5fhm7MYqxIU.jpg\"},{\"name\":\"安杰·安东尼\",\"character\":\"Young Wolf #1 (voice)\",\"profilePath\":\"/u7R4CWfmTqsLexejsMNxWrPYKMG.jpg\",\"profileUrl\":\"https://image.tmdb.org/t/p/w500/u7R4CWfmTqsLexejsMNxWrPYKMG.jpg\"}]'),(265,'141487aa-6fb0-4e6d-90e1-7875b253c71e','立校多磨（上）','801 立校多磨（上）.mp4',NULL,'/data/videos/141487aa-6fb0-4e6d-90e1-7875b253c71e/index.m3u8','https://image.tmdb.org/t/p/w500/eqxrRfBYyPwpfelEwDWKNhu9Ml9.jpg',0,339066530,0,0,'completed',0,NULL,'当可爱地图扩大了范围，显示了小马国以外的世界时，暮光闪闪决定开一个友谊学园。',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'scraped',4,3,1,'2026-04-06 08:30:08','2026-04-06 11:32:10','/local/801 立校多磨（上）.mp4','remote_alist',NULL),(907,'3fb8ad4b-ce59-40bc-9bab-00734bc5b781','立校多磨（下）','802 立校多磨（下）.mp4',NULL,'/data/videos/3fb8ad4b-ce59-40bc-9bab-00734bc5b781/index.m3u8','https://image.tmdb.org/t/p/w500/jAF1bXyZ6jmZpyRyAACBgnjNFY4.jpg',0,343821983,0,0,'completed',0,NULL,'暮光闪闪和她的学生们尝试让友谊学园免遭小马国教育委员会的关停。',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'scraped',4,3,2,'2026-04-06 10:56:22','2026-04-06 11:32:10','/local/802 立校多磨（下）.mp4','remote_alist',NULL),(932,'db462f70-46ef-43a9-b624-72274e4bb450','地设一对','803 地设一对.mp4',NULL,'/data/videos/db462f70-46ef-43a9-b624-72274e4bb450/index.m3u8','https://image.tmdb.org/t/p/w500/qSSYz6QRTFcCiffOqGIn7J0031R.jpg',0,341760713,0,0,'completed',0,NULL,'在石灰交了一位令萍琪相当厌恶的男朋友之后，姐妹俩之间如胶如漆的感情面临了挑战。',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'scraped',4,3,3,'2026-04-06 11:43:04','2026-04-06 11:55:34','/local/803 地设一对.mp4','remote_alist',NULL);
/*!40000 ALTER TABLE `video_metadata` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-06 14:27:54
