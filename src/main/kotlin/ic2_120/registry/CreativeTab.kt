package ic2_120.registry

/**
 * 创造模式物品栏枚举。
 * 提供类型安全的物品栏ID，避免字符串拼写错误。
 */
enum class CreativeTab(val id: String) {
    /** IC2 材料物品栏 */
    IC2_MATERIALS("ic2_materials"),

    /** IC2 机器物品栏 */
    IC2_MACHINES("ic2_machines"),

    /** IC2 工具物品栏 */
    IC2_TOOLS("ic2_tools"),

    /** Minecraft 原版建筑方块 */
    MINECRAFT_BUILDING_BLOCKS("building_blocks"),

    /** Minecraft 原版装饰品 */
    MINECRAFT_DECORATIONS("decorations"),

    /** Minecraft 原版红石 */
    MINECRAFT_REDSTONE("redstone"),

    /** Minecraft 原版交通 */
    MINECRAFT_TRANSPORTATION("transportation"),

    /** Minecraft 原版杂项 */
    MINECRAFT_MISC("misc"),

    /** Minecraft 原版食物 */
    MINECRAFT_FOOD("food"),

    /** Minecraft 原版工具 */
    MINECRAFT_TOOLS("tools"),

    /** Minecraft 原版战斗 */
    MINECRAFT_COMBAT("combat"),

    /** Minecraft 原版酿造 */
    MINECRAFT_BREWING("brewing");

    /**
     * 获取完整的命名空间ID。
     * @param modId 模组命名空间
     * @return 完整ID，如 "ic2_120:ic2_materials" 或 "minecraft:building_blocks"
     */
    fun getNamespacedId(modId: String): String {
        return when (this) {
            IC2_MATERIALS, IC2_MACHINES, IC2_TOOLS -> "$modId:$id"
            MINECRAFT_BUILDING_BLOCKS, MINECRAFT_DECORATIONS, MINECRAFT_REDSTONE,
            MINECRAFT_TRANSPORTATION, MINECRAFT_MISC, MINECRAFT_FOOD,
            MINECRAFT_TOOLS, MINECRAFT_COMBAT, MINECRAFT_BREWING -> "minecraft:$id"
        }
    }

    companion object {
        /**
         * 从字符串解析为枚举。
         * 支持格式：简单的id、带命名空间的id、带命名空间和点的id
         *
         * 例如：
         * - "ic2_materials" -> IC2_MATERIALS
         * - "ic2_120:ic2_materials" -> IC2_MATERIALS
         * - "itemGroup.ic2_120.ic2_materials" -> IC2_MATERIALS
         * - "building_blocks" -> MINECRAFT_BUILDING_BLOCKS
         */
        fun fromString(tabId: String): CreativeTab {
            val normalizedId = when {
                // 移除 itemGroup. 前缀
                tabId.startsWith("itemGroup.") -> {
                    val parts = tabId.substring(10).split('.')
                    if (parts.size >= 2) parts[1] else tabId.substring(10)
                }
                // 移除命名空间
                tabId.contains(':') -> tabId.substringAfter(':')
                // 直接使用
                else -> tabId
            }

            return values().find { it.id == normalizedId }
                ?: throw IllegalArgumentException("未知的物品栏ID: $tabId (标准化为: $normalizedId)")
        }
    }
}
