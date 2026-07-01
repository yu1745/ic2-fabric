# ic2-analytics-worker

IC2_120 mod 的匿名使用统计后端（Cloudflare Worker + D1）。

仅存储：随机 installId（mod 端生成的 UUID）+ mod 版本 + Minecraft 版本 + 来源（server/client）+ 时间戳。
**不**存储 IP、用户名、世界名等任何可追溯信息。

## 当前部署信息

- Worker URL：`https://ic2-analytics.wangyu174551226.workers.dev`
- D1 数据库：`ic2_analytics`（id `4f01dbfb-bd3f-4e85-9221-e20a03f1b266`，region WEUR）
- `/stats` 鉴权 token（`STATS_TOKEN` secret）：`3PHsK8ksJVEijC0xhvQhTfgZ_9P_MxT0`
- mod 内置默认上报地址已指向上述 Worker URL（`AnalyticsReporter.DEFAULT_ENDPOINT`）。
- `/chart.svg` 缓存策略：`Cache-Control: public, max-age=3600`（1 小时）。这是针对 GitHub Camo 代理的最优值——**不要改成 `no-cache`/`no-store`**，Camo 对无缓存头的资源会回退到 1 年默认缓存（见 GitHub community discussion #22283），明确的短 `max-age` 才能让 Camo 定期回源刷新。chart 数据按天变化，滞后 1 小时完全无影响。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/ping` | mod 上报。Body：`{ installId, modVersion, mcVersion, source }`。按 (UTC日期, installId, source) 去重。永远返回 `{ ok: true }`。 |
| `GET`  | `/stats?days=30` | 返回最近 N 天聚合：`{ days, rows: [{ date, unique_installs, server_count, client_count }] }`。设置 `STATS_TOKEN` secret 后需鉴权。 |
| `GET`  | `/chart.svg?days=30` | 返回最近 N 天「每日安装人数」折线图（`image/svg+xml`），公开无需鉴权，供 README 嵌入。`lang=zh\|en` 切换中英双语（默认 zh）：`![](https://.../chart.svg?days=30&lang=en)`。 |

## 部署步骤

需已 `npm login` 到 Cloudflare（`npx wrangler login`）。

```bash
cd analytics-worker
npm install

# 1. 创建 D1 数据库
npx wrangler d1 create ic2_analytics
#    把输出里的 database_id 填进 wrangler.jsonc 的 d1_databases[0].database_id

# 2. 建表（远程）
npx wrangler d1 migrations apply ic2_analytics --remote

# 3. （可选）给 /stats 加访问口令
npx wrangler secret put STATS_TOKEN

# 4. 部署
npx wrangler deploy
#    部署后会得到 https://ic2-analytics.<你的子域>.workers.dev
```

## 把 Worker 域名同步给 mod

部署后，把得到的 Worker URL 填入游戏配置：

- 配置文件 `config/ic2_120.json` → `general.analyticsEndpoint`，例如 `"https://ic2-analytics.xxx.workers.dev"`。
- 留空则用 mod 内置默认值（占位，可能不可达——上报失败会被静默吞掉，不影响游戏）。

## 本地调试

```bash
npx wrangler d1 migrations apply ic2_analytics --local   # 建本地表
npx wrangler dev                                          # 起本地 dev server

# 测上报
curl -X POST http://localhost:8787/ping \
  -H 'Content-Type: application/json' \
  -d '{"installId":"test-uuid-1","modVersion":"1.0.0","mcVersion":"1.20.1","source":"server"}'

# 查统计
curl 'http://localhost:8787/stats?days=7'
```

## 查看已入库数据

```bash
npx wrangler d1 execute ic2_analytics --remote \
  --command "SELECT date, COUNT(DISTINCT install_id) AS installs FROM daily_active GROUP BY date ORDER BY date DESC LIMIT 14;"
```
