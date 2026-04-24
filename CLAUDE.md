# Claude Code - IC2-120 项目指南

当处理此项目时，请务必先阅读 **AGENTS.md** 文件。

## AGENTS.md 包含以下关键信息

1. **Mod 注册流程** - 使用类级别注解（@ModBlock、@ModItem 等）进行快速注册
2. **子系统概览** - 电力(EU)、流体、热能(HU)、核电、升级、同步系统
3. **机器实现模板** - 完整的 Block → BlockEntity → ScreenHandler → Screen 实现流程
4. **配方系统** - 如何使用 datagen 添加处理配方
5. **资源文件** - blockstates、models、lang 等配置
6. **常见问题** - 调试技巧

## 编译

```bash
# 完整编译（机器类改动必须同时编译两侧）
./gradlew clean compileKotlin compileClientKotlin

# 快速编译（跳过 test，仅编译服务端）
./gradlew compileKotlin -x test

# 完整构建 jar
./gradlew clean build -x test
```

## 资源约束

- 资源文件只新增/修改在 `src/main/resources/assets/ic2_120/**`
- `assets/ic2/**` 是上游引用资源，不可修改

## 相关文档

- AGENTS.md - 协作规范与最小执行清单
- `docs/guides/machine-implementation-guide.md` - 机器实现模板
- `docs/guides/item-implemented.md` - 物品实现指南
- `docs/registry/CLASS_BASED_REGISTRY.md` - 注解注册系统

---
**开始任何工作前，请先阅读 AGENTS.md！**
