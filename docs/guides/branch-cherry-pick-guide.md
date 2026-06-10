# main (1.20.1) → 1.21.1 迁移 cherry-pick 指南

> 给"在 1.21.1 worktree 上 cherry-pick main 的 commit"的人看。**先读一遍再动手**。
>
> **证据来源**：`docs/branch-sync-status.md`（68 条待同步）+ `docs/branch-sync-archive.md`（125 条已归档）共 **193 个真实 entry** 的"备注"列，外加对应 commit 的 diff。每条结论都标了 entry 编号作为证据。
>
> 配套：`AGENTS.md` §9 分支同步管理、`docs/migration-1.20.1-to-1.21.1.md`（API 细节查询表）。

---

## 0. 路线图（30 秒版）

main 是**决策根**，1.21.1 是**移植目标**。两者关系不是 git 父子，而是手动双轨同步：

```
main  ── feature commit ──→  1.21.1 worktree (wt-121)
       ──cherry-pick──→  解决冲突 / 适配 1.21.1 API
       ── 单独 commit ──→  在 1.21.1 上写"chore: 适配 1.21.1 API"
```

**单条迁移 commit 极少能一次过**。常见节奏：cherry-pick → 编译 → 1.21.1 修复 commit → 再 cherry-pick 下一个。

---

## 1. 准备工作

### 1.1 准备两个 worktree（**不要在主工作区搞**）

主工作区通常在 1.21.1 上有未提交改动。**不要**在主工作区里 cherry-pick。准备两个干净 worktree：

```bash
# main：源
git worktree add /home/wangyu/wt/ic2-fabric/wt-main main

# 1.21.1：目标（用新分支承载，避开主工作区 dirty 状态）
git worktree add -b migration-121 /home/wangyu/wt/ic2-fabric/wt-121 1.21.1
```

> 1.21.1 用 `-b migration-121` 新分支而不是直接 `add 1.21.1`，是因为主工作区可能正在 1.21.1 上有 dirty 改动。cherry-pick 起点（commit 内容）完全一致，行为无差异。

### 1.2 拉一份 1.21.1 → main 的差异表

```bash
git log --oneline $(git merge-base main 1.21.1)..main
```

对照**主工作区**的 `docs/branch-sync-status.md`（1.21.1 上**没有**这个文件，参见 §6）看哪些已经同步了，避免重复劳动。

### 1.3 先确认 build / datagen 跑得通

```bash
cd /home/wangyu/wt/ic2-fabric/wt-121
./gradlew build                    # 必须能编
./gradlew :core:runDatagen         # 必须能跑 datagen（如果目标 commit 涉及数据）
```

任何一个跑不通，先修 wt-121 让它通，再开始 cherry-pick。否则会分不清是 cherry-pick 引入的还是基线就有的。

---

## 2. Cherry-pick 流程（单 commit 版）

```bash
cd /home/wangyu/wt/ic2-fabric/wt-121

# 2.1 单条 cherry-pick
git cherry-pick <commit-sha>

# 2.2 出现冲突：
#     a) 打开冲突文件，对比 main 原版（去 wt-main 看 git show <sha>）
#     b) 解决冲突（参考 §3-§5 模式识别冲突类型）
#     c) git add .
#     d) git cherry-pick --continue
#
# 2.3 如果 commit 涉及 1.21.1 已有但形式不同的代码（参见 §6）：
#     解决冲突后必须立刻做 API 适配，不能只改冲突标记
#
# 2.4 编译验证
./gradlew build

# 2.5 涉及数据/配方/材质时
./gradlew :core:runDatagen
./gradlew build
```

---

## 3. cherry-pick 处理不了的 5 大类问题

这是本指南最关键的部分——**`git cherry-pick` 只懂挪文本，不懂 1.21.1 API 已换过**。下面 5 大类都有 entry 编号做证据。

### 3.1 API 方法签名重命名 / 参数重排 / 类型变更

