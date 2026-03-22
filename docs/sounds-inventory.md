# Sounds 清单

本档列出了项目中所有声音资源（`sounds.json` 事件定义与 `.ogg` 音频文件）。

## 统计信息

- **声音事件总数**: 59 个（来自 `assets/ic2/sounds.json`）
- **音频文件总数**: 59 个（来自 `assets/ic2/sounds/**/*.ogg`）
- **ic2_120 声音资源**: 0 个（当前仅复用 `assets/ic2` 命名空间）

---

## 一、事件映射 (Event -> Sound)

### 1.1 爆炸与杂项
| 事件 ID | 声音资源 | 猜测用途 |
|---|---|---|
| `block.nuke.explode` | `ic2:misc/nuke/explode` | 核弹爆炸时播放 |

### 1.2 发电机
| 事件 ID | 声音资源 | 猜测用途 |
|---|---|---|
| `generator.generator.loop` | `ic2:generators/generator` | 火力发电机运行循环音 |
| `generator.geothermal.loop` | `ic2:generators/geothermal` | 地热发电机运行循环音 |
| `generator.nuclear.loop` | `ic2:generators/nuclear/reactor` | 反应堆运行底噪循环 |
| `generator.nuclear.power.high` | `ic2:generators/nuclear/power/high` | 核电高功率状态提示音 |
| `generator.nuclear.power.low` | `ic2:generators/nuclear/power/low` | 核电低功率状态提示音 |
| `generator.nuclear.power.medium` | `ic2:generators/nuclear/power/medium` | 核电中功率状态提示音 |
| `generator.water.loop` | `ic2:generators/water` | 水力发电机运行循环音 |
| `generator.wind.loop` | `ic2:generators/wind` | 风力发电机运行循环音 |

### 1.3 物品与工具
| 事件 ID | 声音资源 | 猜测用途 |
|---|---|---|
| `item.battery.use` | `ic2:tools/battery` | 电池交互/插拔时播放 |
| `item.chainsaw.idle` | `ic2:tools/chainsaw/idle` | 链锯待机循环音 |
| `item.chainsaw.stop` | `ic2:tools/chainsaw/stop` | 链锯停机音 |
| `item.chainsaw.use1` | `ic2:tools/chainsaw/use1` | 链锯工作音变体 1 |
| `item.chainsaw.use2` | `ic2:tools/chainsaw/use2` | 链锯工作音变体 2 |
| `item.crowbar.use` | `ic2:tools/crowbar` | 撬棍使用音 |
| `item.cutter.use` | `ic2:tools/cutter` | 切割工具使用音 |
| `item.drill.hard` | `ic2:tools/drill/hard` | 钻头挖掘硬方块音 |
| `item.drill.idle` | `ic2:tools/drill/idle` | 钻头待机/低负载循环音 |
| `item.drill.soft` | `ic2:tools/drill/soft` | 钻头挖掘软方块音 |
| `item.electric.shutdown` | `ic2:tools/shutdown` | 电动工具断电/关机提示音 |
| `item.laser.explosive` | `ic2:tools/laser/explosive` | 采矿激光爆破模式发射音 |
| `item.laser.long_range` | `ic2:tools/laser/long_range` | 采矿激光远射模式发射音 |
| `item.laser.low_focus` | `ic2:tools/laser/low_focus` | 采矿激光低聚焦模式发射音 |
| `item.laser.scatter` | `ic2:tools/laser/scatter` | 采矿激光散射模式发射音 |
| `item.laser.shoot` | `ic2:tools/laser/shoot` | 采矿激光普通发射音 |
| `item.nanosaber.idle` | `ic2:tools/nanosaber/idle` | 纳米剑待机嗡鸣音 |
| `item.nanosaber.power_up` | `ic2:tools/nanosaber/power_up` | 纳米剑启用音 |
| `item.nanosaber.swing1` | `ic2:tools/nanosaber/swing1` | 纳米剑挥击音变体 1 |
| `item.nanosaber.swing2` | `ic2:tools/nanosaber/swing2` | 纳米剑挥击音变体 2 |
| `item.nanosaber.swing3` | `ic2:tools/nanosaber/swing3` | 纳米剑挥击音变体 3 |
| `item.painter.use` | `ic2:tools/painter` | 喷漆刷使用音 |
| `item.scanner.use` | `ic2:tools/scanner` | 扫描器扫描音 |
| `item.treetap.electric.use` | `ic2:tools/treetap/electric` | 电动树脂提取器使用音 |
| `item.treetap.use` | `ic2:tools/treetap/treetap` | 木龙头采树脂使用音 |
| `item.wrench.use` | `ic2:tools/wrench` | 扳手拆装/旋转方块音 |

