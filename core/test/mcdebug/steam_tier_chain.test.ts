// 蒸汽梯次利用链条测试。
//
// 验证 IC2 蒸汽系统的完整梯次链：
//   锅炉(过热蒸汽) → 轮机#1(4KU/mB) → 降级普通蒸汽 → 轮机#2(2KU/mB) → 冷凝机 → 蒸馏水 → 泵附件+管道回流锅炉
//
// 物理拓扑（沿 origin 向东 X+，回流管道走 z+1 隔离带）：
//
//   创造发电机[west2]      红石块[up]      创造发电机[east3.north]
//        │EU                    │               │EU
//        ▼                      ▼               ▼
//   电加热机[west]─HU→ 锅炉[origin] → 轮机#1[east1] → 轮机#2[east2] → 冷凝机[east3]
//                          ↑回流                                   │ 泵附件正面抽
//                          │                              泵附件[east3.south]
//                          │ z+1 隔离带(中间留空不碰轮机)
//                          └── 管道 ←─ 末端管[origin.south] ←─ ... ←─ 泵附件[south]
//
//   每台轮机北面接动能发电机(facing=south)，两个发电机 EU 并联到同一 MFSU（导线中继）。
//
// 取巧（省升温时间）：
//   锅炉 SystemHeatMilli 直接 setField 到 375000 (375°C)，跳过电加热机数分钟升温。
//   电加热机仍然放好并供电，保证稳态时有持续 HU 输入维持温度。
import {
  defineTest,
  defineTests,
  place,
  setBeField,
  getBeNumber,
  setSlot,
  setBlocks,
  waitTicks,
  type TestContext,
} from "@yu1745/mcdebug";

/** origin 方向偏移的语义化别名，提高可读性。 */
function layout(ctx: TestContext) {
  const o = ctx.origin;
  return {
    boiler: o,                         // [0,0,0] 锅炉
    turbine1: o.east(),                // [1,0,0] 轮机#1 (吃过热)
    turbine2: o.east(2),               // [2,0,0] 轮机#2 (吃普通, 梯次第二级)
    condenser: o.east(3),              // [3,0,0] 冷凝机
    // 两个电加热机各 100HU/t，共 200HU/t 维持过热稳态（产汽冷却 200HU/t）
    heatGen1: o.west(),                // [-1,0,0] 电加热机#1 (facing=east→锅炉)
    heatGen1Power: o.west(2),          // [-2,0,0] 创造发电机 (供电加热机#1)
    heatGen1Redstone: o.west().up(),   // [-1,1,0] 红石块 (电加热机#1 上方)
    heatGen2: o.north(),               // [0,0,-1] 电加热机#2 (facing=south→锅炉)
    heatGen2Power: o.north(2).west(),    // [-1,0,-2] 创造发电机 (供电加热机#2，远离 MFSU 避免漏电)
    heatGen2Redstone: o.north().up(),  // [0,1,-1] 红石块 (电加热机#2 上方)
    condenserPower: o.east(3).north(), // [3,0,-1] 创造发电机 (供冷凝机)
    kineticGen1: o.east().north(),     // [1,0,-1] 动能发电机#1 (facing=south→轮机#1)
    kineticGen2: o.east(2).north(),    // [2,0,-1] 动能发电机#2
    mfsu: o.east().north(2),           // [1,0,-2] MFSU (收动能发电机#1)
    cable: o.east(2).north(2),         // [2,0,-2] 玻纤导线 (发电机#2→MFSU)
    pumpAtt: o.east(3).south(),        // [3,0,1] 泵附件 (facing=north→冷凝机，抽冷凝机蒸馏水)
    pumpAtt2: o.east(2).south(),       // [2,0,1] 泵附件#2 (facing=north→轮机#2，抽其自冷凝蒸馏水)
    pipe0: o.east(3).south(2),         // [3,0,2] 回流管道
    pipe1: o.east(2).south(2),
    pipe2: o.east().south(2),
    pipe3: o.south(2),                 // [0,0,2]
    endPipe: o.south(),                // [0,0,1] 末端管 (贴锅炉南面)
  };
}

/**
 * 搭建完整梯次链。所有方块一次性放好，参数用 setField 预设。
 * 取巧：锅炉温度直接拉到 400°C（过热阈值 374°C），不等电加热机缓慢升温。
 * 双电加热机各 100HU/t = 200HU/t，恰好平衡产汽冷却，维持过热稳态。
 */