> API 细节、批量替换 grep、1.20.1→1.21.1 代码对比见 `docs/migration-1.20.1-to-1.21.1.md` §1-§15、§18。下面只列 cherry-pick 实战中最容易踩的 entry 证据 + 关键反直觉点。

**真实证据**（entry 编号 + 备注原文）：

- **#18** `27f47004`：管道 tooltip「适配 1.21.1 `appendTooltip` 签名（移除 `world`、`@Environment`，改用 `TooltipType` + `Item.TooltipContext`）」
- **#22** `8bd5d4c`：「1.21.1 额外提交 `6d3c446`」——主 cherry-pick 编译过了，运行时/二级调用点仍缺方法
- **#25** `f984ec2`：「适配 1.21.1 API（`validateTicker`、`RecipeExporter`）」
- **#30** `59e897e`：「Identifier.of、CustomPayload、getCustomData」——三个新 API 同 PR
- **#38** `8863ece`：「1.21.1 适配 `getLeashHolder()`」——`Leashable.getHoldingEntity()` 改名且带 `player` 参数
- **#55** `f9f399fc`：「getScreenOpeningData、RegistryWrapper、validateTicker、RecipeExporter」——四个 API 堆叠
- **#88** `9380770c`：「65 文件 66 处，直接文本替换到 1.21.1（`validateTicker` 版本）」——**机械重排漏一处 = 编译断**
- **#118** `f82870b4`：「`onUse` 无 `Hand`、ExtendedScreenHandlerFactory<PacketByteBuf>/getScreenOpeningData、RecipeExporter」
- **#119** `06f74979`：「FluidVariant CODEC、ExtendedScreenHandlerFactory、OnUse 无 `Hand`、validateTicker」——蒸汽系统单 PR 触雷最多
- **#159** `1dd7edc4`：消除 Kotlin 编译警告（**废弃覆盖**）——**关键证据**：链锯仍 override 1.20.1 的 `getMiningSpeedMultiplier`，1.21.1 上是「废弃覆盖」warn 而非编译错，**运行时静默退回 1.0**

**最容易漏的反直觉点**：

- **删了旧重写但忘补新重写 → 运行时静默回归**（`#159`/`#170` 族）：`getMiningSpeedMultiplier`→`getMiningSpeed`、`isSuitableFor`→`isCorrectForDrops`、`getHoldingEntity()`→`getLeashHolder(player)`。旧 override 不报错只 warn，链锯/钻头挖掘速度退回 1.0，玩起来才发现。
- **泛型参数漏加**（`#16`/`#28`/`#50`/`#55`/`#90`/`#95`/`#118` 反复出现）：`ExtendedScreenHandlerFactory` 加 `<PacketByteBuf>`，漏写 `writeScreenOpeningData` 行为对不上
- **参数顺序变**（`#18`）：`appendTooltip(stack, world, tooltip, ctx)` → `(stack, ctx, tooltip, type)`，参数类型两两接近，grep `\btooltip\b` 搜不到
- **配套 import 漏改**（`#103`/`#109`/`#113`/`#116`/`#120`）：包路径变了
- **新必填抽象方法**：`Block.getCodec(): MapCodec<out Block>`（1.21.1 新增）

---

### 3.2 NBT / DataComponent 持久化——BE、ItemStack、FluidVariant 全面换 API

> API 细节、批量替换 grep、1.20.1→1.21.1 代码对比见 `docs/migration-1.20.1-to-1.21.1.md` §10-§13。

**真实证据**：

