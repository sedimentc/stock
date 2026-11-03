-- 先在测试数据库执行，并按团队流程备份。该表不修改原有评分字段。
CREATE TABLE IF NOT EXISTS stock_multi_rating (
  ticker_symbol VARCHAR(32) NOT NULL,
  rater_name VARCHAR(64) NOT NULL,
  overall_score DECIMAL(4,1) DEFAULT NULL,
  potential_score DECIMAL(4,1) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (ticker_symbol, rater_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
