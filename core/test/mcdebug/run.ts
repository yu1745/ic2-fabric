// mcdebug TS 测试入口。
//
// 本文件由 `:core:mcdebugTest` Gradle 任务调用，本身只负责装配测试运行器，
// 真正的用例分布在同级 `*.test.ts` 中，由 `loadTestModules` 自动扫描加载。
//
// 运行器配置说明：
// - `client.portFile`：`:core:runServer` 在 MC 服务端启动并开始监听后会写入
//   这个文件，端口号由运行器读取后再建立 debug RPC 客户端；启动前文件不
//   存在时运行器会等待，最长 10 秒（`timeoutMs`）。
// - `loadTestModules(runner, { dir })`：扫描本目录所有 `*.test.ts` 中导出的
//   `defineTest` / `defineTests`，将它们注册到运行器。运行器会为每个用例
//   在 2D 网格上分配独立的 origin 坐标、强制加载区块、用例前后清理区域，并
//   开启并行执行。
// - `reportFile`：测试结束后将结构化结果（JSON：pass/fail/duration/error）
//   写入该路径，方便 CI 和事后排查。
import { createTestRunner, loadTestModules } from "@yu1745/mcdebug";

const runner = createTestRunner({
  client: { portFile: "core/run/mcdebug/port", timeoutMs: 10_000 },
  reportFile: "core/build/mcdebugTest/results.json",
});

await loadTestModules(runner, {
  dir: new URL('.', import.meta.url),
});

await runner.run();
