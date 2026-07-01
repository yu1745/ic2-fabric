-- 匿名使用统计表：按 (UTC日期, installId, source) 去重
-- installId: mod 端生成的随机 UUID，不含任何个人信息
-- source:    'server'（服务端启动）| 'client'（客户端加入世界）
CREATE TABLE IF NOT EXISTS daily_active (
  date          TEXT    NOT NULL,   -- UTC YYYY-MM-DD
  install_id    TEXT    NOT NULL,   -- mod 端随机 UUID
  mod_version   TEXT    NOT NULL,
  mc_version    TEXT    NOT NULL,
  source        TEXT    NOT NULL,   -- 'server' | 'client'
  first_seen_at INTEGER NOT NULL,   -- unix 秒
  last_seen_at  INTEGER NOT NULL,   -- unix 秒
  ping_count    INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (date, install_id, source)
);

-- 按日期查询的辅助索引（unique_installs / server_count / client_count 聚合）
CREATE INDEX IF NOT EXISTS idx_daily_active_date ON daily_active(date);