- **#30** `59e897e`：JEI 适配"getCustomData"——1.21.1 起 `stack.nbt` 不再可用
- **#42** `5286bae`：量子装备「Payload 网络/**DataComponent NBT**/appendTooltip 签名」
- **#55** `f9f399fc`：「getScreenOpeningData、**RegistryWrapper**、validateTicker、RecipeExporter」
- **#109** `cd5e7cdc`：「Identifier.of/ExtendedScreenHandlerFactory/**Inventories**/RecipeInput/**DataComponentTypes.FOOD**」
- **#119** `06f74979`：「**FluidVariant CODEC**、ExtendedScreenHandlerFactory、OnUse 无 Hand、validateTicker」
- **#120** `37af24e4`：压缩机容器返还「**ItemStack.fromNbt/encode**」——`fromNbt` 漏传 lookup 在 cherry-pick 时被显式补上
- **#121** `5f6df96f`：「**FluidPipeUpgradeComponent DataComponent** 适配」

**最容易漏的反直觉点**：

- **写入必须用 `editCustomData { }`**——`getCustomData()` 拿到的 NbtCompound 是只读视图，改完不落盘；main 上 `stack.nbt` / `orCreateNbt` 的"拿 val 改"习惯在 1.21.1 完全失效
- **`getCustomData()` 返回 nullable**——必须 `?: return` / `?: continue`，漏 null check 编译能过、运行时 NPE
- **`lookup: RegistryWrapper.WrapperLookup` 漏传**——`Inventories.readNbt/writeNbt` / `ItemStack.fromNbt` 全部要求
- **`FluidVariant.CODEC` 必须 `.result().orElse(...)` 兜底**——链式调用漏一段运行时 NPE
- **DataComponent 新类型 import 漏**——`DataComponentTypes.FOOD` 及其字段在 #109 全量迁移时集中暴露

---

### 3.3 网络包协议整体换型——CustomPayload + PacketCodec + PayloadTypeRegistry

> API 细节、批量替换 grep、1.20.1→1.21.1 代码对比见 `docs/migration-1.20.1-to-1.21.1.md` §8。代码模板见下方"修复模板"小节。

**真实证据**（出现 7+ 次的高频踩坑类）：

- **#30** `59e897e`：「Identifier.of、CustomPayload、getCustomData」
- **#42** `5286bae`：「Payload 网络/DataComponent NBT/appendTooltip 签名」
- **#49** `b708df3e`：「Payload 网络」（半流体发电机燃料颜色同步）
- **#52** `8f064e2d`：「Payload 网络」（泡沫枪独立键位）
- **#53** `e303132b`：「保留 AddonConfigSyncPacket，main 的 ConfigSyncHelper 不适用于 1.21.1 已移除」——**关键反例**：1.21.1 已有等价 Payload 时保留 1.21.1 版本不覆盖
- **#114** `ec799f6e`：「CustomPayload C2S（SelectTemplatePayload）」——单包 cherry-pick 典型
- **#116** `f7b604d9`：「RecipeExporter、Identifier.of、CustomPayload C2S」
- **#118** `f82870b4`：「onUse 无 Hand、ExtendedScreenHandlerFactory<PacketByteBuf>/getScreenOpeningData、RecipeExporter」
- **#119** `06f74979`：「FluidVariant CODEC、ExtendedScreenHandlerFactory、OnUse 无 Hand、validateTicker」——蒸汽系统综合

**最容易漏的反直觉点**：

- companion 没加 `CODEC`：编译过、运行时 NPE（Payload 反射取 codec 失败）
- 漏 `PayloadTypeRegistry.playC2S()/playS2C().register(id, codec)`：运行时 unknown packet
- 接收回调 lambda 还在 5 参 `(server, player, handler, buf, sender)` 旧签名：编译失败
- `ClientPlayNetworking.send(id, buf)` 双参：编译失败，应为单参 `send(payload)`
- lambda 内仍包 `server.execute {}` / `client.execute {}`：1.21.1 回调已在正确线程，再包会死锁
- **1.21.1 已有等价 Payload 时 main 上同名 helper 保留 1.21.1 版本不覆盖**（#53 反例）

**关键模板**（Payload + PacketCodec 4 段：主类 + 注册 + 接收 + 发送）：

