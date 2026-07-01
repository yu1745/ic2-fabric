// sim 层 barrel：外部统一从 'sim/' 导入
export * from './constants';
export * from './types';
export * from './reactor';
export { getMeta, PALETTE, PALETTE_IDS, CATEGORY_ORDER } from './components/registry';
export type { ComponentMeta } from './components/registry';
export { triangularNumber } from './components/FuelRods';
// simulateFullLife 已在 reactor.ts 导出，通过 export * 自动暴露
