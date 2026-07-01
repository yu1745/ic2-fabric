// 全部数值直接移植自 core/src/main/kotlin/ic2_120/content/sync/NuclearReactorSync.kt
// 及 NuclearReactorBlockEntity.kt 中的魔法数。改动这里之前先核对 Kotlin 源码。

export const REACTOR_TIER = 5;
/** 单次 cycle 产生的 output 单位 × 100 = EU */
export const EU_PER_OUTPUT = 100;
/** 能量缓存上限（EU） */
export const ENERGY_CAPACITY = 1_000_000;
/** 最大输出 EU/t（tier 5） */
export const MAX_OUTPUT_PER_TICK = 8192;

/** 基础反应堆槽位数（无腔室）= 3 列 × 9 行 */
export const BASE_SLOTS = 27;
/** 每个相邻反应堆腔室增加的槽位数 = 1 列 × 9 行 */
export const SLOTS_PER_CHAMBER = 9;
/** 反应堆最大槽位数 = (3 + 6) 列 × 9 行 = 81 */
export const MAX_SLOTS = 81;
/** 行数固定为 9 */
export const ROWS = 9;
/** 最大腔室数 */
export const MAX_CHAMBERS = 6;

/** 基础堆温上限（不含隔板加成） */
export const HEAT_CAPACITY = 10_000;

// 堆温阈值（NuclearReactorSync）
export const HEAT_FIRE_THRESHOLD = 4_000;
export const HEAT_EVAPORATE_THRESHOLD = 5_000;
export const HEAT_DAMAGE_THRESHOLD = 7_000;
export const HEAT_LAVA_THRESHOLD = 8_500;
export const HEAT_EXPLODE_THRESHOLD = 10_000;

/** 一个 reactor cycle = 20 游戏 tick */
export const TICKS_PER_CYCLE = 20;

/** 流体模式：1 桶冷却液可转换的热量（HU） */
export const HU_PER_BUCKET = 20_000;

// ===== 隔板加成（NuclearReactorBlockEntity.getMaxHeat 读取） =====
export const PLATING_HEAT_BONUS = 500;
export const HEAT_PLATING_HEAT_BONUS = 1_700;

// ===== 元件参数（按 Kotlin 类构造函数直接抄） =====

// 散热片：[heatStorage, selfVent, reactorVent]
export const VENT_PARAMS = {
  heat_vent: { heatStorage: 1000, selfVent: 6, reactorVent: 0 },
  reactor_heat_vent: { heatStorage: 1000, selfVent: 5, reactorVent: 5 },
  advanced_heat_vent: { heatStorage: 1000, selfVent: 12, reactorVent: 0 },
  overclocked_heat_vent: { heatStorage: 1000, selfVent: 20, reactorVent: 36 },
} as const;

// component_heat_vent 不储热，向 4 邻各蒸发 sideVent
export const COMPONENT_VENT_SIDE_VENT = 4;

// 热交换器：[heatStorage, switchSide, switchReactor]
export const EXCHANGER_PARAMS = {
  heat_exchanger: { heatStorage: 2500, switchSide: 12, switchReactor: 4 },
  reactor_heat_exchanger: { heatStorage: 5000, switchSide: 0, switchReactor: 72 },
  component_heat_exchanger: { heatStorage: 5000, switchSide: 36, switchReactor: 0 },
  advanced_heat_exchanger: { heatStorage: 10000, switchSide: 24, switchReactor: 8 },
} as const;

// 冷却单元：heatStorage = maxUse
export const COOLANT_CELL_HEAT = {
  reactor_coolant_cell: 10_000,
  triple_reactor_coolant_cell: 30_000,
  sextuple_reactor_coolant_cell: 60_000,
} as const;

// 冷凝器：heatStorage = maxUse（只能正吸收）
export const CONDENSATOR_HEAT = {
  rsh_condensator: 20_000,
  lzh_condensator: 100_000,
} as const;

// 燃料棒：[maxUse, numberOfCells]
export const FUEL_ROD_PARAMS = {
  uranium_fuel_rod: { maxUse: 20_000, cells: 1, depleted: 'depleted_uranium_fuel_rod' },
  dual_uranium_fuel_rod: { maxUse: 20_000, cells: 2, depleted: 'depleted_dual_uranium_fuel_rod' },
  quad_uranium_fuel_rod: { maxUse: 20_000, cells: 4, depleted: 'depleted_quad_uranium_fuel_rod' },
  mox_fuel_rod: { maxUse: 10_000, cells: 1, depleted: 'depleted_mox_fuel_rod' },
  dual_mox_fuel_rod: { maxUse: 10_000, cells: 2, depleted: 'depleted_dual_mox_fuel_rod' },
  quad_mox_fuel_rod: { maxUse: 10_000, cells: 4, depleted: 'depleted_quad_mox_fuel_rod' },
} as const;

// 反射板：maxPulses
export const REFLECTOR_PARAMS = {
  neutron_reflector: { maxPulses: 30_000 },
  thick_neutron_reflector: { maxPulses: 120_000 },
  // iridium_neutron_reflector 永不枯竭，无耐久
} as const;

// 隔板爆炸倍率（processChamber heatRun 时乘到 heatEffectModifier）
export const PLATING_EXPLOSION_MODIFIER = {
  reactor_plating: 0.9,
  reactor_heat_plating: 0.99,
} as const;
