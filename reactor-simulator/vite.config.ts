/// <reference types="vitest" />
import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// 配置见 plan §技术栈：Preact + Vite + TS
// vitest 字段由 vitest.config.ts 单独定义，这里只管应用构建
export default defineConfig({
  plugins: [preact()],
  base: './',
});