async function setupTierChain(ctx: TestContext): Promise<void> {
  const p = layout(ctx);

  // 1. 热源侧：两个电加热机各 100HU/t = 200HU/t，创造发电机供电 + 红石块激活
  //    heatGen1 facing=east (西面→锅炉), heatGen2 facing=south (北面→锅炉)
  //    用 runCommand setblock 确保 facing（placeAsPlayer facing 映射不可靠）
  await setBlocks(ctx, [
    { pos: p.heatGen1Power, block: 'ic2_120:creative_generator' },
    { pos: p.heatGen1Redstone, block: 'minecraft:redstone_block' },
    { pos: p.heatGen2Power, block: 'ic2_120:creative_generator' },
    { pos: p.heatGen2Redstone, block: 'minecraft:redstone_block' },
  ]);
  await ctx.api.server.runCommand(`setblock ${p.heatGen1[0]} ${p.heatGen1[1]} ${p.heatGen1[2]} ic2_120:electric_heat_generator[facing=east]`);
  await ctx.api.server.runCommand(`setblock ${p.heatGen2[0]} ${p.heatGen2[1]} ${p.heatGen2[2]} ic2_120:electric_heat_generator[facing=south]`);
  // 两个电加热机各 10 线圈 + 预充 EU
  for (const hg of [p.heatGen1, p.heatGen2]) {
    for (let s = 0; s <= 9; s++) {
      await setSlot(ctx, hg, s, 'ic2_120:coil', 1);
    }
    await setBeField(ctx, hg, 'EnergyStored', 10000);
  }

  // 2. 锅炉 (facing=west，电加热机#1 east 方向推热给它)
  await ctx.api.server.runCommand(`setblock ${p.boiler[0]} ${p.boiler[1]} ${p.boiler[2]} ic2_120:steam_generator[facing=west]`);
  // 取巧：温度直拉 400°C（过热阈值 374°C 留 26°C 余量），双电加热机 200HU/t 维持稳态
  await setBeField(ctx, p.boiler, 'SystemHeatMilli', 400_000);
  await setBeField(ctx, p.boiler, 'Pressure', 221);
  await setBeField(ctx, p.boiler, 'InputMB', 1);
  // 注满蒸馏水 (10 桶 = 810000 droplets)，用 setField 写嵌套 NBT 绕过空罐 insert 越界
  await setBeField(ctx, p.boiler, 'WaterTank.amount', 810_000);
  await setBeField(ctx, p.boiler, 'WaterTank.variant.fluid', 'ic2_120:distilled_water');

  // 3. 两台轮机 (facing 默认，流体 IO 全方向) + 涡轮
  await place(ctx, p.turbine1, 'ic2_120:steam_kinetic_generator');
  await place(ctx, p.turbine2, 'ic2_120:steam_kinetic_generator');
  await setSlot(ctx, p.turbine1, 0, 'ic2_120:steam_turbine', 1);
  await setSlot(ctx, p.turbine2, 0, 'ic2_120:steam_turbine', 1);

  // 4. 冷凝机 + 散热口(slot 0-3) + EU + 创造发电机持续供电
  await place(ctx, p.condenser, 'ic2_120:condenser');
  for (let s = 0; s <= 3; s++) {
    await setSlot(ctx, p.condenser, s, 'ic2_120:heat_vent', 1);
  }
  await setBeField(ctx, p.condenser, 'EnergyStored', 100_000);
  await setBlocks(ctx, [{ pos: p.condenserPower, block: 'ic2_120:creative_generator' }]);

  // 5. 动能→电能：每台轮机北面动能发电机(facing=south)，并联到一个 MFSU
  //    用 runCommand setblock 确保 facing=south（placeAsPlayer facing 映射不可靠）
  await ctx.api.server.runCommand(`setblock ${p.kineticGen1[0]} ${p.kineticGen1[1]} ${p.kineticGen1[2]} ic2_120:kinetic_generator[facing=south]`);
  await ctx.api.server.runCommand(`setblock ${p.kineticGen2[0]} ${p.kineticGen2[1]} ${p.kineticGen2[2]} ic2_120:kinetic_generator[facing=south]`);
  await setBlocks(ctx, [{ pos: p.mfsu, block: 'ic2_120:mfsu' }]);
  await ctx.api.server.runCommand(`setblock ${p.cable[0]} ${p.cable[1]} ${p.cable[2]} ic2_120:glass_fibre_cable`);

  // 6. 蒸馏水回流：两个泵附件(facing=north)各抽一个蒸馏水源 → 共用管道走 z+2 隔离带 → 末端管贴锅炉
  //    泵附件#1 抽冷凝机蒸馏水，泵附件#2 抽轮机#2 自冷凝蒸馏水（否则轮机#2 蒸馏水满罐阻塞）
  //    用 runCommand setblock 放置（defaultState facing=north，避免 placeAsPlayer facing 不可控）
  await ctx.api.server.runCommand(`setblock ${p.pumpAtt[0]} ${p.pumpAtt[1]} ${p.pumpAtt[2]} ic2_120:bronze_pump_attachment`);
  await ctx.api.server.runCommand(`setblock ${p.pumpAtt2[0]} ${p.pumpAtt2[1]} ${p.pumpAtt2[2]} ic2_120:bronze_pump_attachment`);
  // 管道用原版命令放置，确保连接状态正确更新
  for (const pipe of [p.pipe0, p.pipe1, p.pipe2, p.pipe3, p.endPipe]) {
    await ctx.api.server.runCommand(`setblock ${pipe[0]} ${pipe[1]} ${pipe[2]} ic2_120:bronze_pipe_tiny`);
  }

  // 等链条启动：电加热机推热→锅炉产蒸汽→轮机产KU→发电机充MFSU。
  // 用固定等待而非 waitUntil：避免谓词捕不到瞬态值。
  await waitTicks(ctx, 160); // 8 秒，足够电加热机启动+蒸汽传递+冷凝
}