### 1.4 机器
| 事件 ID | 声音资源 | 猜测用途 |
|---|---|---|
| `machine.canner.operate` | `ic2:machines/canner/operate` | 装罐机正向工作音 |
| `machine.canner.reverse` | `ic2:machines/canner/reverse` | 装罐机反向工作/回退音 |
| `machine.compressor.operate` | `ic2:machines/compressor/operate` | 压缩机工作循环音 |
| `machine.electrolyzer.loop` | `ic2:machines/electrolyzer/loop` | 电解机工作循环音 |
| `machine.extractor.operate` | `ic2:machines/extractor/operate` | 提取机工作循环音 |
| `machine.fabricator.loop` | `ic2:machines/fabricator/loop` | 复制机/质能机主循环音 |
| `machine.fabricator.scrap` | `ic2:machines/fabricator/scrap` | 复制机吞废料加速提示音 |
| `machine.furnace.electric.loop` | `ic2:machines/furnace/electric/loop` | 电炉运行循环音 |
| `machine.furnace.electric.start` | `ic2:machines/furnace/electric/start` | 电炉启动音 |
| `machine.furnace.electric.stop` | `ic2:machines/furnace/electric/stop` | 电炉停止音 |
| `machine.furnace.induction.loop` | `ic2:machines/furnace/induction/loop` | 感应炉运行循环音 |
| `machine.furnace.induction.start` | `ic2:machines/furnace/induction/start` | 感应炉启动音 |
| `machine.furnace.induction.stop` | `ic2:machines/furnace/induction/stop` | 感应炉停止音 |
| `machine.furnace.iron.operate` | `ic2:machines/furnace/iron/operate` | 铁炉工作音 |
| `machine.interrupt1` | `ic2:machines/state/interrupt1` | 机器被中断/停机提示音 |
| `machine.macerator.operate` | `ic2:machines/macerator/operate` | 打粉机工作循环音 |
| `machine.miner.operate` | `ic2:machines/miner/operate` | 采矿机工作循环音 |
| `machine.o_mat.operate` | `ic2:machines/o_mat/operate` | 交易机/物质机工作音 |
| `machine.overload` | `ic2:machines/state/overload` | 机器过载/异常警告音 |
| `machine.pump.operate` | `ic2:machines/pump/operate` | 泵工作循环音 |
| `machine.recycler.operate` | `ic2:machines/recycler/operate` | 回收机工作循环音 |
| `machine.teleporter.charge` | `ic2:machines/teleporter/charge` | 传送机充能阶段音 |
| `machine.teleporter.use` | `ic2:machines/teleporter/use` | 传送触发音 |
| `machine.terraformer.loop` | `ic2:machines/terraformer/loop` | 地形改造机运行循环音 |

---

## 二、音频文件列表 (.ogg)

```text
generators/generator.ogg
generators/geothermal.ogg
generators/nuclear/power/high.ogg
generators/nuclear/power/low.ogg
generators/nuclear/power/medium.ogg
generators/nuclear/reactor.ogg
generators/water.ogg
generators/wind.ogg
machines/canner/operate.ogg
machines/canner/reverse.ogg
machines/compressor/operate.ogg
machines/electrolyzer/loop.ogg
machines/extractor/operate.ogg
machines/fabricator/loop.ogg
machines/fabricator/scrap.ogg
machines/furnace/electric/loop.ogg
machines/furnace/electric/start.ogg
machines/furnace/electric/stop.ogg
machines/furnace/induction/loop.ogg
machines/furnace/induction/start.ogg
machines/furnace/induction/stop.ogg
machines/furnace/iron/operate.ogg
machines/macerator/operate.ogg
machines/miner/operate.ogg
machines/o_mat/operate.ogg
machines/pump/operate.ogg
machines/recycler/operate.ogg
machines/state/interrupt1.ogg
machines/state/overload.ogg
machines/teleporter/charge.ogg
machines/teleporter/use.ogg
machines/terraformer/loop.ogg
misc/nuke/explode.ogg
tools/battery.ogg
tools/chainsaw/idle.ogg
tools/chainsaw/stop.ogg
tools/chainsaw/use1.ogg
tools/chainsaw/use2.ogg
tools/crowbar.ogg
tools/cutter.ogg
tools/drill/hard.ogg
tools/drill/idle.ogg
tools/drill/soft.ogg
tools/laser/explosive.ogg
tools/laser/long_range.ogg
tools/laser/low_focus.ogg
tools/laser/scatter.ogg
tools/laser/shoot.ogg
tools/nanosaber/idle.ogg
tools/nanosaber/power_up.ogg
tools/nanosaber/swing1.ogg
tools/nanosaber/swing2.ogg
tools/nanosaber/swing3.ogg
tools/painter.ogg
tools/scanner.ogg
tools/shutdown.ogg
tools/treetap/electric.ogg
tools/treetap/treetap.ogg
tools/wrench.ogg
```

---

## 三、来源

- 事件定义：`src/main/resources/assets/ic2/sounds.json`
- 音频目录：`src/main/resources/assets/ic2/sounds/`
