ALTER TABLE `psn`.`user`
ADD COLUMN `bookmarkChangeTime` BIGINT(20) UNSIGNED NOT NULL DEFAULT 0 AFTER `lastLoginTime`;
