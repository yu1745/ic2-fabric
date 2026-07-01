/**
 * 纯字符串拼装的 SVG 折线图渲染器。
 * 纵轴 = 当日独立安装人数（unique_installs），横轴 = 日期。
 * 通过 lang 参数切换中英双语（zh | en，默认 zh）。
 */

interface DayRow {
  date: string; // YYYY-MM-DD
  unique_installs: number;
}

type Lang = "zh" | "en";

const I18N: Record<Lang, { title: (days: number) => string; noData: string; yLabel: string; xLabel: string }> = {
  zh: {
    title: (days) => `IC2-120 每日活跃安装数（近 ${days} 天）`,
    noData: "暂无数据",
    yLabel: "安装人数",
    xLabel: "日期",
  },
  en: {
    title: (days) => `IC2-120 Daily Active Installs (last ${days} days)`,
    noData: "No data yet",
    yLabel: "Installs",
    xLabel: "Date",
  },
};

const W = 720;
const H = 260;
const PAD_L = 48;
const PAD_R = 16;
const PAD_T = 20;
const PAD_B = 40;

function escapeXml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

/** 把 YYYY-MM-DD 渲染成 MM-DD（省空间）。 */
function shortDate(iso: string): string {
  return iso.length >= 10 ? iso.slice(5) : iso;
}

export function renderChart(rows: DayRow[], days: number, lang: Lang = "zh"): string {
  const t = I18N[lang] ?? I18N.zh;
  const plotW = W - PAD_L - PAD_R;
  const plotH = H - PAD_T - PAD_B;

  // 把缺失的日期补 0，保证 X 轴连续
  const filled: DayRow[] = [];
  if (rows.length > 0) {
    const map = new Map(rows.map((r) => [r.date, r.unique_installs]));
    const today = new Date();
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(today);
      d.setUTCDate(d.getUTCDate() - i);
      const iso = d.toISOString().slice(0, 10);
      filled.push({ date: iso, unique_installs: map.get(iso) ?? 0 });
    }
  }

  const maxVal = filled.length > 0 ? Math.max(...filled.map((r) => r.unique_installs), 1) : 1;
  // Y 轴刻度：取 4 段，向上取整到整齐值
  const yTicks = niceTicks(maxVal);

  const xStep = filled.length > 1 ? plotW / (filled.length - 1) : 0;
  const yScale = (v: number) => PAD_T + plotH - (v / yTicks[yTicks.length - 1]) * plotH;
  const xScale = (i: number) => PAD_L + i * xStep;

  const parts: string[] = [];
  parts.push(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif">`,
  );
  parts.push(`<rect width="${W}" height="${H}" fill="#ffffff"/>`);

  // 标题
  parts.push(
    `<text x="${W / 2}" y="14" text-anchor="middle" font-size="13" fill="#24292f" font-weight="600">` +
      `${escapeXml(t.title(days))}</text>`,
  );

  if (filled.length === 0) {
    parts.push(
      `<text x="${W / 2}" y="${H / 2}" text-anchor="middle" font-size="13" fill="#6e7781">${escapeXml(t.noData)}</text>`,
    );
    parts.push(`</svg>`);
    return parts.join("");
  }

  // Y 轴网格线 + 刻度标签
  for (const tick of yTicks) {
    const y = yScale(tick);
    parts.push(
      `<line x1="${PAD_L}" y1="${y.toFixed(1)}" x2="${W - PAD_R}" y2="${y.toFixed(1)}" stroke="#e1e4e8" stroke-width="1"/>`,
    );
    parts.push(
      `<text x="${PAD_L - 6}" y="${(y + 3).toFixed(1)}" text-anchor="end" font-size="10" fill="#6e7781">${tick}</text>`,
    );
  }

  // X 轴日期标签（密集时稀疏化）
  const labelEvery = Math.max(1, Math.ceil(filled.length / 8));
  for (let i = 0; i < filled.length; i++) {
    if (i % labelEvery !== 0 && i !== filled.length - 1) continue;
    parts.push(
      `<text x="${xScale(i).toFixed(1)}" y="${H - PAD_B + 16}" text-anchor="middle" font-size="10" fill="#6e7781">${escapeXml(
        shortDate(filled[i].date),
      )}</text>`,
    );
  }

  // 折线
  const linePts = filled.map((_, i) => `${xScale(i).toFixed(1)},${yScale(filled[i].unique_installs).toFixed(1)}`).join(" ");
  parts.push(`<polyline points="${linePts}" fill="none" stroke="#0969da" stroke-width="2"/>`);

  // 折线下方填充（淡蓝）
  const areaPts =
    `${PAD_L},${(PAD_T + plotH).toFixed(1)} ` +
    filled.map((_, i) => `${xScale(i).toFixed(1)},${yScale(filled[i].unique_installs).toFixed(1)}`).join(" ") +
    ` ${(W - PAD_R).toFixed(1)},${(PAD_T + plotH).toFixed(1)}`;
  parts.push(`<polygon points="${areaPts}" fill="#0969da" fill-opacity="0.08"/>`);

  // 数据点
  for (let i = 0; i < filled.length; i++) {
    parts.push(
      `<circle cx="${xScale(i).toFixed(1)}" cy="${yScale(filled[i].unique_installs).toFixed(1)}" r="2.5" fill="#0969da"/>`,
    );
  }

  // 坐标轴
  parts.push(
    `<line x1="${PAD_L}" y1="${PAD_T}" x2="${PAD_L}" y2="${PAD_T + plotH}" stroke="#d0d7de" stroke-width="1"/>`,
  );
  parts.push(
    `<line x1="${PAD_L}" y1="${PAD_T + plotH}" x2="${W - PAD_R}" y2="${PAD_T + plotH}" stroke="#d0d7de" stroke-width="1"/>`,
  );

  // 轴名
  parts.push(
    `<text x="${PAD_L - 36}" y="${PAD_T + plotH / 2}" text-anchor="middle" font-size="10" fill="#6e7781" transform="rotate(-90 ${PAD_L - 36} ${PAD_T + plotH / 2})">${escapeXml(t.yLabel)}</text>`,
  );
  parts.push(
    `<text x="${W / 2}" y="${H - 6}" text-anchor="middle" font-size="10" fill="#6e7781">${escapeXml(t.xLabel)}</text>`,
  );

  parts.push(`</svg>`);
  return parts.join("");
}

/** 生成 5 段整齐的 Y 轴刻度（0, step, 2*step, ...），覆盖到 >= max。 */
function niceTicks(max: number): number[] {
  if (max <= 0) return [0, 1];
  const rawStep = max / 4;
  const mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
  const norm = rawStep / mag;
  let step: number;
  if (norm < 1.5) step = 1 * mag;
  else if (norm < 3) step = 2 * mag;
  else if (norm < 7) step = 5 * mag;
  else step = 10 * mag;
  const ticks: number[] = [];
  for (let v = 0; v <= max + step; v += step) ticks.push(v);
  // 保证至少覆盖 max
  if (ticks[ticks.length - 1] < max) ticks.push(ticks[ticks.length - 1] + step);
  return ticks;
}
