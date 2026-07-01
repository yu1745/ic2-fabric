import assert from "node:assert/strict";
import {
  blockTarget,
  defineTest,
  defineTests,
  itemTarget,
  openMachineScreen,
  place,
  setScreenPlayerSlot,
  setBlocks,
  type StorageGetResult,
  type Target,
} from "@yu1745/mcdebug";
import { type TestContext } from "./helpers.js";

const FABRIC_ITEM = "fabric:item";
const FABRIC_FLUID = "fabric:fluid";
const TEAMREBORN_ENERGY = "teamreborn:energy";
const VANILLA_INVENTORY = "vanilla:inventory";

function assertHandle(
  handles: Awaited<ReturnType<TestContext["api"]["storage"]["list"]>>["handles"],
  handle: string,
  kind: "item" | "fluid" | "energy",
): void {
  assert.ok(
    handles.some((entry) => entry.handle === handle && entry.kind === kind),
    `expected ${kind} storage handle ${handle}; got ${handles.map((entry) => `${entry.handle}:${entry.kind}`).join(", ")}`,
  );
}

function assertStorageKind<T extends StorageGetResult["kind"]>(
  storage: StorageGetResult,
  kind: T,
): Extract<StorageGetResult, { kind: T }> {
  assert.equal(storage.kind, kind);
  return storage as Extract<StorageGetResult, { kind: T }>;
}

async function placeMacerator(ctx: TestContext): Promise<Target> {
  await place(ctx, ctx.origin, "ic2_120:macerator");
  return blockTarget(ctx.origin);
}