```kotlin
// 1) 主类
class SelectTemplatePayload(val templateId: Int) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
    companion object {
        val ID: CustomPayload.Id<SelectTemplatePayload> =
            CustomPayload.Id(Identifier.of("ic2_120", "select_template"))
        val CODEC: PacketCodec<PacketByteBuf, SelectTemplatePayload> =
            PacketCodec.of(
                { buf, p -> buf.writeInt(p.templateId) },
                { buf -> SelectTemplatePayload(buf.readInt()) }
            )
    }
}

// 2) 注册
PayloadTypeRegistry.playC2S().register(SelectTemplatePayload.ID, SelectTemplatePayload.CODEC)

// 3) 服务端接收（1 参 lambda，新线程模型已自带）
ServerPlayNetworking.registerGlobalReceiver(SelectTemplatePayload.ID) { payload, context ->
    val player = context.player()
    // 不再 server.execute {}
}

// 4) 客户端发送（单参 payload）
ClientPlayNetworking.send(SelectTemplatePayload(templateId = 3))
```

---
### 3.4 datagen 路径单数化 + modid 过滤 + 旧产物清理

**真实证据**（几乎每条涉及数据/配方的 commit 都附带）：

- **#39** `df4c5ed`：「适配 1.21.1 单数 recipe/ 路径」
- **#44** `2595b3f0`：「datagen 使用 modid 过滤避免附属触发本体缓存冲突」
- **#70-#96** 大量连续 commit 都以「datagen 刷新」结尾——**最后一个跑一次就够了**
- **#94** `3c7d41c7`：「适配 1.21.1 路径（recipe/ 单数）+ datagen 刷新」
- **#112** `83305a9c`：「适配 1.21.1 tags/block/ 路径」
- **#117** `d80abc0d`：「适配 1.21.1 路径（recipe/ 单数）+ datagen 刷新」
- **#118** `f82870b4`：「data 目录单数 + datagen 刷新」
- **#125** `7f74eecc`：「RecipeExporter/CraftingRecipeCategory（tools→equipment）+ datagen 刷新」

**容易漏**：

- 旧 `generated/` 目录没删，1.21.1 同时读到旧 + 新路径两份，行为不一致
- `replace: true/false` 字段还残留在 tag JSON 中（1.21.1 移除）
- `RecipeCategory.TOOLS` 还在用（要改 EQUIPMENT）
- 附属模块没加 modid 过滤（datagen 把 core 的缓存也清掉）
- 多个连续 cherry-pick 都涉及数据时，**最后一次刷新只跑一次**就够了

**自检 grep**：

```bash
grep -rE 'data/.*/recipes/'  core/src/main/generated/             # 应为零
grep -rE 'data/.*/tags/'     core/src/main/generated/             # 应为零
grep -rE 'tags/items|tags/blocks'  core/                          # 应为零
grep -r  'RecipeCategory\.TOOLS'  core/                           # 应为零
find . \( -path '*/generated/data/*/recipes' \
        -o -path '*/generated/data/*/tags/items' \
        -o -path '*/generated/data/*/tags/blocks' \)              # 应为空
```

**完整操作流程**（涉及数据时）：

1. cherry-pick 前先 `rm -rf core/src/main/generated/`（核心 + 各附属）
2. cherry-pick 解决路径冲突时**全选 1.21.1 版本**（单数）
3. cherry-pick 完成后 `./gradlew :core:runDatagen`
4. datagen 单独 commit（不要混在 cherry-pick commit 里）
5. 跑 `./gradlew build` 确认数据文件能加载
6. 多个连续 cherry-pick 处理数据时，最后一个跑完 datagen 一次刷新所有文件

---

### 3.5 结构性 / 机制性 / 流程性踩坑（不是纯 API 变更）

#### A. 1.21.1 已有等价物 → 保留 1.21.1，不覆盖

