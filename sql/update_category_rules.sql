-- ==============================================================
-- 星界财务 (Star Realm) - 分类规则自演进与用户习惯记忆表结构扩展
-- ==============================================================

USE `personal_fi_plat`;

-- 1. 用户个性化分类记忆表（单用户私有隔离，最高优先级 Level 0）
DROP TABLE IF EXISTS `user_category_rule`;
CREATE TABLE `user_category_rule` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `keyword` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '特征(商户名称/凭证/手机号等)',
  `category` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户指定的标准分类',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_keyword` (`user_id`, `keyword`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户个性化分类记忆表';

-- 2. 动态全局共识规则表（高比例共识自动回流，次级优先级 Level 1）
-- 触发晋升门槛：独立用户样本数 >= 100，且修改为同一分类的共识比例 >= 85%
DROP TABLE IF EXISTS `global_category_rule`;
CREATE TABLE `global_category_rule` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户特征关键字',
  `category` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '大众高共识标准分类',
  `consensus_ratio` DECIMAL(5,2) NOT NULL COMMENT '共识百分比(如 88.50%)',
  `sample_count` INT NOT NULL COMMENT '参与修正的独立用户样本量(>=100)',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '晋升生效时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态全局高共识分类规则表';
