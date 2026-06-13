// 给各机器测试文件复用的通用 helper。
//
// 保持本文件精简：各机器独有的槽位布局、升级位置、热量阈值等应该写在对应
// 机器自己的 `*.test.ts` 中；这里只放真正跨机器共用的部分。
import { place, setBeField, setBlocks, type TestContext } from "@yu1745/mcdebug";

export type { TestContext };

/** 待测机器 origin 的东侧一格 — `setupAdjacentBatbox` 默认放置 BatBox 的位置。 */
export const BATBOX_EAST = (ctx: TestContext) => ctx.origin.east();

/**
 * 搭建最常见的“被测机器 + BatBox 供电”布局：
 *   1. 在 origin 东侧一格放一个面朝西的 BatBox（LV 输出侧贴向机器）；
 *   2. 预充 40 000 EU，足够所有调用本 helper 的机器跑完一次完整配方；
 *   3. 在 origin 放置目标机器。
 *
 * 机器 id 与 BatBox 位置都可参数化：若被测机器的能量接口不在西面，可传入
 * 自定义坐标并自行调整 `facing`。
 */
export async function setupAdjacentBatbox(ctx: TestContext, machine: string, batbox = BATBOX_EAST(ctx)): Promise<void> {
  await setBlocks(ctx, [{ pos: batbox, block: 'ic2_120:batbox', props: { facing: 'west' } }]);
  await setBeField(ctx, batbox, 'EnergyStored', 40000);
  await place(ctx, ctx.origin, machine);
}

/**
 * 把 32 位热量值写入 BE 拆分的高低 16 位字段。
 *
 * 热量系统用两个 16 位字段 (`Heat_Low` / `Heat_High`) 拼成 32 位累加器，
 * 是为了把 BE 序列化出来的 NBT 体积拆小。需要把热量预设到阈值以上的用例
 * 调用本方法一次后，调用方一般会再加 +200，保证阈值安全越过。
 */
export async function setHeat(ctx: TestContext, value: number): Promise<void> {
  await setBeField(ctx, ctx.origin, 'Heat_Low', value & 0xffff);
  await setBeField(ctx, ctx.origin, 'Heat_High', (value >>> 16) & 0xffff);
}
