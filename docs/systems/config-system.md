# 配置系统（Ic2Config）

本文说明 `src/main/kotlin/ic2_120/config/Ic2Config.kt` 对应的配置文件怎么用、怎么重载。

## 配置文件位置

- 文件名：`ic2_120.json`
- 目录：Fabric 的 `config` 目录
- 常见路径（客户端/单机）：`.minecraft/config/ic2_120.json`
- 常见路径（服务端）：`<server_root>/config/ic2_120.json`

首次启动模组时，如果文件不存在，会自动生成默认配置。

## 配置结构

顶层结构（当前）：

- `general`
- `recycler`
- `nuclear`
- `uuReplication`

示例：

```json
{
  "general": {
    "logConfigOnLoad": true,
    "checkForUpdates": true
  },
  "recycler": {
    "blacklist": [
      "minecraft:stick"
    ]
  },
  "nuclear": {
    "enableReactorExplosion": true
  },
  "uuReplication": {
    "replicationWhitelist": {
      "minecraft:diamond": 8192
    }
  }
}
```

## 各字段用途

### `general`

- `logConfigOnLoad`：启动/重载时是否把当前配置打印到日志。
- `checkForUpdates`：更新检查开关（供更新检查逻辑读取）。

### `recycler`

- `blacklist`：回收机黑名单（物品 id 字符串列表）。
- 例如：`["minecraft:stick", "ic2_120:scrap"]`。

### `nuclear`

- `enableReactorExplosion`：核反应堆过热时是否允许爆炸。

### `uuReplication`

- `replicationWhitelist`：UU 复制白名单。
- key = 物品 id，value = 所需 UU 物质（uB，正整数）。

## 加载与重载

### 1) 启动加载

模组初始化时会执行 `Ic2Config.loadOrThrow()`。

- 文件不存在：写入默认配置并载入。
- 文件存在：解析后载入。
- JSON 解析失败：抛异常，启动失败。

### 2) 在线重载

使用命令：

```text
/ic2config reload
```

- 权限要求：`permission level >= 2`（通常 OP）。
- 成功后会回读配置并反馈当前配置内容。
- 失败时会返回错误信息，不会静默吞错。

## 注意事项

- `Ic2Config` 使用 `ignoreUnknownKeys = true`，多余字段会被忽略。
- 旧配置里若含历史 `creative` 字段，读取后会自动写回为新结构。
- `uuReplication.replicationWhitelist` 的值应为正整数；非正值会被逻辑视为无效。
