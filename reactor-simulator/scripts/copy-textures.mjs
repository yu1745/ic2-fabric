// 一次性把 ic2 assets 里的核电元件贴图复制到 public/textures/，按 registry name 命名。
// 来源：core/src/main/resources/assets/ic2/textures/item/**（上游 ic2 命名空间，不可修改）
// 映射关系见 src/sim/components/registry.ts 的 texture 字段。
// 用法：node scripts/copy-textures.mjs
import { copyFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(__dirname, '..', '..');
const assetsRoot = resolve(repoRoot, 'core', 'src', 'main', 'resources', 'assets', 'ic2', 'textures', 'item');
const outDir = resolve(__dirname, '..', 'public', 'textures');

// registry name -> 相对 assetsRoot 的 PNG 路径
const mapping = {
  // 燃料棒（活跃）
  uranium_fuel_rod: 'reactor/fuel_rod/uranium.png',
  dual_uranium_fuel_rod: 'reactor/fuel_rod/dual_uranium.png',
  quad_uranium_fuel_rod: 'reactor/fuel_rod/quad_uranium.png',
  mox_fuel_rod: 'reactor/fuel_rod/mox.png',
  dual_mox_fuel_rod: 'reactor/fuel_rod/dual_mox.png',
  quad_mox_fuel_rod: 'reactor/fuel_rod/quad_mox.png',
  // 燃料棒（枯竭）
  depleted_uranium_fuel_rod: 'resource/nuclear/depleted_uranium.png',
  depleted_dual_uranium_fuel_rod: 'resource/nuclear/depleted_dual_uranium.png',
  depleted_quad_uranium_fuel_rod: 'resource/nuclear/depleted_quad_uranium.png',
  depleted_mox_fuel_rod: 'resource/nuclear/depleted_mox.png',
  depleted_dual_mox_fuel_rod: 'resource/nuclear/depleted_dual_mox.png',
  depleted_quad_mox_fuel_rod: 'resource/nuclear/depleted_quad_mox.png',
  // 散热片
  heat_vent: 'reactor/heat_vent.png',
  reactor_heat_vent: 'reactor/reactor_heat_vent.png',
  advanced_heat_vent: 'reactor/advanced_heat_vent.png',
  overclocked_heat_vent: 'reactor/overclocked_heat_vent.png',
  component_heat_vent: 'reactor/component_heat_vent.png',
  // 热交换器
  heat_exchanger: 'reactor/heat_exchanger.png',
  reactor_heat_exchanger: 'reactor/reactor_heat_exchanger.png',
  component_heat_exchanger: 'reactor/component_heat_exchanger.png',
  advanced_heat_exchanger: 'reactor/advanced_heat_exchanger.png',
  // 冷却单元
  reactor_coolant_cell: 'reactor/heat_storage.png',
  triple_reactor_coolant_cell: 'reactor/tri_heat_storage.png',
  sextuple_reactor_coolant_cell: 'reactor/hex_heat_storage.png',
  // 冷凝器
  rsh_condensator: 'reactor/rsh_condensator.png',
  lzh_condensator: 'reactor/lzh_condensator.png',
  // 隔板
  reactor_plating: 'reactor/plating.png',
  reactor_heat_plating: 'reactor/heat_plating.png',
  containment_reactor_plating: 'reactor/containment_plating.png',
  // 中子反射板
  neutron_reflector: 'reactor/neutron_reflector.png',
  thick_neutron_reflector: 'reactor/thick_neutron_reflector.png',
  iridium_neutron_reflector: 'reactor/iridium_reflector.png',
};

mkdirSync(outDir, { recursive: true });

let ok = 0;
let missing = 0;
for (const [name, rel] of Object.entries(mapping)) {
  const src = resolve(assetsRoot, rel);
  const dst = resolve(outDir, `${name}.png`);
  if (!existsSync(src)) {
    console.warn(`[缺失] ${name} <- ${rel} (源文件不存在)`);
    missing++;
    continue;
  }
  copyFileSync(src, dst);
  console.log(`[ok]   ${name} <- ${rel}`);
  ok++;
}
console.log(`\n完成：复制 ${ok} 个，缺失 ${missing} 个 -> ${outDir}`);