- **#16** `fe72f4e6`：「保留 1.21.1 `ExtendedScreenHandlerFactory<PacketByteBuf>` 泛型」
- **#22** `8bd5d4c`：「1.21.1 额外提交 `6d3c446`」——cherry-pick 不完整
- **#33** `8f4078e`：「代码已存在于 1.21.1，额外 API 适配提交 `6abb6e4`」
- **#53** `e303132b`：「保留 AddonConfigSyncPacket，main 的 ConfigSyncHelper 不适用于 1.21.1 已移除」

**原则**：1.21.1 已存在等价实现时，cherry-pick 阶段若 main 版本与 1.21.1 等价物语义相同，**直接保留 1.21.1 现有代码**，不要用 main 覆盖。覆盖会破坏 1.21.1 后续适配。

#### B. cherry-pick 不完整 → 1.21.1 还要单独加 commit

**流程**：cherry-pick main commit 后，若 1.21.1 还需要适配 API（`Identifier.of`、`RecipeExporter`、`validateTicker` 等），**再追加一条独立 commit** 写明"适配 1.21.1"，不要把适配塞进 cherry-pick commit 本身。

#### C. merge commit 必须 `cherry-pick -m 1`

- **#109** `cd5e7cdc`：「cherry-pick -m 1 + 1.21.1 API 适配」

```bash
git cherry-pick -m 1 <merge-commit-sha>   # 选第一父（main）
```

不指定会报 `fatal: Commit <sha> is a merge but no -m option was given.`。
`-m 1` 选错 mainline 会引入父分支无关历史；统一选 **1**（第一父 = main）。改后必须跑 datagen + build。

#### D. GUI/Screen 冲突"取 HEAD 版本"原则

- **#123** `152ca7ab`：「Screen conflict 解决（取 HEAD 版本）」

**为什么**：1.21.1 的 DrawContext/坐标/matrix stack API 链与 1.20.1 不可互换；接受 main 的 1.20.1 GUI 写法会**直接编译失败**。Screen/Container 冲突时默认保留 1.21.1（HEAD），**不要接受 main 版本**。

#### E. 极广范围机械替换（65 文件 66 处）

- **#88** `9380770c`：「65 文件 66 处，直接文本替换到 1.21.1（validateTicker 版本）」

**流程**：

1. `git show 9380770c > /tmp/x.patch`
2. `git apply --3way /tmp/x.patch` 解决冲突
3. 或 `sed -i 's/old/new/g' $(grep -rl old core/)`
4. **不要**逐文件 cherry-pick，浪费时间

#### F. 命名变更 / enum 重命名 cherry-pick 时容易扯到

- **#25** `f984ec2`：「FluidBottler 统一更名为 FluidCanner」
- **#125** `7f74eecc`：「tools→equipment」

**流程**：命名变更 commit 必然牵连 Block/Item/BE/ScreenHandler/Screen/Sync/lang/recipe；cherry-pick 前先 `git show --stat` 看扩散面，先 cherry-pick 命名 commit，再 cherry-pick 功能 commit（依赖文件存在）。

#### G. PR merge 后误删 / 冲突解决时误删

- **#132** `e0d5114e`：「恢复 PR 中误删的 steam 流体常量和类 + 修复 BlastFurnace typo」
- **#137** `3377e9e6`：「补上冲突解决时误删的 Ic2Fluid 结束大括号」

**防御**：

- 冲突解决后**先 `git diff` 看 unstaged**，再 `git diff --staged` 看 staged
- 抽查 `grep -rn '^}\| ^}$' 新增文件` 看大括号
- cherry-pick 涉及 fluid/class 时**单独 review 一下大括号/分号**

#### H. 高频冲突点

- **#64** `354c59e5`：「解决冲突（import、tooltip 参数适配、**TODO.md**）」
- **#153** `ad08a51f`：「build.yml 注释化」
- **#189** `ab6773ed`：「升级 fabric_guidebook 至 0.1.4 + AGENTS.md / docs/README.md 文档入口」

