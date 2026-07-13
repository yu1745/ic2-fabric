import assert from "node:assert/strict";
import {
  blockId,
  defineTest,
  defineTests,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

const STEAM = "ic2_120:steam";
const SUPERHEATED_STEAM = "ic2_120:superheated_steam";

function steamArea(ctx: TestContext) {
  return {
    from: ctx.origin.offset(-8, -2, -8),
    to: ctx.origin.offset(8, 9, 8),
  };
}

async function clearSteamArea(ctx: TestContext): Promise<void> {
  await ctx.api.world.clearBox(steamArea(ctx), { maxBlocks: 4096 });
}

async function setSteam(
  ctx: TestContext,
  pos: ReturnType<TestContext["pos"]>,
  block = STEAM,
  age = 0,
): Promise<void> {
  await ctx.api.world.setBlock(pos, block, {
    name: block,
    props: { level: "0", age: String(age) },
  });
}

export const steamWorldTests = defineTests([
  defineTest("steam_world:rises_and_spreads_after_replacing_non_air", async (ctx) => {
    await clearSteamArea(ctx);
    try {
      await ctx.api.world.setBlock(ctx.origin.down(), "minecraft:dirt");
      await ctx.api.world.setBlock(ctx.origin, "minecraft:tall_grass");
      await setSteam(ctx, ctx.origin);

      // t=5 生成第一层，t=10 后第一层必须继续流动，避免 flowing tick 类型回归。
      await waitUntil(ctx, blockId(ctx.origin.up(2), STEAM), 40);

      assert.equal((await ctx.api.world.getBlock(ctx.origin)).state.name, STEAM);
      assert.equal((await ctx.api.world.getBlock(ctx.origin.up())).state.name, STEAM);
      assert.equal((await ctx.api.world.getBlock(ctx.origin.up(2))).state.name, STEAM);
      assert.equal((await ctx.api.world.getBlock(ctx.origin.east())).state.name, STEAM);
      assert.equal((await ctx.api.world.getBlock(ctx.origin.down())).state.name, "minecraft:dirt");

      const flowing = await ctx.api.world.getBlock(ctx.origin.up());
      assert.notEqual(flowing.state.props.level, "0", "上方蒸汽必须是有距离衰减的流动状态");
    } finally {
      await clearSteamArea(ctx);
    }
  }),

  defineTest("steam_world:source_expiry_does_not_delete_independent_source", async (ctx) => {
    await clearSteamArea(ctx);
    try {
      await setSteam(ctx, ctx.origin, STEAM, 39);
      await setSteam(ctx, ctx.origin.up(), STEAM, 0);

      // 预期下一个 5-tick 方块调度即完成；并发套件负载高时给调度器留出余量。
      await waitUntil(ctx, blockId(ctx.origin, "minecraft:air"), 60);

      assert.equal((await ctx.api.world.getBlock(ctx.origin)).state.name, "minecraft:air");
      assert.equal((await ctx.api.world.getBlock(ctx.origin.up())).state.name, STEAM);
    } finally {
      await clearSteamArea(ctx);
    }
  }),

  defineTest("steam_world:superheated_cools_locally", async (ctx) => {
    await clearSteamArea(ctx);
    try {
      await setSteam(ctx, ctx.origin, SUPERHEATED_STEAM, 39);
      await setSteam(ctx, ctx.origin.up(), SUPERHEATED_STEAM, 0);

      // 预期下一个 5-tick 方块调度即完成；并发套件负载高时给调度器留出余量。
      await waitUntil(ctx, blockId(ctx.origin, STEAM), 60);

      assert.equal((await ctx.api.world.getBlock(ctx.origin)).state.name, STEAM);
      assert.equal((await ctx.api.world.getBlock(ctx.origin.up())).state.name, SUPERHEATED_STEAM);
    } finally {
      await clearSteamArea(ctx);
    }
  }),

  defineTest("steam_world:ordinary_steam_disappears_after_ten_seconds", async (ctx) => {
    await clearSteamArea(ctx);
    try {
      // 封住所有传播方向，只验证源块本身的 200 tick 生命周期。
      for (const pos of [ctx.origin.up(), ctx.origin.north(), ctx.origin.south(), ctx.origin.west(), ctx.origin.east()]) {
        await ctx.api.world.setBlock(pos, "minecraft:stone");
      }
      await setSteam(ctx, ctx.origin);

      await waitTicks(ctx, 195);
      assert.equal((await ctx.api.world.getBlock(ctx.origin)).state.name, STEAM);

      await waitUntil(ctx, blockId(ctx.origin, "minecraft:air"), 10);
      assert.equal((await ctx.api.world.getBlock(ctx.origin)).state.name, "minecraft:air");
    } finally {
      await clearSteamArea(ctx);
    }
  }),
]);
