package ic2_120.registry

import net.minecraft.util.Identifier

/**
 * 记录通过注解标记为透明渲染的方块 ID。
 * 由通用注册阶段填充，客户端初始化阶段读取并注册到渲染层。
 */
object TransparentBlockRegistry {
    private val transparentBlocks = linkedSetOf<Identifier>()

    fun clear() {
        transparentBlocks.clear()
    }

    fun add(id: Identifier) {
        transparentBlocks.add(id)
    }

    fun ids(): Set<Identifier> = transparentBlocks
}
