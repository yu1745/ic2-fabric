import assert from "node:assert/strict";
import {
  defineTest,
  defineTests,
  setBlocks,
  waitTicks,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

function around(ctx: TestContext, radius = 2) {
  return {
    from: ctx.origin.offset(-radius, -radius, -radius),
    to: ctx.origin.offset(radius, radius, radius),
  };
}

export const mcdebugBasicApiTests = defineTests([
  defineTest("mcdebug_api:redstone lever power and pulse", async (ctx) => {
    const base = ctx.origin;
    const lever = ctx.origin.up();
    await setBlocks(ctx, [
      { pos: base, block: "minecraft:stone" },
      { pos: lever, block: "minecraft:lever", props: { face: "floor", facing: "north", powered: "false" } },
    ]);

    const on = await ctx.api.redstone.setLever(lever, true);
    assert.equal(on.powered, true);

    const powered = await ctx.api.redstone.isPowered(base);
    assert.equal(powered.powered, true);
    assert.equal(powered.received, 15);

    const power = await ctx.api.redstone.getPower(base, { side: "up" });
    assert.equal(power.sideInput, 15);

    const off = await ctx.api.redstone.setLever(lever, false);
    assert.equal(off.powered, false);
    assert.equal((await ctx.api.redstone.isPowered(base)).powered, false);

    await ctx.api.redstone.pulse(lever, 2);
    assert.equal((await ctx.api.redstone.isPowered(base)).powered, true);
    await waitTicks(ctx, 4);
    assert.equal((await ctx.api.redstone.isPowered(base)).powered, false);
  }),

  defineTest("mcdebug_api:entity item spawn teleport collect", async (ctx) => {
    // prepareArea 会把 origin 周围清成 air（含 origin 本身和其下方），item 实体
    // 在 origin.up() 生成后会因无支撑受重力下落、掉出 listItems 的扫描范围，
    // 甚至掉出加载区块被清除（曾表现为 0 !== 1 / 4 !== 1 的随机失败）。
    // 在 origin 放一块石头当地板，item 落在上面即停住。
    await setBlocks(ctx, [{ pos: ctx.origin, block: "minecraft:stone" }]);

    const spawned = await ctx.api.entity.spawn("minecraft:item", ctx.origin.up(), {
      stack: { item: "minecraft:diamond", count: 3 },
    });
    assert.equal(spawned.spawned, true);
    assert.equal(spawned.entity.stack?.item, "minecraft:diamond");
    assert.equal(spawned.entity.stack?.count, 3);

    const listed = await ctx.api.entity.listItems(around(ctx), { item: "minecraft:diamond" });
    assert.equal(listed.count, 1);
    assert.equal(listed.items[0]?.uuid, spawned.entity.uuid);

    // teleport 到 origin 东 2 格上方；同样需要地板，否则 item 会继续下落。
    const target = ctx.origin.east(2).up();
    await setBlocks(ctx, [{ pos: ctx.origin.east(2), block: "minecraft:stone" }]);
    const moved = await ctx.api.entity.teleport(spawned.entity.uuid, target);
    assert.equal(moved.teleported, true);

    const nbt = await ctx.api.entity.getNbt(spawned.entity.uuid);
    assert.equal(nbt.entity.uuid, spawned.entity.uuid);

    const collected = await ctx.api.entity.collectItems(
      { from: target.down(), to: target.up() },
      { item: "minecraft:diamond" },
    );
    assert.equal(collected.count, 1);
    assert.equal(collected.removed, true);
    assert.equal(collected.items[0]?.stack?.count, 3);

    const after = await ctx.api.entity.listItems(around(ctx), { item: "minecraft:diamond" });
    assert.equal(after.count, 0);
  }),

  defineTest("mcdebug_api:fixture capture load and world box edit", async (ctx) => {
    const chest = ctx.origin;
    const block = ctx.origin.east();
    await ctx.api.world.fillBox({ from: chest, to: block }, "minecraft:stone", undefined, { maxBlocks: 2 });
    await setBlocks(ctx, [{ pos: chest, block: "minecraft:chest" }]);
    await ctx.api.inv.setSlot(chest, 0, "minecraft:apple", 5);
    await ctx.api.world.setBlock(block, "minecraft:gold_block");

    const fixture = await ctx.api.fixture.capture({ from: chest, to: block });
    assert.equal(fixture.blocks.length, 2);

    const cleared = await ctx.api.world.clearBox({ from: chest, to: block }, { maxBlocks: 2 });
    assert.equal(cleared.count, 2);
    assert.equal((await ctx.api.world.getBlock(chest)).state.name, "minecraft:air");
    assert.equal((await ctx.api.world.getBlock(block)).state.name, "minecraft:air");

    const target = ctx.origin.south(4);
    const loaded = await ctx.api.fixture.load(fixture, { origin: target });
    assert.equal(loaded.placed, 2);
    assert.equal((await ctx.api.world.getBlock(target)).state.name, "minecraft:chest");
    assert.equal((await ctx.api.world.getBlock(target.east())).state.name, "minecraft:gold_block");

    const slot = await ctx.api.inv.getSlot(target, 0);
    assert.equal(slot.slot.item, "minecraft:apple");
    assert.equal(slot.slot.count, 5);
  }),
]);
