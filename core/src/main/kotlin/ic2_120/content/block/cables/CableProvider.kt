package ic2_120.content.block.cables

/**
 * 附属 mod 向 core 贡献导线方块的扩展点（函数式接口，由 fabric entrypoint 装配）。
 *
 * ## 背景
 * core 的 [CableBlockEntity] 用统一的 `BlockEntityType` 关联所有 [BaseCableBlock]，
 * 该类型在 core.onInitialize 末尾注册（扫描 BuiltInRegistries.BLOCK 里全部 BaseCableBlock）。
 * 但 fabric 按依赖拓扑序执行 main entrypoint，core 必先于附属，故 core 扫描时附属的
 * @ModBlock 导线尚未注册，无法被收编。
 *
 * ## 用法
 * 附属实现本接口，在 [registerCables] 中**实例化**自己的 [BaseCableBlock] 子类，
 * 通过 [ic2_120.registry.ClassScanner.registerCableBlock] 以附属 modId 注册进 BuiltInRegistries
 *（BLOCK + 方块物品 + 创造栏 + 渲染层），然后返回已注册的方块列表。
 *
 * core 会在 onInitialize 期间（CableBlockEntity 统一注册之前）通过 fabric entrypoint
 * `"ic2_120:cables"` 调用本方法，随后扫描注册表即可覆盖附属导线。
 *
 * 附属需在 `fabric.mod.json` 声明 entrypoint：
 * ```json
 * "entrypoints": { "ic2_120:cables": [ { "value": "...CableProviderImpl", "adapter": "kotlin" } ] }
 * ```
 */
fun interface CableProvider {
    /**
     * 实例化并注册本附属的全部导线方块，返回已注册的方块列表。
     *
     * 调用时机：core.onInitialize 期间（CableBlockEntity 统一注册之前），
     * 此时 BuiltInRegistries 尚未冻结，可安全执行 [net.minecraft.core.Registry.register]。
     */
    fun registerCables(): List<BaseCableBlock>
}
