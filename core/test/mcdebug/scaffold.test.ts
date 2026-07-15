import assert from "node:assert/strict";
import { defineTest, defineTests, setBlocks, waitTicks } from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

async function blockId(ctx: TestContext, pos: TestContext["origin"]): Promise<string> {
  return (await ctx.api.world.getBlock(pos)).state.name;
}

export const scaffoldTests = defineTests([
  defineTest("scaffold: removing the grounded base collapses the vertical column", async (ctx) => {
    const ground = ctx.origin;
    const bottom = ground.up();
    const middle = bottom.up();
    const top = middle.up();

    await setBlocks(ctx, [
      { pos: ground, block: "minecraft:stone" },
      { pos: bottom, block: "ic2_120:wooden_scaffold" },
      { pos: middle, block: "ic2_120:wooden_scaffold" },
      { pos: top, block: "ic2_120:wooden_scaffold" },
    ]);
    await waitTicks(ctx, 3);

    assert.equal(await blockId(ctx, bottom), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, middle), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, top), "ic2_120:wooden_scaffold");

    await ctx.api.world.setBlock(bottom, "minecraft:air");
    await waitTicks(ctx, 3);

    assert.equal(await blockId(ctx, middle), "minecraft:air");
    assert.equal(await blockId(ctx, top), "minecraft:air");
  }),

  defineTest("scaffold: wood caps support inherited from iron", async (ctx) => {
    const ground = ctx.origin;
    const iron = ground.up();
    const woodRelay = iron.up();
    const wood1 = woodRelay.east();
    const wood2 = wood1.east();
    const wood3 = wood2.east();

    await setBlocks(ctx, [
      { pos: ground, block: "minecraft:stone" },
      { pos: iron, block: "ic2_120:iron_scaffold" },
      { pos: woodRelay, block: "ic2_120:wooden_scaffold" },
      { pos: wood1, block: "ic2_120:wooden_scaffold" },
      { pos: wood2, block: "ic2_120:wooden_scaffold" },
      { pos: wood3, block: "ic2_120:wooden_scaffold" },
    ]);
    await waitTicks(ctx, 3);

    assert.equal(await blockId(ctx, woodRelay), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, wood1), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, wood2), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, wood3), "minecraft:air");
  }),

  defineTest("scaffold: iron supports five horizontal extensions", async (ctx) => {
    const ground = ctx.origin;
    const anchor = ground.up();
    const extensions = Array.from({ length: 6 }, (_, index) => anchor.east(index + 1));

    await setBlocks(ctx, [
      { pos: ground, block: "minecraft:stone" },
      { pos: anchor, block: "ic2_120:iron_scaffold" },
      ...extensions.map((pos) => ({ pos, block: "ic2_120:iron_scaffold" })),
    ]);
    await waitTicks(ctx, 3);

    for (const pos of extensions.slice(0, 5)) {
      assert.equal(await blockId(ctx, pos), "ic2_120:iron_scaffold");
    }
    assert.equal(await blockId(ctx, extensions[5]), "minecraft:air");
  }),

  defineTest("scaffold: raising the exhausted end does not reset horizontal support", async (ctx) => {
    const ground = ctx.origin;
    const anchor = ground.up();
    const first = anchor.east();
    const limit = first.east();
    const raised = limit.up();
    const beyondLimit = raised.east();

    await setBlocks(ctx, [
      { pos: ground, block: "minecraft:stone" },
      { pos: anchor, block: "ic2_120:wooden_scaffold" },
      { pos: first, block: "ic2_120:wooden_scaffold" },
      { pos: limit, block: "ic2_120:wooden_scaffold" },
      { pos: raised, block: "ic2_120:wooden_scaffold" },
      { pos: beyondLimit, block: "ic2_120:wooden_scaffold" },
    ]);
    await waitTicks(ctx, 5);

    assert.equal(await blockId(ctx, limit), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, raised), "ic2_120:wooden_scaffold");
    assert.equal(await blockId(ctx, beyondLimit), "minecraft:air");
  }),
]);