`TODO.md` / `build.yml` / `settings.gradle` / `gradle.properties` / `AGENTS.md` 在 1.20.1 与 1.21.1 常因环境差异同时被改。**统一策略**：取语义更细的一方 + 在 commit message 注明"含 X 冲突解决"。

#### I. 单源真相文件 → `--skip`（**用户特别指示**）

- **#166** `1d6ade59`：「record pending container screen sync」

**跳过清单**（不要 cherry-pick，main 是唯一来源）：

- `docs/branch-sync-status.md`
- `docs/branch-sync-archive.md`
- `docs/migration-1.20.1-to-1.21.1.md`

**操作**：`git cherry-pick --skip`，**不要手写**这些文件到 1.21.1。

**反例**：`AGENTS.md` / `CLAUDE.md` **不是**单源真相（符号链接 `ln -s` 两处生效），**应当 cherry-pick**。

#### J. 整模块 / 资源 namespace / 语言文件

- **#151** `019e3ea6`：「迁移 ic2 翻译到 ic2_120」
- **#171** `6d768271`：「动能发生机贴图迁移到 ic2_120 命名空间 + 新增贴图」
- **#178-181** BuildCraftAddon 四个连续 commit 整模块添加
- **#183** `03aa4708`：「添加 Guidebook 校验工具」
- **#189** `ab6773ed`：fabric_guidebook 升级
- **#190** `afc9168a`：「mod-sync-fabric 后台同步 mod 索引与重启并发」

**原则**：

- 资源（`assets/ic2 → ic2_120`）与语言（`zh_cn.json`）是整 namespace 迁移，cherry-pick 范围以**目录为单位**
- 附属模块用 `git cherry-pick <sha1> <sha2> <sha3> <sha4>` 一次性串入
- `assets/ic2/**` 上游不可改；任何冲突必须重定向到 `assets/ic2_120/**`

#### K. 完整流程（处理大重构 commit 如 `#119` 蒸汽系统）

1. `git show <sha> --stat` 看影响范围
2. 准备 1.21.1 已有的等价物清单（避免被覆盖）
3. `git cherry-pick -m 1 <sha>`（merge）或 `git cherry-pick <sha>`（普通）
4. 按 entry 备注顺序处理 import / 泛型 / API 改名冲突
5. 单条 1.21.1 适配 commit 写差异
6. 跑 `./gradlew :core:runDatagen` + `./gradlew build`

---

## 4. Cherry-pick 过程垃圾（必须清理）

`git cherry-pick` 完成后，**必须 grep 一次**确认以下痕迹没残留：

```bash
# 4.1 冲突标记
grep -rn '<<<<<<<\|=======\|>>>>>>>' core/ addon*/ advanced-*/

# 4.2 残行（cherry-pick 把上一个 hunk 的尾巴错位了）
grep -rn ')(feat:\|)(fix:\|)(chore:\|)(docs:' core/ addon*/

# 4.3 误删/未补的 API 适配（参见 §3.1 grep 清单）
grep -rn 'orCreateNbt\|FabricItemSettings\|getMiningSpeedMultiplier\|isSuitableFor\|getHoldingEntity' core/ addon*/
```

每次 cherry-pick `--continue` 之后立刻跑一遍。**示例**：`f00c2e0b` 的 diff 里就出现过 `)(feat: 半流体发电机燃料颜色网络同步 + 流体颜色注册表)` 残行，需要单独 commit "清理冲突残余文本"。

---

## 5. 必须 `--skip` 的 commit

见 §3.5.I。要点：

- `docs/branch-sync-status.md`、`docs/branch-sync-archive.md`、`docs/migration-1.20.1-to-1.21.1.md` 视作 main 单源真相，**1.21.1 不复制**
- 看到 commit 改动以上任一文件 → `git cherry-pick --skip`
- 同步状态只在 main 上维护；1.21.1 上的"已同步"打勾、datagen 重跑等**以 main 上的状态表为准**

