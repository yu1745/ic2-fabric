// 元件注册表：id → { i18n 名称、贴图、是否可放入反应堆、behavior 工厂 }。
// 名称直接取自 zh_cn.json（item.ic2_120.<id> / block.ic2_120.<id>），不自行翻译。
// 行为工厂把所有 behavior 模块串起来，并注入 resolver。

import type { ComponentId, IReactorComponent } from '../types';
import { setComponentResolver } from './resolver';
import { makeFuelRodBehavior } from './FuelRods';
import { makeHeatExchangerBehavior } from './HeatExchangers';
import { makeHeatVentBehavior } from './HeatVents';
import { makeCoolantOrPlatingBehavior } from './CoolantCells';
import { makeReflectorBehavior } from './Reflectors';

export interface ComponentMeta {
  id: ComponentId;
  /** i18n 名称（取自 zh_cn.json） */
  name: string;
  /** 贴图文件名（public/textures/<name>.png） */
  texture: string;
  /** 是否可放入反应堆槽位（枯竭棒/隔板占位等 true，但通常不放） */
  placeable: boolean;
  /** 行为工厂 */
  behavior: (id: ComponentId) => IReactorComponent;
  /** 用于元件栏分组排序 */
  category: 'fuel' | 'vent' | 'exchanger' | 'coolant' | 'condensator' | 'plating' | 'reflector';
}

