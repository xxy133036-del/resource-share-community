-- ============================================================
--  Nacos 旧表结构(1.4.x) → Nacos 2.2.3 升级脚本
--
--  【为什么需要】
--  本机 nacos 库是早期版本 Nacos 建的，缺少 2.x 必需字段，
--  会让 Nacos 2.2.3 启动时报：
--      Unknown column 'encrypted_data_key' in 'field list'
--  对应日志：DumpService ... dumpservice bean construction failure
--
--  【安全性】
--  纯新增列 / 新增表，不改动、不删除任何既有数据，
--  命名空间(hc1job01)、Sentinel 规则等配置全部保留。
--
--  【执行方式】
--  mysql -uroot -p123456 -h127.0.0.1 < 02-nacos-223-upgrade.sql
-- ============================================================

USE `nacos`;

-- 1. config_info：补加密密钥列
ALTER TABLE `config_info`
  ADD COLUMN `encrypted_data_key` text NOT NULL COMMENT '配置加密后的密钥';

-- 2. config_info_beta：补加密密钥列
ALTER TABLE `config_info_beta`
  ADD COLUMN `encrypted_data_key` text NOT NULL COMMENT '配置加密后的密钥';

-- 3. his_config_info：补加密密钥列
ALTER TABLE `his_config_info`
  ADD COLUMN `encrypted_data_key` text NOT NULL COMMENT '配置加密后的密钥';

-- 4. 灰度配置表（Nacos 2.2.0 引入，2.2.3 启动时会访问）
CREATE TABLE IF NOT EXISTS `config_info_gray` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `data_id` varchar(255) NOT NULL,
  `group_id` varchar(128) NOT NULL,
  `content` longtext NOT NULL,
  `md5` varchar(32) DEFAULT NULL,
  `src_user` text,
  `src_ip` varchar(50) DEFAULT NULL,
  `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `app_name` varchar(128) DEFAULT NULL,
  `tenant_id` varchar(128) DEFAULT '',
  `gray_name` varchar(128) NOT NULL,
  `gray_rule` text NOT NULL,
  `encrypted_data_key` varchar(256) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfogray_datagrouptenantgray` (`data_id`,`group_id`,`tenant_id`,`gray_name`),
  KEY `idx_dataid_gmt_modified` (`data_id`,`gmt_modified`),
  KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