---

## 6. 涉及 build / 配置时的坑

### 6.1 gradle.properties / settings.gradle 冲突

- `gradle.properties` 改版本号（如 `fabric_guidebook` 升级）会冲突——**接受 1.21.1 一侧**（对应 1.21.1 loom）
- `settings.gradle` 加新模块（`buildcraft-addon`）会冲突——**只保留 1.21.1 现有 include**，删除新模块那行

### 6.2 第三方依赖版本

main 上加的依赖（如 Jade）在 1.21.1 的 `gradle/libs.versions.toml` 里**可能还没添加**。cherry-pick 编译失败时：

1. 看错误信息定位缺的 import
2. 去 1.21.1 的 `gradle/libs.versions.toml` 添加对应版本
3. 这条"加依赖"的改动**单独 commit**，作为该 cherry-pick 的附属 commit

### 6.3 buildSrc / convention plugin

1.21.1 的 `buildSrc/` 内容可能与 main 的 convention plugin 重复。**优先用 1.21.1 现有的**，把 main 的新 plugin 内容合并进去。

---

## 7. 提交规范（避免 commit 污染）

按 `AGENTS.md` §9.1：

1. cherry-pick 一个 main commit，**保留原 commit message**
2. 如果需要 API 适配，**单独 commit**：`chore: 适配 1.21.1 API——<原 commit 简述>`
3. datagen 重生成**单独 commit**：`chore: datagen 刷新（cherry-pick 后）`
4. 同步状态表更新**单独 commit**（**只更新 main**，见 §5）

**禁止**：把 cherry-pick + API 适配 + datagen 合成一个 mega commit。出问题时回退不干净。

---

## 8. 自检清单（每次 cherry-pick 后跑一遍）

```bash
# 1) 编译
./gradlew build

# 2) datagen（如涉及）
./gradlew :core:runDatagen && ./gradlew build

# 3) 冲突标记清理（§4）
! grep -rn '<<<<<<<\|=======\|>>>>>>>' .

# 4) 残留旧 API（§3.1 grep 清单）
! grep -rE 'orCreateNbt|FabricItemSettings|getMiningSpeedMultiplier|isSuitableFor|getHoldingEntity' .

# 5) 1.21.1 适配检查（§5 单源真相文件应无被带过来）
! git diff --name-only HEAD~1 | grep -E 'branch-sync-(status|archive)|migration-1\.20\.1-to-1\.21\.1\.md'

# 6) datagen 路径单数化（§3.4）
! find . \( -path '*/generated/data/*/recipes' -o -path '*/generated/data/*/tags/items' \)

# 7) 网络包协议（§3.3）
! grep -rE 'registerGlobalReceiver\(.*Identifier' .

# 8) NBT/DataComponent（§3.2）
! grep -rE 'orCreateNbt|stack\.nbt\b' core/ addon*/
```

任意一步失败 → **回到 wt-121 修**。**不要在主工作区里接着改**。

---

## 9. 完整迁移批次示例

```bash
# 1) 在 wt-main 找到 SHA 并对照 branch-sync-status.md 确认未同步
SHA="8d982aa6"  # 例：橡胶树叶块放置前检查

# 2) 在 wt-121 cherry-pick
cd /home/wangyu/wt/ic2-fabric/wt-121
git cherry-pick $SHA
# 若冲突：
#   - 打开冲突文件
#   - 查 §3 适配清单看是否需要 API 适配
#   - git add . && git cherry-pick --continue

# 3) 跑 §8 自检清单

# 4) 编译
./gradlew build

# 5) 涉及数据时
./gradlew :core:runDatagen && ./gradlew build

# 6) 回到 main 更新同步状态（不更新 1.21.1，参见 §5）
cd /home/wangyu/wt/ic2-fabric/wt-main
# 编辑 docs/branch-sync-status.md，把该 SHA 标为 ✅
git add docs/branch-sync-status.md
git commit -m "docs(branch-sync): 更新同步状态表——commit $SHA 已同步到 1.21.1"
```

