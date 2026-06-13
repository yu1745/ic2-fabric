// 方块切割机 (Block Cutter) 测试。
//
// 槽位布局（与 `BlockCutterBlockEntity` 保持一致）：
//   slot 0  = 锯片 (SLOT_BLADE)            — 必须放 `iron_block_cutting_blade` 才能开工
//   slot 1  = 输入 (SLOT_INPUT)            — 待切的方块
//   slot 2  = 放电槽 (SLOT_DISCHARGING)    — 备用电池
//   slot 3  = 输出 (SLOT_OUTPUT)           — 切出来的 9 个对应板
//   slot 4+ = 升级槽 (SLOT_UPGRADE_0..3)   — 这里只用第一个放超频升级
import {
  assertBlockId,
  assertSlotCount,
  assertSlotEmpty,
  assertSlotHas,
  defineTest,
  defineTests,
  insertItem,
  invItemEquals,
  place,
  setBeField,
  setBlocks,
  setSlot,
  waitTicks,
  waitUntil,
} from "@yu1745/mcdebug";
import { BATBOX_EAST, setupAdjacentBatbox, type TestContext } from "./helpers.js";

/** 标准搭建：相邻 BatBox 供电 + 锯片 + 一个超频升级。 */
async function setupBlockCutter(ctx: TestContext): Promise<void> {
  await setupAdjacentBatbox(ctx, 'ic2_120:block_cutter');
  await insertItem(ctx, ctx.origin, 'ic2_120:iron_block_cutting_blade', 1, 0);
  await insertItem(ctx, ctx.origin, 'ic2_120:overclocker_upgrade', 2, 4);
}

export const blockCutterTests = defineTests([
  // 基础放置：确认方块实体正确放置。
  defineTest('block_cutter:place', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:block_cutter');
    await assertBlockId(ctx, ctx.origin, 'ic2_120:block_cutter');
  }),

  // 经典配方：1 个石头 → 9 个石板；带超频应在 15s 内完成。
  defineTest('block_cutter:stone to 9 stone_slab', async (ctx) => {
    await setupBlockCutter(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:stone', 1, 1);
    await waitUntil(ctx, invItemEquals(ctx.origin, 3, 'minecraft:stone_slab'), 15 * 20);
    await assertSlotCount(ctx, ctx.origin, 3, 9);
  }),

  // 无电闲置：没有 BatBox 供电时，原材料应保持在输入槽，不应被消耗。
  defineTest('block_cutter:no_power:idle', async (ctx) => {
    await place(ctx, ctx.origin, 'ic2_120:block_cutter');
    await insertItem(ctx, ctx.origin, 'ic2_120:iron_block_cutting_blade', 1, 0);
    await insertItem(ctx, ctx.origin, 'minecraft:stone', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:stone');
    await assertSlotEmpty(ctx, ctx.origin, 3);
  }),

  // 无锯片闲置：有电但没装锯片时，机器不能工作。
  defineTest('block_cutter:no_blade:idle', async (ctx) => {
    // 手动放 BatBox 而不复用 helper，因为这里不放机器升级。
    const batbox = BATBOX_EAST(ctx);
    await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
    await setBeField(ctx, batbox, 'EnergyStored', 40000);
    await place(ctx, ctx.origin, 'ic2_120:block_cutter');
    await insertItem(ctx, ctx.origin, 'minecraft:stone', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:stone');
    await assertSlotEmpty(ctx, ctx.origin, 3);
  }),

  // 非法输入：泥土没有对应配方，应原样保留在输入槽。
  defineTest('block_cutter:invalid_input:dirt', async (ctx) => {
    await setupBlockCutter(ctx);
    await insertItem(ctx, ctx.origin, 'minecraft:dirt', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:dirt');
    await assertSlotEmpty(ctx, ctx.origin, 3);
  }),

  // 输出满阻塞：输出槽先塞满石板，新一轮切割应卡住不消耗输入。
  defineTest('block_cutter:output_full:blocks_next', async (ctx) => {
    await setupBlockCutter(ctx);
    await setSlot(ctx, ctx.origin, 3, 'minecraft:stone_slab', 64);
    await insertItem(ctx, ctx.origin, 'minecraft:stone', 1, 1);
    await waitTicks(ctx, 200);
    await assertSlotHas(ctx, ctx.origin, 1, 'minecraft:stone');
  }),
]);
