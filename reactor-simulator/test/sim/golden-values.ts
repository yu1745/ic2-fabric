// 从游戏内 mcdebug 测试提取的 golden values（core/test/mcdebug/reactor_golden.test.ts）。
//
// 提取方式：放反应堆 + 相邻红石块 → 填槽 → waitUntil(EnergyStored>0) + waitTicks(20) → 读
//   HeatStored（堆温，0..10000）、EnergyStored（EU 缓冲）、各槽 NBT use。
//
// 关键说明：
//  - 测量时燃料棒 use=2，表示已跑过 2 个完整 cycle（pass0 耐久 +1 每周期）。
//  - **heat 字段是 2 个 cycle 累计后的瞬时堆温**。对于净产热恒定的布局，
//    heat_per_cycle ≈ heat / 2（但受散热片/冷却单元吸热影响，需结合 slots.use 综合判断）。
//  - energy 是 EU 缓冲瞬时值，受 tickOffset（随机 0..19）与放出节奏（pendingEnergyOutput/20 每 tick）
//    影响，波动较大，仅作参考量级核对，不严格对拍。
//  - 网格索引 slot = x*9+y；x=列(0..2 基础), y=行(0..8)。
//
// 这些值是 simulator 移植正确性的「真相来源」。

export interface GoldenCase {
  label: string;
  /** [slotIndex, itemId without ic2_120: prefix] */
  layout: Array<[number, string]>;
  /** 游戏读取的瞬时堆温（2 cycle 后） */
  heat: number;
  /** 游戏读取的瞬时 EU 缓冲（仅量级参考） */
  energy: number;
  /** 游戏读取的各槽 use（2 cycle 后） */
  slotUses: Record<string, { item: string; use: number }>;
  /** cycles 已运行（= 燃料棒 use 值） */
  cycles: number;
}

export const GOLDEN: GoldenCase[] = [
  {
    label: 'empty_sanity',
    layout: [],
    heat: 0, energy: 0, cycles: 0,
    slotUses: {},
  },
  {
    label: 'single_uranium_rod_slot0',
    layout: [[0, 'uranium_fuel_rod']],
    heat: 8, energy: 105, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 } },
  },
  {
    label: 'two_adjacent_uranium_rods',
    layout: [[0, 'uranium_fuel_rod'], [1, 'uranium_fuel_rod']],
    heat: 48, energy: 420, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 }, '1': { item: 'ic2_120:uranium_fuel_rod', use: 2 } },
  },
  {
    label: 'four_stacked_uranium_rods',
    layout: [[0, 'uranium_fuel_rod'], [1, 'uranium_fuel_rod'], [2, 'uranium_fuel_rod'], [3, 'uranium_fuel_rod']],
    heat: 144, energy: 1050, cycles: 2,
    slotUses: {
      '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 },
      '1': { item: 'ic2_120:uranium_fuel_rod', use: 2 },
      '2': { item: 'ic2_120:uranium_fuel_rod', use: 2 },
      '3': { item: 'ic2_120:uranium_fuel_rod', use: 2 },
    },
  },
  {
    label: 'rod_plus_vent',
    layout: [[0, 'uranium_fuel_rod'], [9, 'heat_vent']],
    heat: 0, energy: 105, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 }, '9': { item: 'ic2_120:heat_vent', use: 0 } },
  },
  {
    label: 'dual_uranium_rod_slot0',
    layout: [[0, 'dual_uranium_fuel_rod']],
    heat: 48, energy: 420, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:dual_uranium_fuel_rod', use: 2 } },
  },
  {
    label: 'quad_uranium_rod_slot0',
    layout: [[0, 'quad_uranium_fuel_rod']],
    heat: 192, energy: 1260, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:quad_uranium_fuel_rod', use: 2 } },
  },
  {
    label: 'rod_plus_reflector',
    layout: [[0, 'uranium_fuel_rod'], [9, 'neutron_reflector']],
    heat: 24, energy: 219, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 }, '9': { item: 'ic2_120:neutron_reflector', use: 2 } },
  },
  {
    label: 'rod_plus_coolant_cell',
    layout: [[0, 'uranium_fuel_rod'], [9, 'reactor_coolant_cell']],
    heat: 0, energy: 105, cycles: 2,
    slotUses: { '0': { item: 'ic2_120:uranium_fuel_rod', use: 2 }, '9': { item: 'ic2_120:reactor_coolant_cell', use: 8 } },
  },
];
