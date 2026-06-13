# mcdebug TS Test Guide

mcdebug tests are TypeScript black-box integration tests. Do not add Kotlin
`McDebugTest`, `McDebugTestApi`, or `@McDebugTestPackages`.

Each subproject owns its tests under `<subproject>/test/mcdebug/`, and registers
its own Gradle task in that subproject's `build.gradle`. Shared dispatch,
parallel execution, area allocation, force-loading, cleanup, and reports are
provided by `@yu1745/mcdebug`.

## Run

```bash
pnpm install
./gradlew :core:mcdebugTest
```

`mcdebugTest` starts `core:runServer`, waits for `core/run/mcdebug/port`, runs
the TS tests with `pnpm exec tsx core/test/mcdebug/run.ts`, then stops the
server. Parallelism defaults to 128. Set `MCDEBUG_TEST_PARALLELISM=N` to change it.

## Test Organization

- Put tests in `<subproject>/test/mcdebug/**/*.test.ts` and export them with
  `defineTest(...)` or `defineTests([...])`. `run.ts` only creates the runner,
  scans test modules, and starts execution.
- Use `ctx.origin` for the machine under test and `ctx.pos(dx, dy, dz)` for
  nearby blocks.
- The mcdebug runner imports the test modules, collects exported tests, knows the
  total count, assigns isolated origins on a 2D grid by index, force-loads each
  area, clears it before/after the test, and runs tests in parallel.
- Prefer black-box RPC calls through the TypeScript `DebugApi`: `world.*`,
  `inv.*`, `be.*`, `fluid.*`, `wait.*`.

## Timing

Use `waitUntil(ctx, predicate, timeoutTicks)` for state predicates such as:

```ts
await waitUntil(ctx, invItemEquals(ctx.origin, 1, "minecraft:iron_ingot"), 15 * 20);
```

Use `waitTicks(ctx, ticks)` when the intent is only to let time pass. Do not
write raw `tick > 200` predicates; `waitTicks` reads the current server tick and
waits relative to that value.

## Machine Test Matrix

For each machine, keep at least:

- placement sanity
- one canonical successful recipe
- no power / no fuel idle case
- invalid input idle case
- output full / blocked output case when applicable

For powered machines, keep the hidden constraints explicit in setup helpers:
correct storage tier, facing, transformer upgrades, overclocker upgrades, and
slot numbers.