---

## 附录 A：按 commit 类型路由的速查表

| commit 类型 | 必走章节 | 必跑自检 |
|-------------|----------|----------|
| `feat(item)` / `feat(block)` | §3.1 + §3.2（appendTooltip、NBT、Block getCodec） | §8.4 + §8.8 |
| `feat(recipe)` | §3.4 路径单数 + §3.1 RecipeExporter/TOOLS | §8.6 + §8.4 |
| `feat(network)` / 任何 C2S | §3.3 整包重写 | §8.7 |
| `feat(machine)` | §3.1 validateTicker + §3.2 readNbt/writeNbt lookup | §8.4 + §8.8 |
| `feat(tool)` / `feat(weapon)` | §3.1 getMiningSpeed/isCorrectForDrops + 工具类构造 | §8.4 |
| `fix(mixin)` | §3.1 @Inject 替代 @Redirect | §8.4 |
| `docs(*)` | §5 单源真相检查、AGENTS.md/CLAUDE.md 要带 | §8.5 |
| `build:` / `chore(deps)` | §6 gradle 处理 | §8.5 |
| `feat(assets)` / `feat(texture)` | §3.5.J 命名空间迁移、§3.5.D GUI 取 HEAD | §8.5 |
| `merge:` （merge commit） | §3.5.C `cherry-pick -m 1` | §8.1 |
| `fix(perf)` 极广文件范围（如 65 文件） | §3.5.E 机械替换流程 | §8.1 |
| 命名变更 / enum 重命名 | §3.5.F 先看 stat，先 cherry-pick 命名 commit | §8.4 |

## 附录 B：迁移到 1.21.1 时遇到新坑怎么办

1. 先看 `docs/migration-1.20.1-to-1.21.1.md` 是否有现成答案
2. 没有则查 1.21.1 的源码（`./gradlew :core:genSources`）
3. 找到答案后**优先更新 `docs/migration-1.20.1-to-1.21.1.md`**，再在 1.21.1 上提交修复
4. 该 commit **应当 cherry-pick 回 main**（如果 main 也需要），但 `migration-1.20.1-to-1.21.1.md` 永远不带到 1.21.1

## 附录 C：高频 entry 速查（按编号）

- **#16 #22 #28 #33 #50 #55 #90 #95 #118**：`ExtendedScreenHandlerFactory<PacketByteBuf>` 泛型
- **#18 #47 #51 #64**：`appendTooltip` 签名
- **#25 #55 #70 #73 #90 #116 #118 #119 #125**：`RecipeExporter` / `RecipeCategory.EQUIPMENT`
- **#25 #55 #88 #119 #125**：`validateTicker`
- **#30 #51 #73 #90 #93 #116 #118 #119 #132**：`Identifier.of`
- **#30 #42 #47 #49 #52 #53 #54 #114 #116 #118 #119**：CustomPayload / Payload
- **#39 #87 #94 #112 #117 #118 #125**：路径单数化 / tags/block
- **#44**：datagen modid 过滤
- **#88**：65 文件 66 处机械替换
- **#109**：merge commit `-m 1`
- **#119**：蒸汽系统综合（FluidVariant CODEC + ExtendedScreenHandlerFactory + onUse 无 Hand + validateTicker）
- **#123**：GUI/Screen 冲突"取 HEAD"
- **#125**：RecipeCategory TOOLS→EQUIPMENT
- **#132 #137**：PR 误删 / 冲突解决误删
- **#159 #170**：废弃覆盖（运行时静默回归族）
- **#166** + 用户指示：单源真相 → `--skip`