export const steamTierChainTests = defineTests([

  // 完整梯次链：过热蒸汽 → 轮机#1(4KU) → 普通蒸汽 → 轮机#2(2KU) → 冷凝 → 回流
  defineTest('steam_tier_chain:full_chain', async (ctx) => {
    const p = layout(ctx);
    await setupTierChain(ctx);

    // 验证 1：锅炉在过热区产过热蒸汽 (SystemHeatMilli >= 374000)
    const temp = await getBeNumber(ctx, p.boiler, 'SystemHeatMilli');
    if (temp < 374_000) {
      throw new Error(`锅炉未达过热区: SystemHeatMilli=${temp}, 需要 >= 374000`);
    }

    // 验证 2：梯次链通畅 + KU→EU 转换 + EU 速率 ≈ 150 EU/t。
    // 不直接读轮机 KuBuffer（会被动能发电机每 tick 抽走归 0，捕不到 >0 瞬间），
    // 改为验证 MFSU 电量上升速率。
    // 理论：100mB 蒸汽/t × (4+2) KU/mB ÷ 4 KU/EU = 150 EU/t
    const eu0 = await getBeNumber(ctx, p.mfsu, 'EnergyStored');
    await waitTicks(ctx, 100); // 5 秒采样窗口
    const eu1 = await getBeNumber(ctx, p.mfsu, 'EnergyStored');
    const euRate = (eu1 - eu0) / 100; // EU/t
    if (eu1 <= eu0) {
      // 诊断：输出各环节状态帮助定位断点
      const gen1Eu = await getBeNumber(ctx, p.kineticGen1, 'EnergyStored');
      const gen2Eu = await getBeNumber(ctx, p.kineticGen2, 'EnergyStored');
      const boilerSteam = await getBeNumber(ctx, p.boiler, 'SteamTank.amount');
      const boilerTemp = await getBeNumber(ctx, p.boiler, 'SystemHeatMilli');
      const boilerWater = await getBeNumber(ctx, p.boiler, 'WaterTank.amount');
      const t1Steam = await getBeNumber(ctx, p.turbine1, 'SteamTank.amount');
      throw new Error(
        `MFSU EU 未上升 (before=${eu0}, after=${eu1}). ` +
        `诊断: gen1.eu=${gen1Eu}, gen2.eu=${gen2Eu}, ` +
        `boiler.temp=${boilerTemp}, boiler.steam=${boilerSteam}, boiler.water=${boilerWater}, ` +
        `turbine1.steam=${t1Steam}`
      );
    }
    // EU 速率应在 150 EU/t 附近（允许 ±30% 容差应对 tick 对齐和舍入）
    if (euRate < 105 || euRate > 195) {
      throw new Error(
        `EU 速率异常: ${euRate.toFixed(1)} EU/t，期望 ~150 EU/t (105~195). ` +
        `eu0=${eu0}, eu1=${eu1}`
      );
    }

    // 验证 3：冷凝机在处理蒸汽（进度增长），证明梯次第二级轮机#2→冷凝机通畅
    const progress = await getBeNumber(ctx, p.condenser, 'Progress');
    if (progress <= 0) {
      throw new Error(`冷凝机进度为 0，轮机#2→冷凝机可能不通畅`);
    }
  }),

  // 蒸馏水闭环无损耗：稳态运行后锅炉水位基本不降
  defineTest('steam_tier_chain:distilled_water_loop', async (ctx) => {
    const p = layout(ctx);
    await setupTierChain(ctx);

    // 等链条进入稳态（蒸汽传递 + 回流管道填充）
    await waitTicks(ctx, 200); // 10 秒

    // 采样水量，等待 20 秒（400 tick），对比变化
    const waterBefore = await getBeNumber(ctx, p.boiler, 'WaterTank.amount');
    await waitTicks(ctx, 400);
    const waterAfter = await getBeNumber(ctx, p.boiler, 'WaterTank.amount');
    const loss = waterBefore - waterAfter;

    // 允许微小损耗（冷凝进度整数舍入），但不应超过总水量的 2%
    const maxAllowedLoss = Math.floor(waterBefore * 0.02);
    if (loss > maxAllowedLoss) {
      throw new Error(
        `蒸馏水损耗过大: before=${waterBefore}, after=${waterAfter}, loss=${loss} droplets, ` +
        `允许最大 ${maxAllowedLoss}。可能存在蒸汽积压爆炸或回流堵塞。`
      );
    }
  }),

]);
