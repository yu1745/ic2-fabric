# 采矿机 + 高级采矿机实现计划

## 1. 目标与范围

- 同时实现两台机器：`miner`（普通采矿机）与 `advanced_miner`（高级采矿机）。
- 按项目规范落地完整机器链路：`Block + BlockEntity + Sync + ScreenHandler + Screen + assets/ic2_120`。
- 注册方式仅使用类级注解，不写分散注册逻辑。

## 2. 已冻结规则（最终版）

### 2.1 机器能力与限制

- 普通采矿机：`Tier 2`。
- 高级采矿机：`Tier 3`（512 EU/t）。
- 普通采矿机仅允许安装 `OD` 扫描仪。
- 高级采矿机允许安装 `OD / OV` 扫描仪。
- 挖掘范围 = 已安装扫描仪范围。
- 白黑名单槽位固定 `3x5 = 15`。
- 升级槽固定 `4`。
- 重启扫描后起点：机器下方一层。
- 到达底部停止阈值：`Y = -64`。

### 2.2 能量与耗电

- 每个扫描步固定耗电：`64 EU`。
- 无矿或不采掘时，扫描步耗电仍照常扣除。
- 采掘耗电由钻头类型决定：
  - 采矿钻头：`500 EU / 次采掘`
  - 钻石钻头 / 铱钻头：`1500 EU / 次采掘`
- 精准采集开启时：采掘耗电乘以 `10`。
- 容量规则采用方案 B：按“扫描 + 采掘”同次操作总耗电计算，且不加缓冲倍数。
  - 采矿钻头档：`64 + 500 = 564 EU`
  - 钻石/铱钻头档：`64 + 1500 = 1564 EU`

### 2.3 超频与周期

- 每个工作周期执行一次扫描步。
- 基础扫描周期：`20 tick`。
- BlockEntity 实例构造时使用 `0..19` 随机偏移，避免同频共振。
- 超频最小周期允许到 `1 tick`。
- 周期公式：

```text
effectivePeriodTicks = max(1, floor(20 / speedMultiplier))
```

### 2.4 扫描仪与钻头行为

- 扫描器槽会消耗扫描仪本体电量。
- 当机器自身有电时，会优先给扫描仪补电，使扫描仪维持满电。
- 扫描仪“使用次数”仅为显示推导值（由总电量/单次消耗推导），不是独立耐久。
- 钻头不损耗耐久；钻头成本已通过采掘 EU 体现。

### 2.5 液体自动抽取（扫描游标扫到液体）

- 自动抽取前提：安装了流体弹出升级。
- 机器名义无流体缓存，但实现保留 `1B` 内部缓存以兼容 Fabric Fluid API。
- 仅当 `1B` 缓存为空时，允许自动抽取。
- 抽取速率：每秒（每次工作）`1` 格液体。
- 液体抽取不额外耗电。
- 先抽入 `1B` 缓存，再按流体弹出逻辑输出；缓存再次为空后才可进行下一次自动抽取。

## 3. Tick 执行顺序（服务端）

1. 客户端短路
   - `world.isClient` 直接返回。
2. 升级与参数刷新
   - 应用超频/储能/变压（及流体弹出）升级。
   - 计算 `effectivePeriodTicks`、有效容量、有效输入上限。
3. 能量输入阶段
   - 从相邻网络拉电。
   - 从电池槽放电补能。
4. 扫描仪补电阶段
   - 若扫描器槽存在有效扫描仪且机器有可用电量，向扫描仪充电至满电。
5. 液体自动抽取阶段
   - 仅在满足前提（有流体弹出升级 + 缓存空）时，每 20 tick 最多抽 1 格。
   - 抽后立刻尝试外送，保证缓存尽快回空。
6. 周期门控
   - 基于 `initialOffset + worldTime` 与 `effectivePeriodTicks` 判断本 tick 是否为工作周期。
   - 非工作周期仅做同步并返回。
7. 扫描步扣电（先扣）
   - 先尝试扣除 `64 EU`；失败则本周期结束（不扫描不采掘）。
8. 扫描目标获取与游标推进
   - 读取当前游标目标方块。
   - 游标推进到下一格（单周期只推进一次）。
   - 若推进后已触底（`Y < -64`），置停机状态并结束。
9. 方块过滤判定
   - 跳过空气/不可挖/非目标。
   - 应用白名单/黑名单与模式判定。
10. 采掘阶段
   - 按钻头类型计算采掘耗电（500/1500），精准采集乘 `10`。
   - 能量不足则本周期仅完成扫描，不执行采掘。
   - 能量充足则执行破坏并收集掉落。
11. 掉落处理
   - 本次掉落一次性尝试输出（优先邻接容器，剩余掉落世界）。
12. 收尾同步
   - 同步能量流、坐标、状态、库存变化；必要时 `markDirty()`。

## 4. 代码落地拆分

1. 机器与实体
   - `src/main/kotlin/ic2_120/content/block/MinerBlock.kt`
   - `src/main/kotlin/ic2_120/content/block/AdvancedMinerBlock.kt`
   - `src/main/kotlin/ic2_120/content/block/machines/MinerBlockEntity.kt`
   - `src/main/kotlin/ic2_120/content/block/machines/AdvancedMinerBlockEntity.kt`
2. 同步层
   - `src/main/kotlin/ic2_120/content/sync/MinerSync.kt`
   - `src/main/kotlin/ic2_120/content/sync/AdvancedMinerSync.kt`
3. 容器与 UI
   - `src/main/kotlin/ic2_120/content/screen/MinerScreenHandler.kt`
   - `src/main/kotlin/ic2_120/content/screen/AdvancedMinerScreenHandler.kt`
   - `src/client/kotlin/ic2_120/client/MinerScreen.kt`
   - `src/client/kotlin/ic2_120/client/AdvancedMinerScreen.kt`
4. 资源与本地化（仅 `ic2_120` 命名空间）
   - `src/main/resources/assets/ic2_120/blockstates/miner.json`
   - `src/main/resources/assets/ic2_120/blockstates/advanced_miner.json`
   - `src/main/resources/assets/ic2_120/models/item/miner.json`
   - `src/main/resources/assets/ic2_120/models/item/advanced_miner.json`
   - `src/main/resources/assets/ic2_120/lang/zh_cn.json`
   - `src/main/resources/assets/ic2_120/lang/en_us.json`

## 5. 验收清单

- Screen/ScreenHandler/SyncedData 属性顺序一致。
- Shift 快速移动遵循 `SlotSpec + SlotMoveHelper`。
- 两机型扫描器限制正确（普通仅 OD，高级 OD/OV）。
- 每扫描步固定扣 `64 EU`（无矿也扣）行为成立。
- 采掘耗电按钻头档位与精准采集倍率正确。
- 液体自动抽取前提与速率正确（1 秒 1 抽，缓存空才抽）。
- 编译验收：

```bash
./gradlew clean compileKotlin compileClientKotlin
```
