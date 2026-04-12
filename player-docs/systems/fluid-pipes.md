# 流体管道系统

流体通过管道网络传输，管道网络由普通管道和泵附件组成。

---

## 管道规格

### 基础参数

| 尺寸 | 流量（桶/秒） |
|------|--------------|
| tiny | 0.4 |
| small | 0.8 |
| medium | 2.4 |
| large | 4.8 |

### 材质倍率

| 材质 | 倍率 |
|------|------|
| 青铜 | 1x |
| 碳 | 2x |

### 每 tick 上限

```
单管道最大传输 = floor(基础流量 × 材质倍率 × 1000 / 20) mB/t
```

### 管道列表

| 物品 ID | 材质 | 尺寸 |
|---------|------|------|
| bronze_pipe_tiny | 青铜 | tiny |
| bronze_pipe_small | 青铜 | small |
| bronze_pipe_medium | 青铜 | medium |
| bronze_pipe_large | 青铜 | large |
| carbon_pipe_tiny | 碳 | tiny |
| carbon_pipe_small | 碳 | small |
| carbon_pipe_medium | 碳 | medium |
| carbon_pipe_large | 碳 | large |

---

## 管道连接规则

### 普通管道

- 放置时默认连接所有可连接邻居（管道或流体容器）
- 邻居更新时重新计算连通状态
- **扳手右键**点击某面可切换该面"禁连/可连"

### 泵附件

- **物品 ID**：`bronze_pump_attachment`、`carbon_pump_attachment`
- 尺寸固定为 tiny，材质倍率同普通管道
- **正面**（FACING）：连接非管道且有 FluidStorage 的方块（储罐、机器）
- **背面**：仅连接管道
- 其余四面不可连接
- **放置方向**：必须正面朝向储罐放置才能正常工作

---

## 流体传输规则

### Provider（提供方）

流体从 Provider 抽取：

| 来源 | 条件 |
|------|------|
| 泵附件强制 | 正面朝向该方块，且该方块 supportsExtraction() |
| 升级驱动 | MachineBlock 安装了 fluid_ejector_upgrade，且 supportsExtraction() |

> 泵附件可强制任意 FluidStorage 容器（如 AE2 储罐、第三方容器）作为 Provider，无需安装升级。

### Receiver（接收方）

流体向 Receiver 插入：

| 来源 | 条件 |
|------|------|
| 非 MachineBlock | 只要有 FluidStorage 且 supportsInsertion() |
| 升级驱动 | MachineBlock 安装了 fluid_pulling_upgrade，且 supportsInsertion() |

> 非 MachineBlock（如 AE2 储罐、原版储罐）自动作为 Receiver，无需安装升级。

---

## 过滤配置

### 泵附件

- 右键打开 GUI，放入流体样本到过滤槽
- 未设置过滤可抽取任意流体

### 流体弹出/抽入升级

- **设置过滤**：手持升级 + 副手放含流体容器 + 右键
- **清除过滤**：副手为空 + 右键
- **设置方向**：潜行 + 右键循环切换方向

方向循环：任意 → 下 → 上 → 北 → 南 → 西 → 东 → 任意...

---

## 混流停机

每个管道网络同一 tick 只允许**一种流体**参与传输。

- 若同一 tick 内有多个 Provider 可输出不同流体，整网停机
- 状态显示为 Jade HUD 中的 "Stalled: Mixed Fluids"

---

## Jade HUD 显示

安装 Jade 后，悬停管道方块显示：
- "Flow: X / Y mB/t" 进度条
- 混流停机时显示 "Stalled: Mixed Fluids"
- 泵附件额外显示 "Pump Attachment" 标签
