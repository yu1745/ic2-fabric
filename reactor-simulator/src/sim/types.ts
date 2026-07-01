// 移植自 core/src/main/kotlin/ic2_120/content/reactor/IReactorComponent.kt 等
// 类型层级与 Kotlin 严格对应：Slot ↔ ItemStack+nbt，IReactorComponent ↔ 接口。

/** 所有可放入反应堆槽位的元件 registry name */
export type ComponentId =
  // 燃料棒
  | 'uranium_fuel_rod' | 'dual_uranium_fuel_rod' | 'quad_uranium_fuel_rod'
  | 'mox_fuel_rod' | 'dual_mox_fuel_rod' | 'quad_mox_fuel_rod'
  // 枯竭燃料棒（无行为，仅占位/显示产物）
  | 'depleted_uranium_fuel_rod' | 'depleted_dual_uranium_fuel_rod' | 'depleted_quad_uranium_fuel_rod'
  | 'depleted_mox_fuel_rod' | 'depleted_dual_mox_fuel_rod' | 'depleted_quad_mox_fuel_rod'
  // 散热片
  | 'heat_vent' | 'reactor_heat_vent' | 'advanced_heat_vent' | 'overclocked_heat_vent' | 'component_heat_vent'
  // 热交换器
  | 'heat_exchanger' | 'reactor_heat_exchanger' | 'component_heat_exchanger' | 'advanced_heat_exchanger'
  // 冷却单元
  | 'reactor_coolant_cell' | 'triple_reactor_coolant_cell' | 'sextuple_reactor_coolant_cell'
  // 冷凝器
  | 'rsh_condensator' | 'lzh_condensator'
  // 隔板
  | 'reactor_plating' | 'reactor_heat_plating' | 'containment_reactor_plating'
  // 中子反射板
  | 'neutron_reflector' | 'thick_neutron_reflector' | 'iridium_neutron_reflector';

/** 槽位：null = 空。use 对应 Kotlin NBT "use"（燃料棒 = 已用 cycle 数；散热片/交换器/冷却单元/冷凝器 = 当前热量；反射板 = 已消耗脉冲数）。 */
export interface Slot {
  id: ComponentId;
  use: number;
}

/** 列优先网格：grid[x * ROWS + y]。x = 列（0..cols-1），y = 行（0..8） */
export type Grid = (Slot | null)[];

/** 反应堆运行模式 */
export type ReactorMode = 'electric' | 'fluid';

/**
 * IReactor：反应堆对元件暴露的接口（移植自 IReactor.kt）。
 * 元件通过它读写堆温、邻接槽位、累加发电/散热。
 */
export interface IReactor {
  // ===== 槽位访问（列优先） =====
  getItemAt(x: number, y: number): Slot | null;
  setItemAt(x: number, y: number, slot: Slot | null): void;

  // ===== 堆温 =====
  getHeat(): number;
  setHeat(h: number): void;
  addHeat(amount: number): void;
  getMaxHeat(): number;
  /** 散热片用的「有效堆温」= 当前堆温 + emitHeatBuffer，防止同周期多片重复扣同一份热 */
  getEffectiveHeatForDrain(): number;
  /** 把热量累加到 emitHeatBuffer（周期末合并到堆温） */
  addEmitHeat(heat: number): void;

  // ===== 发电 =====
  /** 是否产电（红石关停时为 false） */
  produceEnergy(): boolean;
  addOutput(units: number): void;

  // ===== 散热/产热统计（用于仪表盘与流体模式冷却液转换） =====
  addHeatProduced(heat: number): void;
  addHeatDissipated(heat: number): void;
  /** 记录某槽位本周期产热/散热/发电（仪表盘热力图用） */
  addSlotHeatInfo(slotIndex: number, heatProduced: number, heatDissipated: number, energyOutput?: number): void;

  // ===== 模式查询 =====
  isFluidCooled(): boolean;
  hasCoolant(): boolean;
  /** 周期开始时的堆温（MOX 流体模式双倍热量判定用） */
  getCycleStartHeat(): number;

  // ===== 爆炸影响 =====
  multiplyHeatEffectModifier(mod: number): void;
}

/**
 * IReactorComponent：每个反应堆内元件实现的接口（移植自 IReactorComponent.kt）。
 * 方法签名与 Kotlin 一一对应，便于逐方法核对。
 */
export interface IReactorComponent {
  /** 每周期被调用两次：heatRun=false 算发电/耐久，heatRun=true 算热量 */
  processChamber(slot: Slot, reactor: IReactor, x: number, y: number, heatRun: boolean): void;
  /** 是否接受来自邻位燃料棒的中子脉冲（返回 true 则该燃料棒 neighborPulses +1） */
  acceptUraniumPulse(slot: Slot, reactor?: IReactor | null, pulsingSlot?: Slot | null, youX?: number, youY?: number, pulseX?: number, pulseY?: number, heatRun?: boolean): boolean;
  canStoreHeat(slot?: Slot, reactor?: IReactor | null, x?: number, y?: number): boolean;
  getMaxHeat(slot?: Slot, reactor?: IReactor | null, x?: number, y?: number): number;
  getCurrentHeat(slot?: Slot, reactor?: IReactor | null, x?: number, y?: number): number;
  /** 增减热量，返回无法吸收（正溢出）/无法释放（负溢出）的部分 */
  alterHeat(slot: Slot, reactor: IReactor, x: number, y: number, heat: number): number;
  /** 爆炸威力影响：>0 && <1 乘到 boomMod，>=1 加到 boomPower */
  influenceExplosion(slot?: Slot, reactor?: IReactor | null): number;
}

/** 单周期结算结果（仪表盘展示） */
export interface CycleStats {
  heat: number;
  maxHeat: number;
  euPerTick: number;        // 电力模式：EU/t
  heatProduced: number;     // 本周期产热
  heatDissipated: number;   // 本周期散热（selfVent 实际生效部分）
  ventDissipated: number;   // 散热片额定散热（流体模式冷却液转换上限用）
  netHeat: number;          // heatProduced - heatDissipated（每周期净热量变化）
  slotHeat: Map<number, { produced: number; dissipated: number; energy: number }>;
  exploded: boolean;
  explosionPower: number;
  /** 流体模式：本周期产出的热冷却液 mB（× 1000 为桶的千分位） */
  hotCoolantOutputMb: number;
  /** 燃料棒全空时为 true（运行模式停止条件之一） */
  hasFuelRods: boolean;
}
