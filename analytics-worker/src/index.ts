/**
 * IC2_120 匿名使用统计后端
 *
 * 接口：
 *   POST /ping   —— mod 上报一次使用。Body: { installId, modVersion, mcVersion, source }
 *                   按 (UTC日期, installId, source) 去重 upsert。永远返回 200 { ok: true }。
 *   GET  /stats  —— 查询最近 N 天聚合。?days=30
 *                   若设置了 STATS_TOKEN secret，需带 ?token=xxx 或 Authorization: Bearer xxx。
 *   GET  /chart.svg —— 返回最近 N 天「每日安装人数」折线图（image/svg+xml），公开无需鉴权，
 *                   供 README / markdown 直接嵌入。?lang=zh|en 切换中英双语（默认 zh）。
 *
 * 隐私：仅存储 installId(随机UUID) + 版本号 + 来源 + 时间戳。
 *       不存储 IP、用户名、世界名等任何可追溯信息。
 */

import { renderChart } from "./chart";

interface Env {
  DB: D1Database;
  STATS_TOKEN?: string;
}

interface PingBody {
  installId?: unknown;
  modVersion?: unknown;
  mcVersion?: unknown;
  source?: unknown;
}

const MAX_BODY_BYTES = 4096;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...corsHeaders },
  });
}

function isNonEmptyString(v: unknown): v is string {
  return typeof v === "string" && v.length > 0 && v.length <= 128;
}

/** UTC YYYY-MM-DD */
function todayUTC(d = new Date()): string {
  return d.toISOString().slice(0, 10);
}

/** 任何错误都吞掉，返回 200 { ok: true }，绝不干扰调用方（mod 端不在意结果）。 */
async function handlePing(request: Request, env: Env): Promise<Response> {
  let body: PingBody = {};
  try {
    const text = await request.text();
    if (text.length > MAX_BODY_BYTES) return json({ ok: true });
    body = JSON.parse(text) as PingBody;
  } catch {
    return json({ ok: true });
  }

  const installId = isNonEmptyString(body.installId) ? body.installId : null;
  const modVersion = isNonEmptyString(body.modVersion) ? body.modVersion.slice(0, 64) : "unknown";
  const mcVersion = isNonEmptyString(body.mcVersion) ? body.mcVersion.slice(0, 64) : "unknown";
  const source = body.source === "server" || body.source === "client" ? body.source : "unknown";

  if (!installId) return json({ ok: true });

  const date = todayUTC();
  const now = Math.floor(Date.now() / 1000);

  try {
    await env.DB.prepare(
      `INSERT INTO daily_active (date, install_id, mod_version, mc_version, source, first_seen_at, last_seen_at, ping_count)
       VALUES (?, ?, ?, ?, ?, ?, ?, 1)
       ON CONFLICT(date, install_id, source) DO UPDATE SET
         last_seen_at = excluded.last_seen_at,
         ping_count   = daily_active.ping_count + 1`,
    )
      .bind(date, installId, modVersion, mcVersion, source, now, now)
      .run();
  } catch {
    // 吞错，不影响 mod
  }

  return json({ ok: true });
}

function checkStatsToken(request: Request, env: Env): boolean {
  if (!env.STATS_TOKEN) return true; // 未设置 secret 则公开
  const url = new URL(request.url);
  const queryToken = url.searchParams.get("token");
  if (queryToken && queryToken === env.STATS_TOKEN) return true;
  const auth = request.headers.get("Authorization") ?? "";
  if (auth.startsWith("Bearer ") && auth.slice(7) === env.STATS_TOKEN) return true;
  return false;
}

async function handleStats(request: Request, env: Env): Promise<Response> {
  if (!checkStatsToken(request, env)) {
    return json({ error: "unauthorized" }, 401);
  }

  const url = new URL(request.url);
  const daysRaw = Number.parseInt(url.searchParams.get("days") ?? "30", 10);
  const days = Number.isFinite(daysRaw) && daysRaw > 0 && daysRaw <= 365 ? daysRaw : 30;

  try {
    const result = await env.DB.prepare(
      `SELECT date,
              COUNT(DISTINCT install_id) AS unique_installs,
              SUM(CASE WHEN source = 'server' THEN 1 ELSE 0 END) AS server_count,
              SUM(CASE WHEN source = 'client' THEN 1 ELSE 0 END) AS client_count
       FROM daily_active
       WHERE date >= date('now', ?)
       GROUP BY date
       ORDER BY date DESC`,
    )
      .bind(`-${days} day`)
      .all<{
        date: string;
        unique_installs: number;
        server_count: number;
        client_count: number;
      }>();

    return json({ days, rows: result.results ?? [] });
  } catch (e) {
    return json({ error: "query_failed", message: String(e) }, 500);
  }
}

/** 读取最近 N 天的每日聚合（unique_installs），供图表与统计共用。 */
async function fetchDailyRows(env: Env, days: number): Promise<{ date: string; unique_installs: number }[]> {
  const result = await env.DB.prepare(
    `SELECT date, COUNT(DISTINCT install_id) AS unique_installs
     FROM daily_active
     WHERE date >= date('now', ?)
     GROUP BY date
     ORDER BY date ASC`,
  )
    .bind(`-${days} day`)
    .all<{ date: string; unique_installs: number }>();
  return result.results ?? [];
}

/** GET /chart.svg —— 公开（无需鉴权），返回 SVG 折线图，供 README 嵌入。 */
async function handleChart(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const daysRaw = Number.parseInt(url.searchParams.get("days") ?? "30", 10);
  const days = Number.isFinite(daysRaw) && daysRaw > 0 && daysRaw <= 365 ? daysRaw : 30;

  let rows: { date: string; unique_installs: number }[] = [];
  try {
    rows = await fetchDailyRows(env, days);
  } catch {
    rows = [];
  }

  // lang: zh | en，默认 zh
  const lang = url.searchParams.get("lang") === "en" ? "en" : "zh";
  const svg = renderChart(rows, days, lang);
  return new Response(svg, {
    status: 200,
    headers: {
      "Content-Type": "image/svg+xml; charset=utf-8",
      "Cache-Control": "public, max-age=3600",
      "Access-Control-Allow-Origin": "*",
    },
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    if (url.pathname === "/ping" && request.method === "POST") {
      return handlePing(request, env);
    }
    if (url.pathname === "/stats" && request.method === "GET") {
      return handleStats(request, env);
    }
    if (url.pathname === "/chart.svg" && request.method === "GET") {
      return handleChart(request, env);
    }

    return json({ error: "not_found" }, 404);
  },
};
