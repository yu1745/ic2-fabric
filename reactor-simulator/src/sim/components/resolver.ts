// 解决循环依赖：FuelRods/HeatExchangers 需要查询邻位元件的 behavior，
// 但 registry 又需要导入它们。这里用一个可设置的解析器，由 registry 在初始化时注入。
import type { IReactorComponent } from '../types';
import type { ComponentId } from '../types';

export type ComponentResolver = (id: ComponentId) => IReactorComponent | null;

let resolver: ComponentResolver = () => null;

export function setComponentResolver(r: ComponentResolver): void {
  resolver = r;
}

export function getComponent(id: ComponentId): IReactorComponent | null {
  return resolver(id);
}
