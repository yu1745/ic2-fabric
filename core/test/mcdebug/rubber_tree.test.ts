// 橡胶树生长回归测试：覆盖"骨粉催熟树苗不应删除周围原木"这个 bug。
//
// 背景：原 `RubberTreeFeature.isSaplingGrowth` 单纯用
// `context.world.getBlockState(context.origin).block is RubberSaplingBlock` 判别场景。
// 但 vanilla `SaplingGenerator.generate` 在调用 feature 之前会先
// `world.setBlockState(pos, world.getFluidState(pos).getBlockState(), NO_REDRAW)`，
// 树苗位置被替换成流体状态（无水 = AIR），导致上述判别永远为 false，
// 实际逻辑错误地走了"自然世界生成"分支 → 删除/替换周围原木。
//
// 修复：覆写 `RubberSaplingGenerator.generate`，用 ThreadLocal 显式标记
// "这是树苗生长路径"，并仍然在调用 feature 前把树苗替换为流体状态（与 vanilla
// 行为一致，避免 vanilla `TreeFeature.getTopPosition` 在 i=0 处看到 sapling 方块
// 而提前返回失败高度）。
//
// 本测试同时验证：
// 1. 树苗+骨粉 能长出橡胶树（树苗路径修复没把事情弄得更糟）
// 2. 周围 4 块原木在催熟后 **仍然存在**（这是原 bug 的核心症状）
import assert from "node:assert/strict";
import {
  defineTest,
  defineTests,
  place,
  waitTicks,
  setBlocks,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

/** 骨粉催熟一次；用 face=south 命中树苗所在方块。 */
async function boneMeal(ctx: TestContext): Promise<void> {
  // ctx.origin 是 mcdebug `pos()` 返回的数组（带 east/west/... helper），
  // server-side 期望 `pos` 是 JSON 数组 `[x, y, z]`，直接传过去即可。
  await ctx.api.world.useOnBlock(ctx.origin, "south", {
    item: "minecraft:bone_meal",
    count: 1,
  });
}

export const rubberTreeTests = defineTests([
  defineTest("rubber_tree:sapling+bone_meal does NOT remove surrounding logs", async (ctx) => {
    const origin = ctx.origin;
    const dirt = origin.down();
    const sapling = origin;

    // 1) 摆好地板 + stage=1 树苗（stage=1 模拟已经被第一次骨粉推到可生长状态）。
    //    测试运行器默认清理范围是 y=origin-1 到 y=origin+6，橡胶树能长到 y=origin+8，
    //    失败的旧用例可能在该范围之上留下 rubber_log，污染下一次测试。
    //    我们先清掉一块覆盖橡胶树整个高度的区域，再开始场景。
    //
    //    注意：必须用 `world.setBlock` + `state.props`，server 端只读 `stateProps` 字段；
    //    `setBlocks` helper 把 state 包装成 `state: { name, props }` 整个塞进 params，
    //    server 看不到，等于没传 state。结果就是树苗 stage=0，每次 bone meal
    //    只会把 stage 推到 1（45% 概率），永远到不了生成树的那一步。
    const clearFrom = [origin[0] - 4, origin[1] - 1, origin[2] - 4];
    const clearTo = [origin[0] + 4, origin[1] + 12, origin[2] + 4];
    await ctx.api.world.clearBox({ from: clearFrom, to: clearTo }, { maxBlocks: 4096 });

    await place(ctx, dirt, "minecraft:dirt");
    await ctx.api.world.setBlock(sapling, "ic2_120:rubber_sapling", {
      name: "ic2_120:rubber_sapling",
      props: { stage: "1" },
    });

    // 2) 摆 4 块周围原木（x/z 各偏移 ±1，同 y），模拟截图里"4 块原木围着树苗"的场景。
    const surroundings = [origin.west(), origin.east(), origin.north(), origin.south()];
    await setBlocks(ctx, surroundings.map((p) => ({ pos: p, block: "ic2_120:rubber_log" })));

    // 3) 多次骨粉：vanilla `SaplingBlock.canGrow` 只有 45% 概率通过，
    //    连续催熟直到树真的长出来（最多 20 次）。每两次之间等 1 tick。
    //    用 origin 变成 rubber_log 作为"树真的长出来"的标志。
    //
    // mcdebug 协议上 pos/from/to 全是 JSON 数组 `[x, y, z]`，不是对象。
    // TS 类型签名虽是 Pos 对象，但实际值就是数组（`pos()` 在 types.js 里
    // 用 Object.assign 在数组上挂 helper），所以下面直接用数组。
    const floorY = origin[1];
    let treeGrew = false;
    for (let i = 0; i < 20; i++) {
      await boneMeal(ctx);
      await waitTicks(ctx, 1);
      const ob = await ctx.api.world.getBlock(origin);
      if (ob.state.name === "ic2_120:rubber_log") {
        treeGrew = true;
        break;
      }
    }
    assert.equal(
      treeGrew,
      true,
      "rubber sapling failed to grow (origin still rubber_sapling) after 20 bone meal applications"
    );

    // 4) **核心断言**：4 块周围原木必须全部还在。
    for (let i = 0; i < surroundings.length; i++) {
      const s = surroundings[i]!;
      const b = await ctx.api.world.getBlock(s);
      assert.equal(
        b.state.name,
        "ic2_120:rubber_log",
        `surrounding log #${i} at (${s[0]},${s[1]},${s[2]}) was removed (state=${b.state.name}); ` +
          `rubber tree bug regression: bone-meal-on-sapling should not touch neighboring blocks`
      );
    }

    // 5) 顺带验证树苗位置已经被替换为树干的第 0 格，
    //    防止出现"周围保留 + 树完全没长"的退化情况。
    const originBlock = await ctx.api.world.getBlock(origin);
    assert.equal(
      originBlock.state.name,
      "ic2_120:rubber_log",
      `origin block should be replaced by trunk after grow, got ${originBlock.state.name}`
    );

    // 6) y=floorY 层的 rubber_log 总数应当 ≥ 4（4 块周围 + 1 块树干底）。
    const yFloorFrom = [origin[0] - 1, floorY, origin[2] - 1];
    const yFloorTo = [origin[0] + 1, floorY, origin[2] + 1];
    const yFloorCount = await ctx.api.scan.countByBlock({ from: yFloorFrom, to: yFloorTo });
    assert.ok(
      (yFloorCount.counts["ic2_120:rubber_log"] ?? 0) >= 4,
      `expected at least 4 logs at y=${floorY} after grow, got ${yFloorCount.counts["ic2_120:rubber_log"] ?? 0} (surroundings were removed)`
    );
  }),
]);
