-- ============================================================
--  file_info 增加 file_hash 列（MD5 秒传去重）
--
--  【为什么需要】
--  FileServiceImpl.uploadFile 在流式落盘时计算文件 MD5，
--  并通过 FileMapper.selectByFileHash 查重实现秒传。
--  缺这一列会导致上传报：
--      Unknown column 'file_hash' in 'field list'
--
--  【安全性】
--  纯新增列 + 索引，不改动既有数据；历史数据的 file_hash 为 NULL，
--  不影响查询（后续新上传的文件才会写入哈希）。
--
--  【执行方式】
--  mysql -uroot -p123456 -h127.0.0.1 hc1db_user < 03-add-file-hash.sql
-- ============================================================

USE `hc1db_user`;

ALTER TABLE `file_info`
  ADD COLUMN `file_hash` varchar(64) DEFAULT NULL COMMENT '文件MD5，用于秒传去重';

-- 秒传查询走这个索引，避免全表扫描
CREATE INDEX `idx_file_hash` ON `file_info` (`file_hash`);