export const mcdebugStorageApiTests = defineTests([
  defineTest("mcdebug_api:batbox energy adapter", async (ctx) => {
    await setBlocks(ctx, [{ pos: ctx.origin, block: "ic2_120:batbox", props: { facing: "west" } }]);
    const target = blockTarget(ctx.origin);

    const list = await ctx.api.storage.list(target);
    assertHandle(list.handles, TEAMREBORN_ENERGY, "energy");

    const before = assertStorageKind(await ctx.api.storage.get(target, TEAMREBORN_ENERGY), "energy");
    assert.ok(before.capacity > 0, `expected positive BatBox energy capacity, got ${before.capacity}`);

    const inserted = await ctx.api.storage.insert(target, TEAMREBORN_ENERGY, { kind: "energy" }, 32);
    assert.equal(inserted.inserted, 32);

    const afterInsert = assertStorageKind(await ctx.api.storage.get(target, TEAMREBORN_ENERGY), "energy");
    assert.equal(afterInsert.amount, before.amount + 32);

    const extracted = await ctx.api.storage.extract(target, TEAMREBORN_ENERGY, { kind: "energy" }, 16);
    assert.equal(extracted.extracted, 16);

    const afterExtract = assertStorageKind(await ctx.api.storage.get(target, TEAMREBORN_ENERGY), "energy");
    assert.equal(afterExtract.amount, afterInsert.amount - 16);
  }),

  defineTest("mcdebug_api:macerator item storage routes", async (ctx) => {
    const target = await placeMacerator(ctx);

    const list = await ctx.api.storage.list(target);
    assertHandle(list.handles, VANILLA_INVENTORY, "item");
    assertHandle(list.handles, FABRIC_ITEM, "item");

    const inserted = await ctx.api.storage.insert(target, FABRIC_ITEM, { kind: "item", item: "minecraft:cobblestone" }, 1);
    assert.equal(inserted.inserted, 1);

    const storage = assertStorageKind(await ctx.api.storage.get(target, FABRIC_ITEM), "item");
    const inputSlot = storage.slots.find((slot) => slot.index === 0);
    assert.equal(inputSlot?.stack.item, "minecraft:cobblestone");
    assert.equal(inputSlot?.stack.count, 1);

    const invalid = await ctx.api.storage.insert(
      target,
      FABRIC_ITEM,
      { kind: "item", item: "minecraft:dirt" },
      1,
      { simulate: true },
    );
    assert.equal(invalid.inserted, 0);
  }),

  defineTest("mcdebug_api:ic2 fluid cell item target", async (ctx) => {
    const target = itemTarget({ item: "ic2_120:water_cell", count: 1 });

    const list = await ctx.api.storage.list(target);
    assertHandle(list.handles, FABRIC_FLUID, "fluid");

    const storage = assertStorageKind(await ctx.api.storage.get(target, FABRIC_FLUID), "fluid");
    assert.ok(
      storage.tanks.some((tank) => tank.fluid === "minecraft:water" && tank.amount === 81_000),
      `expected water cell to expose 81000 droplets; got ${JSON.stringify(storage.tanks)}`,
    );

    const extracted = await ctx.api.storage.extract(target, FABRIC_FLUID, { kind: "fluid", fluid: "minecraft:water" }, 81_000);
    assert.equal(extracted.extracted, 81_000);
    assert.equal(extracted.targetAfter?.kind, "item");
    if (extracted.targetAfter?.kind === "item") {
      assert.equal(extracted.targetAfter.stack.item, "ic2_120:empty_cell");
      assert.equal(extracted.targetAfter.stack.count, 1);
    }
  }),

  defineTest("mcdebug_api:macerator screen handler", async (ctx) => {
    await placeMacerator(ctx);

    const opened = await openMachineScreen(ctx, ctx.origin);
    assert.equal(opened.handlerType, "ic2_120:macerator");
    assert.equal(opened.slots.length, 43);
    assert.ok(opened.properties.length > 0, "expected macerator PropertyDelegate values");

    const afterQuickMove = await ctx.api.screen.quickMove(opened.screenId, 0);
    assert.equal(afterQuickMove.screenId, opened.screenId);

    const closed = await ctx.api.screen.close(opened.screenId);
    assert.equal(closed.closed, true);
  }),

  defineTest("mcdebug_api:macerator screen quick-move routes player items", async (ctx) => {
    await placeMacerator(ctx);
    const opened = await openMachineScreen(ctx, ctx.origin);

    await setScreenPlayerSlot(ctx, opened.screenId, 0, { item: "minecraft:cobblestone", count: 1 });
    const afterInputMove = await ctx.api.screen.quickMove(opened.screenId, 34);
    assert.equal(afterInputMove.slots[0]?.item, "minecraft:cobblestone");
    assert.equal(afterInputMove.slots[34]?.item, null);

    await setScreenPlayerSlot(ctx, opened.screenId, 1, { item: "ic2_120:overclocker_upgrade", count: 1 });
    const afterUpgradeMove = await ctx.api.screen.quickMove(opened.screenId, 35);
    assert.equal(afterUpgradeMove.slots[3]?.item, "ic2_120:overclocker_upgrade");
    assert.equal(afterUpgradeMove.slots[35]?.item, null);

    await setScreenPlayerSlot(ctx, opened.screenId, 2, { item: "minecraft:dirt", count: 1 });
    const afterInvalidMove = await ctx.api.screen.quickMove(opened.screenId, 36);
    assert.equal(afterInvalidMove.slots[36]?.item, "minecraft:dirt");

    const closed = await ctx.api.screen.close(opened.screenId);
    assert.equal(closed.closed, true);
  }),

  defineTest("mcdebug_api:tank fluid storage and transfer", async (ctx) => {
    const fromPos = ctx.origin;
    const toPos = ctx.origin.east();
    await place(ctx, fromPos, "ic2_120:bronze_tank");
    await place(ctx, toPos, "ic2_120:bronze_tank");

    const from = blockTarget(fromPos);
    const to = blockTarget(toPos);
    const list = await ctx.api.storage.list(from);
    assertHandle(list.handles, FABRIC_FLUID, "fluid");

    const inserted = await ctx.api.storage.insert(from, FABRIC_FLUID, { kind: "fluid", fluid: "minecraft:water" }, 162_000);
    assert.equal(inserted.inserted, 162_000);

    const moved = await ctx.api.storage.transfer(from, to, { kind: "fluid", fluid: "minecraft:water" }, 81_000);
    assert.equal(moved.transferred, 81_000);

    const fromStorage = assertStorageKind(await ctx.api.storage.get(from, FABRIC_FLUID), "fluid");
    const toStorage = assertStorageKind(await ctx.api.storage.get(to, FABRIC_FLUID), "fluid");
    assert.equal(fromStorage.tanks[0]?.amount, 81_000);
    assert.equal(toStorage.tanks[0]?.amount, 81_000);

    const cellTransfer = await ctx.api.storage.transfer(
      itemTarget({ item: "ic2_120:water_cell", count: 1 }),
      to,
      { kind: "fluid", fluid: "minecraft:water" },
      81_000,
    );
    assert.equal(cellTransfer.transferred, 81_000);
    assert.equal(cellTransfer.fromAfter?.kind, "item");
    if (cellTransfer.fromAfter?.kind === "item") {
      assert.equal(cellTransfer.fromAfter.stack.item, "ic2_120:empty_cell");
    }

    const finalToStorage = assertStorageKind(await ctx.api.storage.get(to, FABRIC_FLUID), "fluid");
    assert.equal(finalToStorage.tanks[0]?.amount, 162_000);
  }),
]);