// 名称来自 zh_cn.json（item.ic2_120.<id>）
const REGISTRY: Record<ComponentId, Omit<ComponentMeta, 'id'>> = {
  // 燃料棒
  uranium_fuel_rod: { name: '燃料棒 (铀)', texture: 'uranium_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  dual_uranium_fuel_rod: { name: '双联燃料棒 (铀)', texture: 'dual_uranium_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  quad_uranium_fuel_rod: { name: '四联燃料棒 (铀)', texture: 'quad_uranium_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  mox_fuel_rod: { name: '燃料棒 (MOX)', texture: 'mox_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  dual_mox_fuel_rod: { name: '双联燃料棒 (MOX)', texture: 'dual_mox_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  quad_mox_fuel_rod: { name: '四联燃料棒 (MOX)', texture: 'quad_mox_fuel_rod', placeable: true, behavior: makeFuelRodBehavior, category: 'fuel' },
  // 枯竭棒（运行产物，通常不放回，但允许显示）
  depleted_uranium_fuel_rod: { name: '燃料棒 (枯竭铀)', texture: 'depleted_uranium_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  depleted_dual_uranium_fuel_rod: { name: '双联燃料棒 (枯竭铀)', texture: 'depleted_dual_uranium_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  depleted_quad_uranium_fuel_rod: { name: '四联燃料棒 (枯竭铀)', texture: 'depleted_quad_uranium_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  depleted_mox_fuel_rod: { name: '燃料棒 (枯竭MOX)', texture: 'depleted_mox_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  depleted_dual_mox_fuel_rod: { name: '双联燃料棒 (枯竭MOX)', texture: 'depleted_dual_mox_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  depleted_quad_mox_fuel_rod: { name: '四联燃料棒 (枯竭MOX)', texture: 'depleted_quad_mox_fuel_rod', placeable: false, behavior: makeFuelRodBehavior, category: 'fuel' },
  // 散热片
  heat_vent: { name: '散热片', texture: 'heat_vent', placeable: true, behavior: makeHeatVentBehavior, category: 'vent' },
  reactor_heat_vent: { name: '反应堆散热片', texture: 'reactor_heat_vent', placeable: true, behavior: makeHeatVentBehavior, category: 'vent' },
  advanced_heat_vent: { name: '高级散热片', texture: 'advanced_heat_vent', placeable: true, behavior: makeHeatVentBehavior, category: 'vent' },
  overclocked_heat_vent: { name: '超频散热片', texture: 'overclocked_heat_vent', placeable: true, behavior: makeHeatVentBehavior, category: 'vent' },
  component_heat_vent: { name: '元件散热片', texture: 'component_heat_vent', placeable: true, behavior: makeHeatVentBehavior, category: 'vent' },
  // 热交换器
  heat_exchanger: { name: '热交换器', texture: 'heat_exchanger', placeable: true, behavior: makeHeatExchangerBehavior, category: 'exchanger' },
  reactor_heat_exchanger: { name: '反应堆热交换器', texture: 'reactor_heat_exchanger', placeable: true, behavior: makeHeatExchangerBehavior, category: 'exchanger' },
  component_heat_exchanger: { name: '元件热交换器', texture: 'component_heat_exchanger', placeable: true, behavior: makeHeatExchangerBehavior, category: 'exchanger' },
  advanced_heat_exchanger: { name: '高级热交换器', texture: 'advanced_heat_exchanger', placeable: true, behavior: makeHeatExchangerBehavior, category: 'exchanger' },
  // 冷却单元
  reactor_coolant_cell: { name: '10k 冷却单元', texture: 'reactor_coolant_cell', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'coolant' },
  triple_reactor_coolant_cell: { name: '30k 冷却单元', texture: 'triple_reactor_coolant_cell', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'coolant' },
  sextuple_reactor_coolant_cell: { name: '60k 冷却单元', texture: 'sextuple_reactor_coolant_cell', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'coolant' },
  // 冷凝器
  rsh_condensator: { name: '红石冷凝模块', texture: 'rsh_condensator', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'condensator' },
  lzh_condensator: { name: '青金石冷凝模块', texture: 'lzh_condensator', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'condensator' },
  // 隔板
  reactor_plating: { name: '反应堆隔板', texture: 'reactor_plating', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'plating' },
  reactor_heat_plating: { name: '高热容反应堆隔板', texture: 'reactor_heat_plating', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'plating' },
  containment_reactor_plating: { name: '密封反应堆隔热板', texture: 'containment_reactor_plating', placeable: true, behavior: makeCoolantOrPlatingBehavior, category: 'plating' },
  // 中子反射板
  neutron_reflector: { name: '中子反射板', texture: 'neutron_reflector', placeable: true, behavior: makeReflectorBehavior, category: 'reflector' },
  thick_neutron_reflector: { name: '加厚中子反射板', texture: 'thick_neutron_reflector', placeable: true, behavior: makeReflectorBehavior, category: 'reflector' },
  iridium_neutron_reflector: { name: '铱中子反射板', texture: 'iridium_neutron_reflector', placeable: true, behavior: makeReflectorBehavior, category: 'reflector' },
};

// behavior 实例缓存（按 id）
const behaviorCache = new Map<ComponentId, IReactorComponent>();

/** 注入 resolver：返回某 id 的 behavior 单例 */
function resolveBehavior(id: ComponentId): IReactorComponent | null {
  const meta = REGISTRY[id];
  if (!meta) return null;
  let b = behaviorCache.get(id);
  if (!b) {
    b = meta.behavior(id);
    behaviorCache.set(id, b);
  }
  return b;
}

// 一次性注入（模块加载时）
setComponentResolver(resolveBehavior);

export function getMeta(id: ComponentId): ComponentMeta {
  const m = REGISTRY[id];
  if (!m) throw new Error(`unknown component id: ${id}`);
  return { id, ...m };
}

/** 元件栏：只展示 placeable 的（枯竭棒不进栏，只在运行产物里显示） */
export const PALETTE_IDS: ComponentId[] = (Object.keys(REGISTRY) as ComponentId[]).filter((id) => REGISTRY[id].placeable);

/** 按 category 分组的元件栏顺序 */
export const CATEGORY_ORDER: ComponentMeta['category'][] = ['fuel', 'vent', 'exchanger', 'coolant', 'condensator', 'plating', 'reflector'];

export const PALETTE: Array<{ category: ComponentMeta['category']; items: ComponentMeta[] }> = CATEGORY_ORDER.map((category) => ({
  category,
  items: PALETTE_IDS.filter((id) => REGISTRY[id].category === category).map((id) => getMeta(id)),
}));
